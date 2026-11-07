package com.bakerylabel.pro.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.bakerylabel.pro.model.BgPattern
import com.bakerylabel.pro.model.BorderStyle
import com.bakerylabel.pro.model.ColorRole
import com.bakerylabel.pro.model.ElementStyle
import com.bakerylabel.pro.model.FieldKey
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.model.LabelLayout
import com.bakerylabel.pro.model.TextAlignX
import com.bakerylabel.pro.util.Dates
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Single drawing routine used for the on-screen preview, PNG, PDF, A4 sheets and thermal printers,
 * so what the user sees is exactly what gets printed.
 *
 * @param pxPerMm resolution of the target canvas (e.g. 300 DPI = 11.811 px/mm, PDF points = 2.8346).
 * @param mono black-on-white output for thermal printers.
 */
object LabelRenderer {
    private const val PT_TO_MM = 0.3528f
    private val MULTILINE = setOf(FieldKey.INGREDIENTS, FieldKey.STORAGE, FieldKey.CUSTOM, FieldKey.CONTACT)

    fun textFor(key: FieldKey, c: LabelContent, dateFormat: String): String = when (key) {
        FieldKey.SHOP -> c.shopName
        FieldKey.BAKERY -> c.bakeryName
        FieldKey.PRODUCT -> c.productName
        FieldKey.MFG -> "MFG: " + Dates.format(c.mfgEpochDay, dateFormat)
        FieldKey.EXP -> c.expEpochDay?.let { "EXP: " + Dates.format(it, dateFormat) } ?: ""
        FieldKey.BEST_BEFORE -> c.bestBefore.takeIf { it.isNotBlank() }?.let { "Best Before: $it" } ?: ""
        FieldKey.BATCH -> c.batch.takeIf { it.isNotBlank() }?.let { "Batch: $it" } ?: ""
        FieldKey.WEIGHT -> c.netWeight.takeIf { it.isNotBlank() }?.let { "Net Wt: $it" } ?: ""
        FieldKey.PRICE -> c.price.takeIf { it.isNotBlank() }?.let { "Price: $it" } ?: ""
        FieldKey.INGREDIENTS -> c.ingredients.takeIf { it.isNotBlank() }?.let { "Ingredients: $it" } ?: ""
        FieldKey.STORAGE -> c.storage.takeIf { it.isNotBlank() }?.let { "Storage: $it" } ?: ""
        FieldKey.CONTACT -> c.contact
        FieldKey.LICENSE -> c.license
        FieldKey.CUSTOM -> c.customFields.filter { it.label.isNotBlank() || it.value.isNotBlank() }
            .joinToString("\n") { if (it.label.isBlank()) it.value else "${it.label}: ${it.value}" }
        FieldKey.BARCODE -> c.barcode
        FieldKey.QR -> c.qrText
        FieldKey.LOGO -> ""
    }

    /** Renders a label into a new bitmap at the given DPI. */
    fun toBitmap(c: LabelContent, d: LabelDesign, dpi: Int, logo: Bitmap?, dateFormat: String, mono: Boolean = false): Bitmap {
        val pxPerMm = dpi / 25.4f
        val wPx = max(1, (d.widthMm * pxPerMm).roundToInt())
        val hPx = max(1, (d.heightMm * pxPerMm).roundToInt())
        val bmp = Bitmap.createBitmap(wPx, hPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (mono) canvas.drawColor(Color.WHITE)
        draw(canvas, c, d, pxPerMm, logo, dateFormat, mono)
        return bmp
    }

    fun draw(
        canvas: Canvas,
        c: LabelContent,
        d: LabelDesign,
        pxPerMm: Float,
        logo: Bitmap?,
        dateFormat: String,
        mono: Boolean = false,
    ) {
        val w = d.widthMm * pxPerMm
        val h = d.heightMm * pxPerMm
        val bg = if (mono) Color.WHITE else d.bgColor
        val text = if (mono) Color.BLACK else d.textColor
        val accent = if (mono) Color.BLACK else d.accentColor
        val border = if (mono) Color.BLACK else d.borderColor
        val radius = d.cornerRadiusMm * pxPerMm

        val shape = Path().apply { addRoundRect(RectF(0f, 0f, w, h), radius, radius, Path.Direction.CW) }
        canvas.save()
        canvas.clipPath(shape)

        // Background
        canvas.drawColor(bg)
        if (!mono) drawPattern(canvas, d.pattern, w, h, pxPerMm, accent)

        // Banner band
        if (d.layout == LabelLayout.BANNER) {
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; style = Paint.Style.FILL }
            canvas.drawRect(0f, 0f, w, h * 0.27f, p)
        }

        // Elements
        val visible = c.visible
        for ((key, el) in d.elements) {
            if (key !in visible) continue
            val box = RectF(el.x * w, el.y * h, (el.x + el.w) * w, (el.y + el.h) * h)
            when (key) {
                FieldKey.LOGO -> logo?.let { drawBitmapFit(canvas, it, box) }
                FieldKey.BARCODE -> drawBarcode(canvas, c.barcode, box, text, d, pxPerMm)
                FieldKey.QR -> drawQr(canvas, c.qrText, box, text)
                else -> {
                    val s = textFor(key, c, dateFormat)
                    if (s.isNotBlank()) {
                        val color = when (el.role) {
                            ColorRole.TEXT -> text
                            ColorRole.ACCENT -> accent
                            ColorRole.ON_ACCENT -> if (mono) Color.WHITE else bg
                        }
                        drawText(canvas, s, box, el, d, pxPerMm, color, key in MULTILINE)
                    }
                }
            }
        }
        canvas.restore()

        // Border (drawn on top, inside the label edge)
        drawBorder(canvas, d, w, h, pxPerMm, radius, border)
    }

