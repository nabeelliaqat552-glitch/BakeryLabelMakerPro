package com.bakerylabel.pro.ui.screens

import android.content.Context
import android.graphics.Bitmap
import com.bakerylabel.pro.R
import com.bakerylabel.pro.data.AppSettings
import com.bakerylabel.pro.print.BluetoothPrinter
import com.bakerylabel.pro.print.Exporter
import com.bakerylabel.pro.print.PrintItem
import com.bakerylabel.pro.render.LabelRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Export / print helpers shared by the editor, saved labels and print screens. Each returns a message for a snackbar. */
object LabelActions {

    suspend fun sharePdf(context: Context, name: String, items: List<PrintItem>, logo: Bitmap?, s: AppSettings): String {
        val file = Exporter.exportLabelsPdf(context, name, items, logo, s.dateFormat)
        Exporter.saveToDownloads(context, file)
        Exporter.share(context, Exporter.uriFor(context, file), "application/pdf")
        return context.getString(R.string.msg_pdf_ready, file.name)
    }

    suspend fun savePng(context: Context, item: PrintItem, logo: Bitmap?, s: AppSettings, share: Boolean = true): String {
        val uri = Exporter.exportPng(context, item.name, item.content, item.design, logo, s.dateFormat, 300)
        if (share) Exporter.share(context, uri, "image/png")
        return context.getString(R.string.msg_png_saved)
    }

    /** Individual label printing through Android's print system, sized to the label itself. */
    suspend fun systemPrint(context: Context, name: String, items: List<PrintItem>, logo: Bitmap?, s: AppSettings) {
        val file = Exporter.exportLabelsPdf(context, name, items, logo, s.dateFormat)
        val first = items.first().design
        val pages = items.sumOf { it.copies.coerceAtLeast(1) }
        withContext(Dispatchers.Main) {
            Exporter.printPdf(context, name, file, pages, first.widthMm, first.heightMm, isA4 = false)
        }
    }

    suspend fun bluetoothPrint(context: Context, items: List<PrintItem>, logo: Bitmap?, s: AppSettings): String {
        if (s.printerAddress.isBlank()) return context.getString(R.string.msg_choose_printer)
        val printer = BluetoothPrinter(context)
        var printed = 0
        for (item in items) {
            val bmp = withContext(Dispatchers.Default) {
                LabelRenderer.toBitmap(item.content, item.design, s.printerDpi, logo, s.dateFormat, mono = true)
            }
            val r = printer.print(
                s.printerAddress, bmp, s.printerProtocol, s.printerWidthMm, s.printerDpi,
                item.design.widthMm, item.design.heightMm, item.copies,
            )
            r.exceptionOrNull()?.let { return context.getString(R.string.msg_printer_error, it.message ?: it.javaClass.simpleName) }
            printed += item.copies
        }
        return context.getString(R.string.msg_sent, printed, s.printerName.ifBlank { "printer" })
    }
}
