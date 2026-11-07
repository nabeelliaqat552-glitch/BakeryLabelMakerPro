package com.bakerylabel.pro.render

import com.bakerylabel.pro.model.BgPattern
import com.bakerylabel.pro.model.BorderStyle
import com.bakerylabel.pro.model.ColorThemes
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.model.LabelFont
import com.bakerylabel.pro.model.LabelLayout
import com.bakerylabel.pro.model.LogoPosition
import com.bakerylabel.pro.model.TextAlignX

data class LabelTemplate(
    val id: String,
    val name: String,
    val category: String,
    val design: LabelDesign,
    val userTemplateId: Long? = null,
)

/** The 26 built-in templates. Each one is a normal design and can be changed freely in the designer. */
object Templates {
    private const val IN = 25.4f

    private fun t(
        id: String, name: String, cat: String, theme: Int, layout: LabelLayout,
        font: LabelFont, pattern: BgPattern = BgPattern.NONE, border: BorderStyle = BorderStyle.SOLID,
        w: Float = 2 * IN, h: Float = 1 * IN, radius: Float = 2f, bw: Float = 0.6f,
        logoPos: LogoPosition = LogoPosition.TOP_LEFT, align: TextAlignX = TextAlignX.CENTER,
    ): LabelTemplate {
        val th = ColorThemes.all[theme]
        val d = LabelDesign(
            widthMm = w, heightMm = h,
            bgColor = th.bg, textColor = th.text, accentColor = th.accent, borderColor = th.border,
            font = font, pattern = pattern, borderStyle = border, cornerRadiusMm = radius,
            borderWidthMm = bw, layout = layout, logoPosition = logoPos, align = align, themeName = th.name,
        )
        return LabelTemplate(id, name, cat, LayoutEngine.arrange(d, LabelContent.DEFAULT_VISIBLE))
    }

