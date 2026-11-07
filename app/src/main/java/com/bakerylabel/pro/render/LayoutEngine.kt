package com.bakerylabel.pro.render

import com.bakerylabel.pro.model.ColorRole
import com.bakerylabel.pro.model.ElementStyle
import com.bakerylabel.pro.model.FieldKey
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.model.LabelLayout
import com.bakerylabel.pro.model.LogoPosition
import com.bakerylabel.pro.model.TextAlignX
import kotlin.math.max
import kotlin.math.min

/**
 * Works out where each visible element goes for the preset layouts.
 * Everything is computed in millimetres, then stored as fractions of the label size,
 * so a layout stays correct when the label is resized.
 */
object LayoutEngine {
    private const val MM_PER_PT = 0.3528f

    private val ORDER = listOf(
        FieldKey.SHOP, FieldKey.BAKERY, FieldKey.PRODUCT, FieldKey.CONTACT, FieldKey.LICENSE,
        FieldKey.INGREDIENTS, FieldKey.STORAGE, FieldKey.BATCH, FieldKey.MFG, FieldKey.EXP,
        FieldKey.BEST_BEFORE, FieldKey.WEIGHT, FieldKey.PRICE, FieldKey.CUSTOM, FieldKey.BARCODE,
    )
    private val PAIRS = listOf(
        FieldKey.MFG to FieldKey.EXP,
        FieldKey.WEIGHT to FieldKey.PRICE,
        FieldKey.BATCH to FieldKey.BEST_BEFORE,
    )

    /** Default font sizes for a fresh element added in custom mode. */
    fun defaultFor(key: FieldKey, d: LabelDesign): ElementStyle =
        compute(d.copy(layout = LabelLayout.CLASSIC), FieldKey.entries.toSet())[key]
            ?: ElementStyle(0.1f, 0.4f, 0.8f, 0.15f, 8f)

    fun arrange(d: LabelDesign, visible: Set<FieldKey>): LabelDesign {
        if (d.layout == LabelLayout.CUSTOM) {
            val missing = visible.filter { it !in d.elements.keys }
            if (missing.isEmpty()) return d
            val defaults = compute(d.copy(layout = LabelLayout.CLASSIC), visible)
            val m = d.elements.toMutableMap()
            missing.forEach { k -> defaults[k]?.let { m[k] = it } }
            return d.copy(elements = m)
        }
        return d.copy(elements = compute(d, visible))
    }

