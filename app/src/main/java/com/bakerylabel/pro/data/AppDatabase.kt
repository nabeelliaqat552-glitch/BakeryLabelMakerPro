package com.bakerylabel.pro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        SavedLabelEntity::class,
        TemplateEntity::class,
        ShopProfileEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categories(): CategoryDao
    abstract fun products(): ProductDao
    abstract fun labels(): SavedLabelDao
    abstract fun templates(): TemplateDao
    abstract fun shop(): ShopDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "bakery_labels.db").build()
    }
}

/** Built-in starter products. No shelf life is attached — the user decides expiry dates. */
object SeedData {
    val sweets = listOf(
        "Barfi", "Gulab Jamun", "Kala Jamun", "Rasgulla", "Cham Cham", "Laddu", "Besan Laddu",
        "Motichoor Laddu", "Jalebi", "Kaju Katli", "Peda", "Milk Cake", "Kalakand", "Soan Papdi",
        "Patisa", "Gajar Halwa", "Habshi Halwa", "Khoya", "Coconut Barfi", "Chocolate Barfi",
        "Doodh Peda", "Pista Barfi", "Badam Barfi", "Rasmalai", "Sohan Halwa", "Mix Mithai",
    )
    val bakery = listOf(
        "Biscuits", "Butter Biscuits", "Cookies", "Chocolate Cookies", "Bakery Rusk", "Bread",
        "Milk Bread", "Cake", "Chocolate Cake", "Cream Cake", "Cupcakes", "Muffins", "Brownies",
        "Donuts", "Pastries", "Cream Rolls", "Puff Pastry", "Patties", "Croissants", "Pizza",
        "Sandwiches", "Nimco", "Fruit Cake", "Plain Cake", "Zeera Biscuits", "Nan Khatai",
    )
    val others: Map<Pair<String, String>, List<String>> = linkedMapOf(
        ("Chocolates" to CategoryGroup.OTHER) to listOf("Dark Chocolate", "Milk Chocolate", "Chocolate Box", "Truffles"),
        ("Dairy Products" to CategoryGroup.OTHER) to listOf("Fresh Cream", "Yogurt", "Butter", "Paneer"),
        ("Frozen Products" to CategoryGroup.OTHER) to listOf("Frozen Samosa", "Frozen Kebab", "Ice Cream Cake"),
        ("Snacks" to CategoryGroup.OTHER) to listOf("Samosa", "Spring Rolls", "Chicken Puff"),
        ("Namkeen" to CategoryGroup.OTHER) to listOf("Mix Namkeen", "Dal Moth", "Masala Peanuts", "Chips"),
        ("Confectionery" to CategoryGroup.OTHER) to listOf("Toffees", "Candy", "Lollipops"),
        ("Custom Food Products" to CategoryGroup.CUSTOM) to emptyList(),
    )

    suspend fun seed(db: AppDatabase) {
        val cats = db.categories()
        val prods = db.products()
        var order = 0
        val sweetsId = cats.insert(CategoryEntity(name = "Sweets & Mithai", groupType = CategoryGroup.SWEETS, sortOrder = order++))
        val bakeryId = cats.insert(CategoryEntity(name = "Bakery Products", groupType = CategoryGroup.BAKERY, sortOrder = order++))
        prods.insertAll(sweets.map { ProductEntity(name = it, categoryId = sweetsId) })
        prods.insertAll(bakery.map { ProductEntity(name = it, categoryId = bakeryId) })
        for ((key, items) in others) {
            val id = cats.insert(CategoryEntity(name = key.first, groupType = key.second, sortOrder = order++))
            prods.insertAll(items.map { ProductEntity(name = it, categoryId = id) })
        }
    }
}