    private fun typeface(d: LabelDesign, bold: Boolean): Typeface =
        Typeface.create(d.font.family, if (bold) Typeface.BOLD else Typeface.NORMAL)

    private fun drawText(
        canvas: Canvas, s: String, box: RectF, el: ElementStyle, d: LabelDesign,
        pxPerMm: Float, color: Int, multiline: Boolean,
    ) {
        if (box.width() < 1f || box.height() < 1f) return
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = typeface(d, el.bold)
        }
        val alignment = when (el.align) {
            TextAlignX.START -> Layout.Alignment.ALIGN_NORMAL
            TextAlignX.CENTER -> Layout.Alignment.ALIGN_CENTER
            TextAlignX.END -> Layout.Alignment.ALIGN_OPPOSITE
        }
        val boxW = box.width().toInt().coerceAtLeast(1)
        val maxPx = el.sizePt * PT_TO_MM * pxPerMm
        val minPx = max(1f, 2.5f * PT_TO_MM * pxPerMm)
        var size = maxPx
        val sourceLines = s.lines()
        var layout: StaticLayout
        var done: Boolean
        // Shrink the text until it fits its box (single line for short fields, wrapped for long ones).
        do {
            paint.textSize = size
            layout = StaticLayout.Builder.obtain(s, 0, s.length, paint, boxW)
                .setAlignment(alignment)
                .setIncludePad(false)
                .setLineSpacing(0f, 1f)
                .build()
            val fitsHeight = layout.height <= box.height()
            val fitsWidth = multiline ||
                (sourceLines.size == layout.lineCount && sourceLines.all { paint.measureText(it) <= boxW })
            done = (fitsHeight && fitsWidth) || size <= minPx
            if (!done) size *= 0.92f
        } while (!done)
        canvas.save()
        canvas.clipRect(box)
        val dy = max(0f, (box.height() - layout.height) / 2f)
        canvas.translate(box.left, box.top + dy)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawBitmapFit(canvas: Canvas, bmp: Bitmap, box: RectF) {
        val s = min(box.width() / bmp.width, box.height() / bmp.height)
        val dw = bmp.width * s
        val dh = bmp.height * s
        val dst = RectF(box.centerX() - dw / 2, box.centerY() - dh / 2, box.centerX() + dw / 2, box.centerY() + dh / 2)
        canvas.drawBitmap(bmp, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    private fun matrix(value: String, format: BarcodeFormat, w: Int, h: Int): BitMatrix? = runCatching {
        val hints = mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.CHARACTER_SET to "UTF-8")
        MultiFormatWriter().encode(value, format, w, h, hints)
    }.getOrNull()

    private fun drawModules(canvas: Canvas, m: BitMatrix, box: RectF, color: Int) {
        val p = Paint().apply { this.color = color; style = Paint.Style.FILL; isAntiAlias = false }
        val mw = box.width() / m.width
        val mh = box.height() / m.height
        for (yy in 0 until m.height) {
            var xx = 0
            while (xx < m.width) {
                if (m.get(xx, yy)) {
                    val start = xx
                    while (xx < m.width && m.get(xx, yy)) xx++
                    canvas.drawRect(box.left + start * mw, box.top + yy * mh, box.left + xx * mw, box.top + (yy + 1) * mh, p)
                } else xx++
            }
        }
    }

    private fun drawBarcode(canvas: Canvas, value: String, box: RectF, color: Int, d: LabelDesign, pxPerMm: Float) {
        if (value.isBlank()) return
        val textH = box.height() * 0.28f
        val barBox = RectF(box.left, box.top, box.right, box.bottom - textH)
        // Code 128 supports ASCII; fall back gracefully on anything else.
        val m = matrix(value, BarcodeFormat.CODE_128, 0, 1) ?: return
        // Encode 1px tall: drawModules stretches rows to the box height.
        drawModules(canvas, m, barBox, color)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            textSize = textH * 0.85f
        }
        while (paint.measureText(value) > box.width() && paint.textSize > 2f) paint.textSize *= 0.9f
        canvas.drawText(value, box.centerX(), box.bottom - textH * 0.12f, paint)
    }

