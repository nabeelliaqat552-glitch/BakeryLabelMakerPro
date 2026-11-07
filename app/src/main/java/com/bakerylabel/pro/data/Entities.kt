package com.bakerylabel.pro.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Category group used by the dashboard shortcuts. */
object CategoryGroup {
    const val SWEETS = "SWEETS"
    const val BAKERY = "BAKERY"
    const val OTHER = "OTHER"
    const val CUSTOM = "CUSTOM"
    val all = listOf(SWEETS, BAKERY, OTHER, CUSTOM)
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val groupType: String = CategoryGroup.CUSTOM,
    val sortOrder: Int = 0,
)

/**
 * A product. Shelf life is optional and only ever set by the user; the app never assumes one.
 * shelfLifeUnit is "D" (days) or "M" (months).
 */
@Entity(tableName = "products", indices = [Index("categoryId"), Index("name")])
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val categoryId: Long? = null,
    val shelfLifeValue: Int? = null,
    val shelfLifeUnit: String? = null,
    val netWeight: String = "",
    val price: String = "",
    val ingredients: String = "",
    val storage: String = "",
    val barcode: String = "",
    val isCustom: Boolean = false,
    val useCount: Int = 0,
    val lastUsed: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "saved_labels", indices = [Index("productId"), Index("categoryId")])
data class SavedLabelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val productId: Long? = null,
    val categoryId: Long? = null,
    val contentJson: String,
    val designJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** A design the user saved as a reusable template. Built-in templates live in code. */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val designJson: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "shop_profile")
data class ShopProfileEntity(
    @PrimaryKey val id: Int = 1,
    val shopName: String = "",
    val bakeryName: String = "",
    val logoPath: String? = null,
    val address: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val website: String = "",
    val license: String = "",
) {
    fun contactLine(): String = listOf(
        phone.takeIf { it.isNotBlank() }?.let { "Ph: $it" },
        whatsapp.takeIf { it.isNotBlank() && it != phone }?.let { "WA: $it" },
        website.takeIf { it.isNotBlank() },
        address.takeIf { it.isNotBlank() },
    ).filterNotNull().joinToString(" | ")
}
