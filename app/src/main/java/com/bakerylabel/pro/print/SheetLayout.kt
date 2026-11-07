package com.bakerylabel.pro.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import com.bakerylabel.pro.render.LabelRenderer
import kotlin.math.floor
import kotlin.math.max

data class SheetConfig(
    val marginMm: Float = 8f,
    val gapMm: Float = 3f,
    /** 0 = automatic */
    val rows: Int = 0,
    /** 0 = automatic */
    val cols: Int = 0,
    val cutMarks: Boolean = true,
)

data class Placement(val item: PrintItem, val xMm: Float, val yMm: Float)
data class SheetPage(val placements: List<Placement>)

data class SheetResult(
    val pages: List<SheetPage>,
    val rows: Int,
    val cols: Int,
    val cellWmm: Float,
    val cellHmm: Float,
    val warning: String? = null,
)

/** Arranges labels on A4 pages at their true physical size (never scaled). */
object SheetLayout {
    const val A4_W = 210f
    const val A4_H = 297f

    fun layout(items: List<PrintItem>, cfg: SheetConfig): SheetResult {
        val expanded = items.flatMap { item -> List(item.copies.coerceAtLeast(1)) { item } }
        if (expanded.isEmpty()) return SheetResult(emptyList(), 0, 0, 0f, 0f)
        val cellW = expanded.maxOf { it.design.widthMm }
        val cellH = expanded.maxOf { it.design.heightMm }
        val usableW = A4_W - cfg.marginMm * 2
        val usableH = A4_H - cfg.marginMm * 2
        val maxCols = max(0, floor((usableW + cfg.gapMm) / (cellW + cfg.gapMm)).toInt())
        val maxRows = max(0, floor((usableH + cfg.gapMm) / (cellH + cfg.gapMm)).toInt())
        if (maxCols == 0 || maxRows == 0) {
            return SheetResult(emptyList(), 0, 0, cellW, cellH, "Labels are larger than the printable A4 area. Reduce margins or label size.")
        }
        var warning: String? = null
        var cols = if (cfg.cols > 0) cfg.cols else maxCols
        var rows = if (cfg.rows > 0) cfg.rows else maxRows
        if (cols > maxCols) { warning = "Only $maxCols columns fit at this size; using $maxCols."; cols = maxCols }
        if (rows > maxRows) { warning = (warning?.let { "$it " } ?: "") + "Only $maxRows rows fit; using $maxRows."; rows = maxRows }
        val perPage = rows * cols
        val pages = expanded.chunked(perPage).map { chunk ->
            SheetPage(chunk.mapIndexed { i, item ->
                val r = i / cols
                val c = i % cols
                Placement(item, cfg.marginMm + c * (cellW + cfg.gapMm), cfg.marginMm + r * (cellH + cfg.gapMm))
            })
        }
        return SheetResult(pages, rows, cols, cellW, cellH, warning)
    }

    /** Draws one A4 page onto a canvas whose origin is the page's top-left corner. */
    fun drawPage(canvas: Canvas, page: SheetPage, pxPerMm: Float, logo: Bitmap?, dateFormat: String, cutMarks: Boolean) {
        val guide = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 190, 190)
            style = Paint.Style.STROKE
            strokeWidth = 0.15f * pxPerMm
            pathEffect = DashPathEffect(floatArrayOf(1f * pxPerMm, 1f * pxPerMm), 0f)
        }
        for (p in page.placements) {
            canvas.save()
            canvas.translate(p.xMm * pxPerMm, p.yMm * pxPerMm)
            LabelRenderer.draw(canvas, p.item.content, p.item.design, pxPerMm, logo, dateFormat)
            if (cutMarks) {
                canvas.drawRect(0f, 0f, p.item.design.widthMm * pxPerMm, p.item.design.heightMm * pxPerMm, guide)
            }
            canvas.restore()
        }
    }

    fun previewBitmap(page: SheetPage, pxPerMm: Float, logo: Bitmap?, dateFormat: String, cutMarks: Boolean): Bitmap {
        val bmp = Bitmap.createBitmap((A4_W * pxPerMm).toInt(), (A4_H * pxPerMm).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        drawPage(c, page, pxPerMm, logo, dateFormat, cutMarks)
        return bmp
    }
}