    private fun compute(d: LabelDesign, visible: Set<FieldKey>): Map<FieldKey, ElementStyle> {
        val w = d.widthMm
        val h = d.heightMm
        val out = mutableMapOf<FieldKey, ElementStyle>()
        val pad = max(d.borderWidthMm * 2f + 0.8f, min(w, h) * 0.06f)
        val gap = min(w, h) * 0.02f
        var left = pad
        var top = pad
        var right = w - pad
        val bottom = h - pad
        val scale = d.fontScale

        fun put(k: FieldKey, x: Float, y: Float, ew: Float, eh: Float, pt: Float = 8f,
                bold: Boolean = false, align: TextAlignX = d.align, role: ColorRole = ColorRole.TEXT) {
            if (ew <= 0f || eh <= 0f) return
            out[k] = ElementStyle(x / w, y / h, ew / w, eh / h, pt, bold || d.boldText, align, role)
        }

        // --- Banner: coloured band across the top holding logo + shop name ---
        if (d.layout == LabelLayout.BANNER) {
            val bandH = h * 0.27f
            val inner = bandH - pad * 0.8f
            var bx = pad
            if (FieldKey.LOGO in visible) {
                val s = inner.coerceAtLeast(1f)
                put(FieldKey.LOGO, bx, (bandH - s) / 2f, s, s)
                bx += s + gap * 2
            }
            if (FieldKey.SHOP in visible) {
                put(FieldKey.SHOP, bx, (bandH - inner) / 2f, w - pad - bx, inner,
                    pt = inner * 0.6f / MM_PER_PT * scale, bold = true, role = ColorRole.ON_ACCENT)
            }
            top = bandH + gap * 2
        }

        // --- QR code: square at the bottom right, text keeps clear of that column ---
        if (FieldKey.QR in visible) {
            val q = min((right - left) * 0.32f, bottom - top)
            put(FieldKey.QR, right - q, bottom - q, q, q)
            right -= q + gap * 2
        }

        // --- Logo placement for the other layouts ---
        var logoBox: FloatArray? = null // x, y, w, h
        val textAlign: TextAlignX
        if (d.layout == LabelLayout.LOGO_LEFT) {
            textAlign = TextAlignX.START
            if (FieldKey.LOGO in visible) {
                val s = min((right - left) * (0.2f + d.logoSize * 0.5f), bottom - top)
                put(FieldKey.LOGO, left, top + (bottom - top - s) / 2f, s, s)
                left += s + gap * 2
            }
        } else {
            textAlign = d.align
            if (d.layout != LabelLayout.BANNER && FieldKey.LOGO in visible) {
                val s = min(w, h) * d.logoSize.coerceIn(0.1f, 0.6f)
                when (d.logoPosition) {
                    LogoPosition.TOP_CENTER -> {
                        put(FieldKey.LOGO, (left + right - s) / 2f, top, s, s)
                        top += s + gap
                    }
                    LogoPosition.TOP_LEFT -> {
                        put(FieldKey.LOGO, left, top, s, s)
                        logoBox = floatArrayOf(left, top, s, s)
                    }
                    LogoPosition.TOP_RIGHT -> {
                        put(FieldKey.LOGO, right - s, top, s, s)
                        logoBox = floatArrayOf(right - s, top, s, s)
                    }
                }
            }
        }

        // --- Text rows ---
        val rowKeys = ORDER.filter { it in visible && !(d.layout == LabelLayout.BANNER && it == FieldKey.SHOP) }
        val rows = mutableListOf<List<FieldKey>>()
        val used = mutableSetOf<FieldKey>()
        for (k in rowKeys) {
            if (k in used) continue
            val pair = if (d.layout == LabelLayout.COMPACT) PAIRS.firstOrNull { it.first == k && it.second in rowKeys } else null
            if (pair != null) {
                rows += listOf(pair.first, pair.second); used += pair.first; used += pair.second
            } else {
                rows += listOf(k); used += k
            }
        }
        if (rows.isEmpty()) return out

        val totalWeight = rows.sumOf { r -> r.maxOf { it.weight }.toDouble() }.toFloat()
        val available = (bottom - top) - gap * (rows.size - 1)
        if (available <= 0f) return out
        var y = top
        for (row in rows) {
            val rw = row.maxOf { it.weight }
            val rh = available * rw / totalWeight
            var rl = left
            var rr = right
            logoBox?.let { b ->
                val overlaps = y < b[1] + b[3] && y + rh > b[1]
                if (overlaps) {
                    if (d.logoPosition == LogoPosition.TOP_LEFT) rl = max(rl, b[0] + b[2] + gap * 2)
                    else rr = min(rr, b[0] - gap * 2)
                }
            }
            val rowW = rr - rl
            val basePt = (rh * 0.72f / MM_PER_PT) * scale
            if (row.size == 2) {
                val half = (rowW - gap * 2) / 2f
                put(row[0], rl, y, half, rh, basePt, align = TextAlignX.START)
                put(row[1], rl + half + gap * 2, y, half, rh, basePt, align = TextAlignX.END)
            } else {
                val k = row[0]
                when (k) {
                    FieldKey.BARCODE -> {
                        val bw = rowW * 0.85f
                        val bx = when (textAlign) {
                            TextAlignX.START -> rl
                            TextAlignX.CENTER -> rl + (rowW - bw) / 2f
                            TextAlignX.END -> rr - bw
                        }
                        put(k, bx, y, bw, rh)
                    }
                    FieldKey.PRODUCT -> put(k, rl, y, rowW, rh, basePt, bold = true, align = textAlign, role = ColorRole.ACCENT)
                    FieldKey.SHOP -> put(k, rl, y, rowW, rh, basePt, bold = true, align = textAlign, role = ColorRole.ACCENT)
                    else -> put(k, rl, y, rowW, rh, basePt, align = textAlign)
                }
            }
            y += rh + gap
        }
        return out
    }
}
