package com.bakerylabel.pro.model

import org.json.JSONArray
import org.json.JSONObject

/** Every piece of information that can appear on a label. */
enum class FieldKey(val weight: Float, val isText: Boolean = true) {
    LOGO(0f, false),
    SHOP(1.3f),
    BAKERY(1.0f),
    PRODUCT(2.0f),
    CONTACT(0.85f),
    LICENSE(0.85f),
    INGREDIENTS(1.5f),
    STORAGE(1.0f),
    BATCH(1.0f),
    MFG(1.0f),
    EXP(1.0f),
    BEST_BEFORE(1.0f),
    WEIGHT(1.0f),
    PRICE(1.0f),
    CUSTOM(1.0f),
    BARCODE(2.0f, false),
    QR(0f, false);
}

enum class TextAlignX { START, CENTER, END }
enum class ColorRole { TEXT, ACCENT, ON_ACCENT }
enum class BorderStyle { NONE, SOLID, DASHED, DOTTED, DOUBLE }
enum class BgPattern { NONE, DOTS, STRIPES, GRID, DIAMONDS, WAVES }
enum class LabelLayout { CLASSIC, BANNER, LOGO_LEFT, COMPACT, CUSTOM }
enum class LogoPosition { TOP_LEFT, TOP_CENTER, TOP_RIGHT }

enum class LabelFont(val family: String, val displayName: String) {
    SERIF("serif", "Serif Classic"),
    SANS("sans-serif", "Modern Sans"),
    MEDIUM("sans-serif-medium", "Sans Medium"),
    CONDENSED("sans-serif-condensed", "Condensed"),
    SMALLCAPS("sans-serif-smallcaps", "Small Caps"),
    MONO("monospace", "Monospace"),
    SERIF_MONO("serif-monospace", "Typewriter"),
    CURSIVE("cursive", "Script"),
    CASUAL("casual", "Casual"),
}

data class CustomField(val label: String, val value: String)

/** Position and style of one element, in fractions of the label width/height. */
data class ElementStyle(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val sizePt: Float,
    val bold: Boolean = false,
    val align: TextAlignX = TextAlignX.CENTER,
    val role: ColorRole = ColorRole.TEXT,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("x", x.toDouble()).put("y", y.toDouble())
        .put("w", w.toDouble()).put("h", h.toDouble())
        .put("s", sizePt.toDouble()).put("b", bold)
        .put("a", align.name).put("r", role.name)

    companion object {
        fun fromJson(o: JSONObject) = ElementStyle(
            x = o.optDouble("x", 0.0).toFloat(),
            y = o.optDouble("y", 0.0).toFloat(),
            w = o.optDouble("w", 1.0).toFloat(),
            h = o.optDouble("h", 0.1).toFloat(),
            sizePt = o.optDouble("s", 8.0).toFloat(),
            bold = o.optBoolean("b", false),
            align = enumOr(o.optString("a"), TextAlignX.CENTER),
            role = enumOr(o.optString("r"), ColorRole.TEXT),
        )
    }
}

