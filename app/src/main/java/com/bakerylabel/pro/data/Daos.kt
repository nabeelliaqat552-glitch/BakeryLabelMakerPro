package com.bakerylabel.pro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Insert
    suspend fun insert(c: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<CategoryEntity>)

    @Update
    suspend fun update(c: CategoryEntity)

    @Delete
    suspend fun delete(c: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun clear()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun get(id: Long): ProductEntity?

    @Query("SELECT COUNT(*) FROM products")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM products WHERE useCount > 0 ORDER BY useCount DESC, lastUsed DESC LIMIT :limit")
    fun observeFrequent(limit: Int): Flow<List<ProductEntity>>

    @Insert
    suspend fun insert(p: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<ProductEntity>)

    @Update
    suspend fun update(p: ProductEntity)

    @Delete
    suspend fun delete(p: ProductEntity)

    @Query("UPDATE products SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("UPDATE products SET useCount = useCount + 1, lastUsed = :now WHERE id = :id")
    suspend fun markUsed(id: Long, now: Long)

    @Query("DELETE FROM products")
    suspend fun clear()
}

@Dao
interface SavedLabelDao {
    @Query("SELECT * FROM saved_labels ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<SavedLabelEntity>>

    @Query("SELECT * FROM saved_labels ORDER BY updatedAt DESC")
    suspend fun getAll(): List<SavedLabelEntity>

    @Query("SELECT * FROM saved_labels ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SavedLabelEntity>>

    @Query("SELECT COUNT(*) FROM saved_labels")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM saved_labels WHERE id = :id")
    suspend fun get(id: Long): SavedLabelEntity?

    @Query("SELECT * FROM saved_labels WHERE id IN (:ids)")
    suspend fun getMany(ids: List<Long>): List<SavedLabelEntity>

    @Upsert
    suspend fun upsert(l: SavedLabelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<SavedLabelEntity>)

    @Query("UPDATE saved_labels SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun rename(id: Long, name: String, now: Long)

    @Query("DELETE FROM saved_labels WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE saved_labels SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("DELETE FROM saved_labels")
    suspend fun clear()
}

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    suspend fun getAll(): List<TemplateEntity>

    @Insert
    suspend fun insert(t: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TemplateEntity>)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM templates")
    suspend fun clear()
}

@Dao
interface ShopDao {
    @Query("SELECT * FROM shop_profile WHERE id = 1")
    fun observe(): Flow<ShopProfileEntity?>

    @Query("SELECT * FROM shop_profile WHERE id = 1")
    suspend fun get(): ShopProfileEntity?

    @Upsert
    suspend fun save(p: ShopProfileEntity)
}