    private fun drawQr(canvas: Canvas, value: String, box: RectF, color: Int) {
        if (value.isBlank()) return
        val side = min(box.width(), box.height())
        val m = matrix(value, BarcodeFormat.QR_CODE, 0, 0) ?: return
        val sq = RectF(box.centerX() - side / 2, box.centerY() - side / 2, box.centerX() + side / 2, box.centerY() + side / 2)
        drawModules(canvas, m, sq, color)
    }

    private fun drawPattern(canvas: Canvas, pattern: BgPattern, w: Float, h: Float, pxPerMm: Float, color: Int) {
        if (pattern == BgPattern.NONE) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            alpha = 28
            strokeWidth = 0.25f * pxPerMm
            style = Paint.Style.STROKE
        }
        val step = 3f * pxPerMm
        when (pattern) {
            BgPattern.DOTS -> {
                p.style = Paint.Style.FILL
                var y = step / 2
                var row = 0
                while (y < h) {
                    var x = if (row % 2 == 0) step / 2 else step
                    while (x < w) { canvas.drawCircle(x, y, 0.35f * pxPerMm, p); x += step }
                    y += step; row++
                }
            }
            BgPattern.STRIPES -> {
                var x = -h
                while (x < w) { canvas.drawLine(x, h, x + h, 0f, p); x += step }
            }
            BgPattern.GRID -> {
                var x = 0f
                while (x < w) { canvas.drawLine(x, 0f, x, h, p); x += step }
                var y = 0f
                while (y < h) { canvas.drawLine(0f, y, w, y, p); y += step }
            }
            BgPattern.DIAMONDS -> {
                var x = -h
                while (x < w) {
                    canvas.drawLine(x, h, x + h, 0f, p)
                    canvas.drawLine(x, 0f, x + h, h, p)
                    x += step * 1.5f
                }
            }
            BgPattern.WAVES -> {
                var y = step
                while (y < h) {
                    val path = Path()
                    path.moveTo(0f, y)
                    var x = 0f
                    while (x < w) {
                        path.quadTo(x + step / 2, y - step / 3, x + step, y)
                        path.quadTo(x + step * 1.5f, y + step / 3, x + step * 2, y)
                        x += step * 2
                    }
                    canvas.drawPath(path, p)
                    y += step
                }
            }
            BgPattern.NONE -> Unit
        }
    }

    private fun drawBorder(canvas: Canvas, d: LabelDesign, w: Float, h: Float, pxPerMm: Float, radius: Float, color: Int) {
        if (d.borderStyle == BorderStyle.NONE || d.borderWidthMm <= 0f) return
        val sw = d.borderWidthMm * pxPerMm
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = sw
        }
        when (d.borderStyle) {
            BorderStyle.DASHED -> p.pathEffect = DashPathEffect(floatArrayOf(sw * 4, sw * 2.5f), 0f)
            BorderStyle.DOTTED -> {
                p.pathEffect = DashPathEffect(floatArrayOf(0.01f, sw * 2f), 0f)
                p.strokeCap = Paint.Cap.ROUND
            }
            else -> Unit
        }
        val inset = sw / 2
        val r = RectF(inset, inset, w - inset, h - inset)
        val rr = max(0f, radius - inset)
        if (d.borderStyle == BorderStyle.DOUBLE) {
            p.strokeWidth = sw / 2.5f
            val i1 = p.strokeWidth / 2
            canvas.drawRoundRect(RectF(i1, i1, w - i1, h - i1), rr, rr, p)
            val i2 = sw * 1.6f
            canvas.drawRoundRect(RectF(i2, i2, w - i2, h - i2), max(0f, radius - i2), max(0f, radius - i2), p)
        } else {
            canvas.drawRoundRect(r, rr, rr, p)
        }
    }
}