    val builtIn: List<LabelTemplate> = listOf(
        t("mithai_traditional", "Traditional Mithai", "Sweets", 0, LabelLayout.BANNER, LabelFont.SERIF, BgPattern.DIAMONDS, BorderStyle.DOUBLE, bw = 1f),
        t("mithai_royal", "Royal Mithai Box", "Sweets", 1, LabelLayout.CLASSIC, LabelFont.SERIF, BgPattern.DIAMONDS, BorderStyle.DOUBLE, 3 * IN, 2 * IN, bw = 1.2f, logoPos = LogoPosition.TOP_CENTER),
        t("barfi_premium", "Premium Barfi", "Sweets", 1, LabelLayout.LOGO_LEFT, LabelFont.SERIF, BgPattern.NONE, BorderStyle.SOLID),
        t("barfi_pista", "Pista Barfi Green", "Sweets", 5, LabelLayout.BANNER, LabelFont.MEDIUM, BgPattern.DOTS),
        t("gulab_jamun", "Gulab Jamun", "Sweets", 6, LabelLayout.CLASSIC, LabelFont.SERIF, BgPattern.WAVES, BorderStyle.SOLID, radius = 4f),
        t("rasgulla", "Rasgulla Fresh", "Sweets", 3, LabelLayout.COMPACT, LabelFont.SANS, BgPattern.NONE, BorderStyle.DASHED),
        t("laddu_premium", "Premium Laddu", "Sweets", 0, LabelLayout.LOGO_LEFT, LabelFont.SERIF, BgPattern.DOTS, BorderStyle.DOUBLE, 3 * IN, 2 * IN, bw = 1f),
        t("jalebi", "Jalebi Classic", "Sweets", 4, LabelLayout.BANNER, LabelFont.CONDENSED),
        t("kaju_katli", "Kaju Katli Silver", "Sweets", 1, LabelLayout.COMPACT, LabelFont.SMALLCAPS, BgPattern.GRID, BorderStyle.SOLID),
        t("biscuits", "Bakery Biscuits", "Bakery", 2, LabelLayout.CLASSIC, LabelFont.MEDIUM, BgPattern.DOTS),
        t("cookies_choc", "Chocolate Cookies", "Bakery", 2, LabelLayout.BANNER, LabelFont.SERIF, BgPattern.NONE, BorderStyle.SOLID, radius = 3f),
        t("bread", "Bakery Bread", "Bakery", 2, LabelLayout.COMPACT, LabelFont.SANS, BgPattern.STRIPES, BorderStyle.DASHED, 3 * IN, 2 * IN),
        t("cake", "Celebration Cake", "Bakery", 6, LabelLayout.CLASSIC, LabelFont.CURSIVE, BgPattern.DOTS, BorderStyle.DOUBLE, 3 * IN, 3 * IN, radius = 6f, bw = 1f, logoPos = LogoPosition.TOP_CENTER),
        t("cake_choc", "Chocolate Cake", "Bakery", 1, LabelLayout.BANNER, LabelFont.SERIF, BgPattern.WAVES, BorderStyle.SOLID, 3 * IN, 2 * IN),
        t("pastries", "Pastries Boutique", "Bakery", 6, LabelLayout.LOGO_LEFT, LabelFont.CURSIVE, BgPattern.NONE, BorderStyle.DOTTED, radius = 4f),
        t("brownies", "Fudge Brownies", "Bakery", 2, LabelLayout.CLASSIC, LabelFont.SMALLCAPS, BgPattern.GRID),
        t("donuts", "Donut Shop", "Bakery", 4, LabelLayout.BANNER, LabelFont.CASUAL, BgPattern.DOTS, BorderStyle.SOLID, radius = 5f),
        t("rusk", "Bakery Rusk", "Bakery", 0, LabelLayout.COMPACT, LabelFont.CONDENSED, BgPattern.STRIPES),
        t("croissant", "French Croissant", "Bakery", 3, LabelLayout.CLASSIC, LabelFont.SERIF, BgPattern.NONE, BorderStyle.DOUBLE),
        t("chocolates", "Luxury Chocolates", "Chocolates", 1, LabelLayout.CLASSIC, LabelFont.SERIF, BgPattern.DIAMONDS, BorderStyle.DOUBLE, 2 * IN, 2 * IN, bw = 1f, logoPos = LogoPosition.TOP_CENTER),
        t("choc_bar", "Chocolate Bar Wrap", "Chocolates", 2, LabelLayout.LOGO_LEFT, LabelFont.MEDIUM, BgPattern.WAVES, BorderStyle.SOLID, 4 * IN, 2 * IN),
        t("packaging_premium", "Premium Food Packaging", "Packaging", 0, LabelLayout.BANNER, LabelFont.SERIF, BgPattern.NONE, BorderStyle.DOUBLE, 4 * IN, 3 * IN, bw = 1.2f),
        t("packaging_minimal", "Minimal Packaging", "Packaging", 3, LabelLayout.LOGO_LEFT, LabelFont.SANS, BgPattern.NONE, BorderStyle.SOLID, 4 * IN, 2 * IN, radius = 0f, align = TextAlignX.START),
        t("square_seal", "Gold Seal Square", "Packaging", 0, LabelLayout.CLASSIC, LabelFont.SMALLCAPS, BgPattern.DIAMONDS, BorderStyle.DOUBLE, 1 * IN, 1 * IN, radius = 12f, bw = 0.8f, logoPos = LogoPosition.TOP_CENTER),
        t("date_strip", "MFG / EXP Date Strip", "Packaging", 4, LabelLayout.COMPACT, LabelFont.MONO, BgPattern.NONE, BorderStyle.SOLID, 2 * IN, 1 * IN, radius = 0f),
        t("namkeen", "Namkeen & Nimco", "Snacks", 5, LabelLayout.BANNER, LabelFont.CONDENSED, BgPattern.STRIPES),
        t("dairy", "Fresh Dairy", "Dairy", 3, LabelLayout.CLASSIC, LabelFont.SANS, BgPattern.WAVES, BorderStyle.DASHED, 2 * IN, 2 * IN, logoPos = LogoPosition.TOP_CENTER),
    )

    fun byId(id: String): LabelTemplate? = builtIn.firstOrNull { it.id == id }
}
