package com.bakerylabel.pro.print

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.render.LabelRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.ceil

/** PNG / PDF export and Android system printing. All output keeps true physical size. */
object Exporter {
    const val PT_PER_MM = 72f / 25.4f

    private fun safeName(name: String) =
        name.replace(Regex("[^A-Za-z0-9_\\- ]"), "").trim().replace(' ', '_').ifEmpty { "label" }

    private fun exportDir(context: Context) = File(context.cacheDir, "exports").apply { mkdirs() }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    /** Renders at the chosen DPI (default 300) and stores the PNG in Pictures/BakeryLabels. Returns a shareable Uri. */
    suspend fun exportPng(
        context: Context, name: String, c: LabelContent, d: LabelDesign, logo: Bitmap?,
        dateFormat: String, dpi: Int = 300,
    ): Uri = withContext(Dispatchers.IO) {
        val bmp = LabelRenderer.toBitmap(c, d, dpi, logo, dateFormat)
        val fileName = "${safeName(name)}_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BakeryLabels")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create image file")
            context.contentResolver.openOutputStream(uri)?.use { out ->
                writePngWithDpi(bmp, dpi, out)
            } ?: error("Could not write image file")
            uri
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "BakeryLabels").apply { mkdirs() }
            val f = File(dir, fileName)
            FileOutputStream(f).use { writePngWithDpi(bmp, dpi, it) }
            uriFor(context, f)
        }
    }

    /**
     * Writes a PNG and injects a pHYs chunk so image editors and print shops read the correct DPI
     * (Android's encoder does not store resolution on its own).
     */
    private fun writePngWithDpi(bmp: Bitmap, dpi: Int, out: java.io.OutputStream) {
        val bytes = java.io.ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        // PNG signature (8) + IHDR chunk (4 len + 4 type + 13 data + 4 crc = 25) = 33
        val insertAt = 33
        if (bytes.size < insertAt) { out.write(bytes); return }
        val ppm = Math.round(dpi / 0.0254)
        val data = java.nio.ByteBuffer.allocate(9).putInt(ppm.toInt()).putInt(ppm.toInt()).put(1.toByte()).array()
        val type = "pHYs".toByteArray(Charsets.US_ASCII)
        val crc = java.util.zip.CRC32().apply { update(type); update(data) }.value
        val chunk = java.nio.ByteBuffer.allocate(4 + 4 + 9 + 4).putInt(9).put(type).put(data).putInt(crc.toInt()).array()
        out.write(bytes, 0, insertAt)
        out.write(chunk)
        out.write(bytes, insertAt, bytes.size - insertAt)
    }

    /** One PDF page per label, each page exactly the size of the label. */
    suspend fun exportLabelsPdf(
        context: Context, name: String, items: List<PrintItem>, logo: Bitmap?, dateFormat: String,
    ): File = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        var n = 1
        for (item in items) repeat(item.copies.coerceAtLeast(1)) {
            val wPt = ceil(item.design.widthMm * PT_PER_MM).toInt()
            val hPt = ceil(item.design.heightMm * PT_PER_MM).toInt()
            val page = doc.startPage(PdfDocument.PageInfo.Builder(wPt, hPt, n++).create())
            LabelRenderer.draw(page.canvas, item.content, item.design, PT_PER_MM, logo, dateFormat)
            doc.finishPage(page)
        }
        val f = File(exportDir(context), "${safeName(name)}.pdf")
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        f
    }

    /** A4 sticker sheets. */
    suspend fun exportSheetPdf(
        context: Context, name: String, result: SheetResult, cfg: SheetConfig, logo: Bitmap?, dateFormat: String,
    ): File = withContext(Dispatchers.IO) {
        require(result.pages.isNotEmpty()) { result.warning ?: "Nothing to print" }
        val doc = PdfDocument()
        val wPt = Math.round(SheetLayout.A4_W * PT_PER_MM).toInt() // 595
        val hPt = Math.round(SheetLayout.A4_H * PT_PER_MM).toInt() // 842
        result.pages.forEachIndexed { i, p ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(wPt, hPt, i + 1).create())
            SheetLayout.drawPage(page.canvas, p, PT_PER_MM, logo, dateFormat, cfg.cutMarks)
            doc.finishPage(page)
        }
        val f = File(exportDir(context), "${safeName(name)}.pdf")
        FileOutputStream(f).use { doc.writeTo(it) }
        doc.close()
        f
    }

    /** Copies an exported PDF to Downloads (Android 10+) so it is easy to find later. */
    suspend fun saveToDownloads(context: Context, file: File): Uri? = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BakeryLabels")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        context.contentResolver.openOutputStream(uri)?.use { out -> FileInputStream(file).use { it.copyTo(out) } }
        uri
    }

    fun share(context: Context, uri: Uri, mime: String) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Sends a PDF to Android's print system. [mediaWmm]/[mediaHmm] tell the printer the paper size,
     * so the label prints at 100 % rather than being scaled to another paper size.
     */
    fun printPdf(context: Context, jobName: String, pdf: File, pageCount: Int, mediaWmm: Float, mediaHmm: Float, isA4: Boolean) {
        val pm = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val media = if (isA4) PrintAttributes.MediaSize.ISO_A4 else PrintAttributes.MediaSize(
            "label_${mediaWmm.toInt()}x${mediaHmm.toInt()}", "Label ${mediaWmm.toInt()}×${mediaHmm.toInt()} mm",
            (mediaWmm / 25.4f * 1000).toInt(), (mediaHmm / 25.4f * 1000).toInt(),
        )
        val attrs = PrintAttributes.Builder()
            .setMediaSize(media)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()
        pm.print(jobName, PdfFileAdapter(jobName, pdf, pageCount), attrs)
    }

    private class PdfFileAdapter(val name: String, val file: File, val pages: Int) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
            cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
            val info = PrintDocumentInfo.Builder("$name.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pages)
                .build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }

        override fun onWrite(
            pageRanges: Array<out PageRange>?, destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?, callback: WriteResultCallback,
        ) {
            try {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) }
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }
}