/** What is printed on a label. Dates are stored as epoch days (java.time.LocalDate.toEpochDay). */
data class LabelContent(
    val shopName: String = "",
    val bakeryName: String = "",
    val productName: String = "",
    val mfgEpochDay: Long = java.time.LocalDate.now().toEpochDay(),
    val expEpochDay: Long? = null,
    /** How the expiry was calculated, e.g. "D:3" or "M:6"; null when the user picked the date by hand. */
    val expRule: String? = null,
    val bestBefore: String = "",
    val batch: String = "",
    val netWeight: String = "",
    val price: String = "",
    val ingredients: String = "",
    val storage: String = "",
    val barcode: String = "",
    val qrText: String = "",
    val contact: String = "",
    val license: String = "",
    val customFields: List<CustomField> = emptyList(),
    val visible: Set<FieldKey> = DEFAULT_VISIBLE,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("shop", shopName); put("bakery", bakeryName); put("product", productName)
        put("mfg", mfgEpochDay)
        if (expEpochDay != null) put("exp", expEpochDay)
        if (expRule != null) put("expRule", expRule)
        put("bb", bestBefore); put("batch", batch); put("wt", netWeight); put("price", price)
        put("ingr", ingredients); put("store", storage); put("bar", barcode); put("qr", qrText)
        put("contact", contact); put("lic", license)
        put("custom", JSONArray().apply {
            customFields.forEach { put(JSONObject().put("l", it.label).put("v", it.value)) }
        })
        put("visible", JSONArray().apply { visible.forEach { put(it.name) } })
    }

    fun toJsonString(): String = toJson().toString()

    companion object {
        val DEFAULT_VISIBLE = setOf(
            FieldKey.LOGO, FieldKey.SHOP, FieldKey.PRODUCT, FieldKey.MFG,
            FieldKey.EXP, FieldKey.WEIGHT, FieldKey.PRICE,
        )

        fun fromJson(o: JSONObject): LabelContent {
            val custom = mutableListOf<CustomField>()
            o.optJSONArray("custom")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.getJSONObject(i)
                    custom += CustomField(c.optString("l"), c.optString("v"))
                }
            }
            val vis = mutableSetOf<FieldKey>()
            o.optJSONArray("visible")?.let { arr ->
                for (i in 0 until arr.length()) {
                    runCatching { FieldKey.valueOf(arr.getString(i)) }.getOrNull()?.let { vis += it }
                }
            }
            return LabelContent(
                shopName = o.optString("shop"),
                bakeryName = o.optString("bakery"),
                productName = o.optString("product"),
                mfgEpochDay = o.optLong("mfg", java.time.LocalDate.now().toEpochDay()),
                expEpochDay = if (o.has("exp")) o.getLong("exp") else null,
                expRule = if (o.has("expRule")) o.getString("expRule") else null,
                bestBefore = o.optString("bb"),
                batch = o.optString("batch"),
                netWeight = o.optString("wt"),
                price = o.optString("price"),
                ingredients = o.optString("ingr"),
                storage = o.optString("store"),
                barcode = o.optString("bar"),
                qrText = o.optString("qr"),
                contact = o.optString("contact"),
                license = o.optString("lic"),
                customFields = custom,
                visible = if (o.has("visible")) vis else DEFAULT_VISIBLE,
            )
        }

        fun fromJsonString(s: String): LabelContent =
            runCatching { fromJson(JSONObject(s)) }.getOrDefault(LabelContent())
    }
}

/** How a label looks. All measurements are in real millimetres so print output is exact. */
data class LabelDesign(
    val widthMm: Float = 50.8f,
    val heightMm: Float = 25.4f,
    val bgColor: Int = 0xFFFFF8EC.toInt(),
    val textColor: Int = 0xFF4A2C21.toInt(),
    val accentColor: Int = 0xFF7A1F2B.toInt(),
    val borderColor: Int = 0xFFD4A944.toInt(),
    val font: LabelFont = LabelFont.SERIF,
    val fontScale: Float = 1f,
    val boldText: Boolean = false,
    val align: TextAlignX = TextAlignX.CENTER,
    val borderWidthMm: Float = 0.6f,
    val borderStyle: BorderStyle = BorderStyle.SOLID,
    val cornerRadiusMm: Float = 2f,
    val pattern: BgPattern = BgPattern.NONE,
    val layout: LabelLayout = LabelLayout.CLASSIC,
    val logoSize: Float = 0.30f,
    val logoPosition: LogoPosition = LogoPosition.TOP_LEFT,
    val themeName: String = "Maroon & Gold",
    val elements: Map<FieldKey, ElementStyle> = emptyMap(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("w", widthMm.toDouble()); put("h", heightMm.toDouble())
        put("bg", bgColor); put("tx", textColor); put("ac", accentColor); put("bc", borderColor)
        put("font", font.name); put("fs", fontScale.toDouble()); put("bold", boldText)
        put("align", align.name)
        put("bw", borderWidthMm.toDouble()); put("bs", borderStyle.name); put("cr", cornerRadiusMm.toDouble())
        put("pat", pattern.name); put("layout", layout.name)
        put("logoSize", logoSize.toDouble()); put("logoPos", logoPosition.name)
        put("theme", themeName)
        put("el", JSONObject().apply { elements.forEach { (k, v) -> put(k.name, v.toJson()) } })
    }

    fun toJsonString(): String = toJson().toString()

    companion object {
        fun fromJson(o: JSONObject): LabelDesign {
            val els = mutableMapOf<FieldKey, ElementStyle>()
            o.optJSONObject("el")?.let { eo ->
                eo.keys().forEach { k ->
                    val key = runCatching { FieldKey.valueOf(k) }.getOrNull() ?: return@forEach
                    els[key] = ElementStyle.fromJson(eo.getJSONObject(k))
                }
            }
            return LabelDesign(
                widthMm = o.optDouble("w", 50.8).toFloat(),
                heightMm = o.optDouble("h", 25.4).toFloat(),
                bgColor = o.optInt("bg", 0xFFFFF8EC.toInt()),
                textColor = o.optInt("tx", 0xFF4A2C21.toInt()),
                accentColor = o.optInt("ac", 0xFF7A1F2B.toInt()),
                borderColor = o.optInt("bc", 0xFFD4A944.toInt()),
                font = enumOr(o.optString("font"), LabelFont.SERIF),
                fontScale = o.optDouble("fs", 1.0).toFloat(),
                boldText = o.optBoolean("bold", false),
                align = enumOr(o.optString("align"), TextAlignX.CENTER),
                borderWidthMm = o.optDouble("bw", 0.6).toFloat(),
                borderStyle = enumOr(o.optString("bs"), BorderStyle.SOLID),
                cornerRadiusMm = o.optDouble("cr", 2.0).toFloat(),
                pattern = enumOr(o.optString("pat"), BgPattern.NONE),
                layout = enumOr(o.optString("layout"), LabelLayout.CLASSIC),
                logoSize = o.optDouble("logoSize", 0.3).toFloat(),
                logoPosition = enumOr(o.optString("logoPos"), LogoPosition.TOP_LEFT),
                themeName = o.optString("theme", "Custom"),
                elements = els,
            )
        }

        fun fromJsonString(s: String): LabelDesign =
            runCatching { fromJson(JSONObject(s)) }.getOrDefault(LabelDesign())
    }
}

inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
    if (name.isNullOrEmpty()) default else runCatching { enumValueOf<T>(name) }.getOrDefault(default)

/** A colour theme: background, text, accent and border. */
data class ColorTheme(val name: String, val bg: Int, val text: Int, val accent: Int, val border: Int)

object ColorThemes {
    val all = listOf(
        ColorTheme("Maroon & Gold", 0xFFFFF8EC.toInt(), 0xFF4A2C21.toInt(), 0xFF7A1F2B.toInt(), 0xFFD4A944.toInt()),
        ColorTheme("Black & Gold", 0xFF141212.toInt(), 0xFFF5E6C4.toInt(), 0xFFD4A944.toInt(), 0xFFD4A944.toInt()),
        ColorTheme("Chocolate & Cream", 0xFFFFF4E0.toInt(), 0xFF3B2218.toInt(), 0xFF4A2C21.toInt(), 0xFF4A2C21.toInt()),
        ColorTheme("Royal Blue & White", 0xFFFFFFFF.toInt(), 0xFF0F2A5C.toInt(), 0xFF1B3F8B.toInt(), 0xFF1B3F8B.toInt()),
        ColorTheme("Red & White", 0xFFFFFFFF.toInt(), 0xFF3A0A0A.toInt(), 0xFFC62828.toInt(), 0xFFC62828.toInt()),
        ColorTheme("Green & White", 0xFFFFFFFF.toInt(), 0xFF12341C.toInt(), 0xFF2E7D32.toInt(), 0xFF2E7D32.toInt()),
        ColorTheme("Pink & Gold", 0xFFFFF0F4.toInt(), 0xFF5A1F35.toInt(), 0xFFD6487A.toInt(), 0xFFD4A944.toInt()),
    )
}

/** Standard label sizes in inches. */
data class LabelSize(val label: String, val widthMm: Float, val heightMm: Float)

object LabelSizes {
    private const val IN = 25.4f
    val presets = listOf(
        LabelSize("1 × 1 in", 1 * IN, 1 * IN),
        LabelSize("2 × 1 in", 2 * IN, 1 * IN),
        LabelSize("2 × 2 in", 2 * IN, 2 * IN),
        LabelSize("3 × 2 in", 3 * IN, 2 * IN),
        LabelSize("3 × 3 in", 3 * IN, 3 * IN),
        LabelSize("4 × 2 in", 4 * IN, 2 * IN),
        LabelSize("4 × 3 in", 4 * IN, 3 * IN),
    )
}
