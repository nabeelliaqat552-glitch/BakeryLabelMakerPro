package com.bakerylabel.pro.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import com.bakerylabel.pro.model.ColorThemes
import com.bakerylabel.pro.model.FieldKey
import com.bakerylabel.pro.model.LabelContent
import com.bakerylabel.pro.model.LabelDesign
import com.bakerylabel.pro.render.LayoutEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class Repository(
    private val context: Context,
    val db: AppDatabase,
    val settings: SettingsRepository,
) {
    val categories = db.categories()
    val products = db.products()
    val labels = db.labels()
    val templates = db.templates()
    val shop = db.shop()

    suspend fun seedIfNeeded() {
        val s = settings.current()
        if (!s.seeded) {
            if (categories.count() == 0) SeedData.seed(db)
            settings.update { it.copy(seeded = true) }
        }
    }

    // ---------- Categories ----------
    suspend fun deleteCategory(c: CategoryEntity) = db.withTransaction {
        products.clearCategory(c.id)
        labels.clearCategory(c.id)
        categories.delete(c)
    }

    // ---------- Labels ----------
    /** Saves a label and returns its id. */
    suspend fun saveLabel(entity: SavedLabelEntity): Long {
        val now = System.currentTimeMillis()
        val e = entity.copy(updatedAt = now)
        val r = labels.upsert(e)
        return if (e.id != 0L) e.id else r
    }

    suspend fun duplicateLabel(id: Long): Long? {
        val l = labels.get(id) ?: return null
        val now = System.currentTimeMillis()
        return labels.upsert(l.copy(id = 0, name = l.name + " (copy)", createdAt = now, updatedAt = now))
    }

    // ---------- New label defaults ----------
    /** A fresh label prefilled from the shop profile and the default settings. */
    suspend fun newLabel(product: ProductEntity? = null): Pair<LabelContent, LabelDesign> {
        val s = settings.current()
        val p = shop.get() ?: ShopProfileEntity()
        val theme = ColorThemes.all.getOrElse(s.defaultThemeIndex) { ColorThemes.all.first() }
        var content = LabelContent(
            shopName = p.shopName,
            bakeryName = p.bakeryName,
            contact = p.contactLine(),
            license = if (p.license.isNotBlank()) "Food Lic# ${p.license}" else "",
        )
        if (product != null) content = applyProduct(content, product)
        val design = LabelDesign(
            widthMm = s.defaultWidthMm,
            heightMm = s.defaultHeightMm,
            bgColor = theme.bg, textColor = theme.text,
            accentColor = theme.accent, borderColor = theme.border,
            themeName = theme.name,
        )
        return content to LayoutEngine.arrange(design, content.visible)
    }

    /** Copies product info into a label. Expiry is filled only if the user saved a shelf life for this product. */
    fun applyProduct(c: LabelContent, p: ProductEntity): LabelContent {
        var n = c.copy(productName = p.name)
        if (p.netWeight.isNotBlank()) n = n.copy(netWeight = p.netWeight)
        if (p.price.isNotBlank()) n = n.copy(price = p.price)
        if (p.ingredients.isNotBlank()) n = n.copy(ingredients = p.ingredients, visible = n.visible + FieldKey.INGREDIENTS)
        if (p.storage.isNotBlank()) n = n.copy(storage = p.storage, visible = n.visible + FieldKey.STORAGE)
        if (p.barcode.isNotBlank()) n = n.copy(barcode = p.barcode, visible = n.visible + FieldKey.BARCODE)
        if (p.shelfLifeValue != null && p.shelfLifeValue > 0) {
            val rule = "${p.shelfLifeUnit ?: "D"}:${p.shelfLifeValue}"
            n = n.copy(expRule = rule, expEpochDay = com.bakerylabel.pro.util.Dates.applyRule(n.mfgEpochDay, rule))
        }
        return n
    }

    // ---------- Logo ----------
    private var logoCache: Pair<String, Bitmap>? = null

    fun loadLogo(path: String?): Bitmap? {
        if (path.isNullOrEmpty()) return null
        logoCache?.let { if (it.first == path) return it.second }
        val f = File(path)
        if (!f.exists()) return null
        val bmp = BitmapFactory.decodeFile(path) ?: return null
        logoCache = path to bmp
        return bmp
    }

    /** Copies a picked image into private storage, scaled to at most 800 px. */
    suspend fun importLogo(uri: Uri): String = withContext(Dispatchers.IO) {
        val src = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            ?: error("Could not read image")
        val max = 800f
        val scale = minOf(1f, max / maxOf(src.width, src.height))
        val bmp = if (scale < 1f) Bitmap.createScaledBitmap(src, (src.width * scale).toInt(), (src.height * scale).toInt(), true) else src
        val file = File(context.filesDir, "logo_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        context.filesDir.listFiles()?.filter { it.name.startsWith("logo_") && it.name != file.name }?.forEach { it.delete() }
        logoCache = null
        file.absolutePath
    }

    // ---------- Backup / restore ----------
    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "BakeryLabelMakerPro")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("categories", JSONArray().apply {
            categories.getAll().forEach {
                put(JSONObject().put("id", it.id).put("name", it.name).put("group", it.groupType).put("order", it.sortOrder))
            }
        })
        root.put("products", JSONArray().apply {
            products.getAll().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("name", p.name); put("cat", p.categoryId ?: JSONObject.NULL)
                    put("slv", p.shelfLifeValue ?: JSONObject.NULL); put("slu", p.shelfLifeUnit ?: JSONObject.NULL)
                    put("wt", p.netWeight); put("price", p.price); put("ingr", p.ingredients)
                    put("store", p.storage); put("bar", p.barcode); put("custom", p.isCustom)
                    put("use", p.useCount); put("last", p.lastUsed); put("created", p.createdAt)
                })
            }
        })
        root.put("labels", JSONArray().apply {
            labels.getAll().forEach { l ->
                put(JSONObject().apply {
                    put("id", l.id); put("name", l.name)
                    put("pid", l.productId ?: JSONObject.NULL); put("cat", l.categoryId ?: JSONObject.NULL)
                    put("content", l.contentJson); put("design", l.designJson)
                    put("created", l.createdAt); put("updated", l.updatedAt)
                })
            }
        })
        root.put("templates", JSONArray().apply {
            templates.getAll().forEach { t ->
                put(JSONObject().put("id", t.id).put("name", t.name).put("design", t.designJson).put("created", t.createdAt))
            }
        })
        shop.get()?.let { s ->
            val o = JSONObject().apply {
                put("shop", s.shopName); put("bakery", s.bakeryName); put("address", s.address)
                put("phone", s.phone); put("wa", s.whatsapp); put("web", s.website); put("lic", s.license)
            }
            s.logoPath?.let { path ->
                val f = File(path)
                if (f.exists()) o.put("logo", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
            }
            root.put("shop", o)
        }
        val st = settings.current()
        root.put("settings", JSONObject().apply {
            put("date", st.dateFormat); put("w", st.defaultWidthMm.toDouble()); put("h", st.defaultHeightMm.toDouble())
            put("theme", st.defaultThemeIndex); put("margin", st.sheetMarginMm.toDouble()); put("gap", st.sheetGapMm.toDouble())
        })
        root.toString()
    }

    /** Replaces all local data with the backup. Returns a short summary. */
    suspend fun restoreBackupJson(json: String): String = withContext(Dispatchers.IO) {
        val root = JSONObject(json)
        require(root.optString("app") == "BakeryLabelMakerPro") { "This file is not a Bakery Label Maker backup." }

        fun JSONObject.longOrNull(k: String): Long? = if (isNull(k) || !has(k)) null else getLong(k)
        fun JSONObject.intOrNull(k: String): Int? = if (isNull(k) || !has(k)) null else getInt(k)
        fun JSONObject.strOrNull(k: String): String? = if (isNull(k) || !has(k)) null else getString(k)

        val cats = mutableListOf<CategoryEntity>()
        root.optJSONArray("categories")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let {
                cats += CategoryEntity(it.getLong("id"), it.getString("name"), it.optString("group", CategoryGroup.CUSTOM), it.optInt("order"))
            }
        }
        val prods = mutableListOf<ProductEntity>()
        root.optJSONArray("products")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let {
                prods += ProductEntity(
                    id = it.getLong("id"), name = it.getString("name"), categoryId = it.longOrNull("cat"),
                    shelfLifeValue = it.intOrNull("slv"), shelfLifeUnit = it.strOrNull("slu"),
                    netWeight = it.optString("wt"), price = it.optString("price"), ingredients = it.optString("ingr"),
                    storage = it.optString("store"), barcode = it.optString("bar"), isCustom = it.optBoolean("custom"),
                    useCount = it.optInt("use"), lastUsed = it.optLong("last"), createdAt = it.optLong("created"),
                )
            }
        }
        val labs = mutableListOf<SavedLabelEntity>()
        root.optJSONArray("labels")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let {
                labs += SavedLabelEntity(
                    id = it.getLong("id"), name = it.getString("name"), productId = it.longOrNull("pid"),
                    categoryId = it.longOrNull("cat"), contentJson = it.getString("content"),
                    designJson = it.getString("design"), createdAt = it.optLong("created"), updatedAt = it.optLong("updated"),
                )
            }
        }
        val temps = mutableListOf<TemplateEntity>()
        root.optJSONArray("templates")?.let { a ->
            for (i in 0 until a.length()) a.getJSONObject(i).let {
                temps += TemplateEntity(it.getLong("id"), it.getString("name"), it.getString("design"), it.optLong("created"))
            }
        }
        var shopEntity: ShopProfileEntity? = null
        root.optJSONObject("shop")?.let { o ->
            var logoPath: String? = null
            if (o.has("logo")) {
                val file = File(context.filesDir, "logo_${System.currentTimeMillis()}.png")
                file.writeBytes(Base64.decode(o.getString("logo"), Base64.NO_WRAP))
                logoPath = file.absolutePath
            }
            shopEntity = ShopProfileEntity(
                shopName = o.optString("shop"), bakeryName = o.optString("bakery"), logoPath = logoPath,
                address = o.optString("address"), phone = o.optString("phone"), whatsapp = o.optString("wa"),
                website = o.optString("web"), license = o.optString("lic"),
            )
        }

        db.withTransaction {
            labels.clear(); products.clear(); categories.clear(); templates.clear()
            categories.insertAll(cats)
            products.insertAll(prods)
            labels.insertAll(labs)
            templates.insertAll(temps)
            shopEntity?.let { shop.save(it) }
        }
        logoCache = null
        root.optJSONObject("settings")?.let { o ->
            settings.update {
                it.copy(
                    dateFormat = o.optString("date", it.dateFormat),
                    defaultWidthMm = o.optDouble("w", it.defaultWidthMm.toDouble()).toFloat(),
                    defaultHeightMm = o.optDouble("h", it.defaultHeightMm.toDouble()).toFloat(),
                    defaultThemeIndex = o.optInt("theme", it.defaultThemeIndex),
                    sheetMarginMm = o.optDouble("margin", it.sheetMarginMm.toDouble()).toFloat(),
                    sheetGapMm = o.optDouble("gap", it.sheetGapMm.toDouble()).toFloat(),
                    seeded = true,
                )
            }
        }
        "${prods.size} products, ${cats.size} categories, ${labs.size} labels, ${temps.size} templates"
    }

    /** Product list as CSV for spreadsheet use. */
    suspend fun exportProductsCsv(): String = withContext(Dispatchers.IO) {
        val catNames = categories.getAll().associate { it.id to it.name }
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        buildString {
            appendLine("Name,Category,Shelf life,Net weight,Price,Ingredients,Storage,Barcode")
            products.getAll().forEach { p ->
                val sl = p.shelfLifeValue?.let { "$it ${if (p.shelfLifeUnit == "M") "months" else "days"}" } ?: ""
                appendLine(listOf(p.name, catNames[p.categoryId] ?: "", sl, p.netWeight, p.price, p.ingredients, p.storage, p.barcode).joinToString(",") { esc(it) })
            }
        }
    }
}
