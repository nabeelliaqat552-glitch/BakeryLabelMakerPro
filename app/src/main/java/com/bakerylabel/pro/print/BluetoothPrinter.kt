package com.bakerylabel.pro.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import com.bakerylabel.pro.data.PrinterProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.UUID

data class BtDevice(val name: String, val address: String)

/**
 * Prints to paired Bluetooth thermal printers over the classic Serial Port Profile.
 * ESC/POS covers most receipt-style printers; TSPL covers most dedicated label printers
 * (TSC, Xprinter label models, Gprinter, etc.).
 */
class BluetoothPrinter(private val context: Context) {
    companion object {
        private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        val requiredPermissions: Array<String> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) arrayOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyArray()
    }

    private val adapter get() = context.getSystemService(BluetoothManager::class.java)?.adapter

    fun isSupported(): Boolean = adapter != null
    fun isEnabled(): Boolean = adapter?.isEnabled == true

    fun hasPermission(): Boolean = requiredPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BtDevice> {
        if (!hasPermission()) return emptyList()
        return adapter?.bondedDevices.orEmpty()
            .map { BtDevice(it.name ?: it.address, it.address) }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Prints a monochrome bitmap. The image is never stretched: if it is wider than the print head,
     * an error is returned so the user can choose a smaller label or a wider printer.
     */
    @SuppressLint("MissingPermission")
    suspend fun print(
        address: String,
        image: Bitmap,
        protocol: PrinterProtocol,
        headWidthMm: Float,
        dpi: Int,
        labelWmm: Float,
        labelHmm: Float,
        copies: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(hasPermission()) { "Bluetooth permission not granted" }
            val a = adapter ?: error("This phone has no Bluetooth")
            check(a.isEnabled) { "Turn on Bluetooth first" }
            val headPx = (headWidthMm / 25.4f * dpi).toInt()
            check(image.width <= headPx + 2) {
                "Label is wider (${"%.0f".format(labelWmm)} mm) than the printer head (${"%.0f".format(headWidthMm)} mm)."
            }
            val bytes = when (protocol) {
                PrinterProtocol.ESC_POS -> escPos(image, headPx, copies)
                PrinterProtocol.TSPL -> tspl(image, labelWmm, labelHmm, copies)
            }
            val device = a.getRemoteDevice(address)
            a.cancelDiscovery()
            val socket: BluetoothSocket = try {
                device.createRfcommSocketToServiceRecord(SPP).also { it.connect() }
            } catch (e: Exception) {
                // Some cheap printers only accept insecure connections.
                device.createInsecureRfcommSocketToServiceRecord(SPP).also { it.connect() }
            }
            try {
                val out: OutputStream = socket.outputStream
                var offset = 0
                while (offset < bytes.size) {
                    val len = minOf(2048, bytes.size - offset)
                    out.write(bytes, offset, len)
                    out.flush()
                    offset += len
                    delay(15)
                }
                delay(600) // let the printer drain its buffer before closing
            } finally {
                runCatching { socket.close() }
            }
            Unit
        }
    }

    /** Packs the bitmap into 1-bit rows. Returns (bytesPerRow, data) with 1 = black. */
    private fun pack(img: Bitmap, targetWidth: Int): Pair<Int, ByteArray> {
        val w = targetWidth
        val h = img.height
        val src = if (img.width == w) img else Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            val c = Canvas(it)
            c.drawColor(Color.WHITE)
            c.drawBitmap(img, ((w - img.width) / 2f).coerceAtLeast(0f), 0f, null)
        }
        val bpr = (w + 7) / 8
        val data = ByteArray(bpr * h)
        val row = IntArray(w)
        for (y in 0 until h) {
            src.getPixels(row, 0, w, 0, y, w, 1)
            for (x in 0 until w) {
                val p = row[x]
                val a = Color.alpha(p)
                val lum = (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
                val black = a > 128 && lum < 140
                if (black) {
                    val i = y * bpr + x / 8
                    data[i] = (data[i].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
        }
        return bpr to data
    }

    private fun escPos(img: Bitmap, headPx: Int, copies: Int): ByteArray {
        val (bpr, data) = pack(img, headPx)
        val h = img.height
        val out = ByteArrayOutputStream()
        repeat(copies.coerceAtLeast(1)) {
            out.write(byteArrayOf(0x1B, 0x40)) // init
            out.write(byteArrayOf(0x1B, 0x61, 0x01)) // centre
            var y = 0
            val band = 128
            while (y < h) {
                val rows = minOf(band, h - y)
                out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00, (bpr and 0xFF).toByte(), (bpr shr 8 and 0xFF).toByte(),
                    (rows and 0xFF).toByte(), (rows shr 8 and 0xFF).toByte()))
                out.write(data, y * bpr, rows * bpr)
                y += rows
            }
            out.write(byteArrayOf(0x1B, 0x64, 0x03)) // feed 3 lines
        }
        return out.toByteArray()
    }

    private fun tspl(img: Bitmap, wMm: Float, hMm: Float, copies: Int): ByteArray {
        val (bpr, data) = pack(img, img.width)
        // TSPL bitmaps print a dot where the bit is 0, so invert.
        for (i in data.indices) data[i] = (data[i].toInt() xor 0xFF).toByte()
        val out = ByteArrayOutputStream()
        fun cmd(s: String) = out.write(s.toByteArray(Charsets.US_ASCII))
        cmd("SIZE ${"%.1f".format(java.util.Locale.US, wMm)} mm,${"%.1f".format(java.util.Locale.US, hMm)} mm\r\n")
        cmd("GAP 2 mm,0 mm\r\n")
        cmd("DIRECTION 1\r\n")
        cmd("REFERENCE 0,0\r\n")
        cmd("CLS\r\n")
        cmd("BITMAP 0,0,$bpr,${img.height},0,")
        out.write(data)
        cmd("\r\nPRINT ${copies.coerceAtLeast(1)},1\r\n")
        return out.toByteArray()
    }
}
