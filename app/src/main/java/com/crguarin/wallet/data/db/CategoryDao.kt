package com.crguarin.wallet.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.crguarin.wallet.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT * FROM categories
        WHERE (:type IS NULL OR type = :type)
        ORDER BY is_archived, sort_order, id
        """,
    )
    fun observeAll(type: CategoryType? = null): Flow<List<CategoryEntity>>

    @Query(
        """
        SELECT * FROM categories
        WHERE (:type IS NULL OR type = :type)
        ORDER BY is_archived, sort_order, id
        """,
    )
    suspend fun getAll(type: CategoryType? = null): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE parent_id = :parentId ORDER BY sort_order, id")
    suspend fun getChildren(parentId: Long): List<CategoryEntity>

    @Query("UPDATE categories SET is_archived = 1 WHERE id = :id OR parent_id = :id")
    suspend fun archiveCategoryAndSubcategories(id: Long): Int

    @Query("UPDATE categories SET is_archived = 0 WHERE id = :id")
    suspend fun unarchive(id: Long): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    fun observeById(id: Long): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE system_key = :systemKey LIMIT 1")
    suspend fun getBySystemKey(systemKey: String): CategoryEntity?

    @Query("SELECT COALESCE(MAX(sort_order), -1) + 1 FROM categories WHERE parent_id IS NULL")
    suspend fun nextRootSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun update(category: CategoryEntity): Int

    @Delete
    suspend fun delete(category: CategoryEntity): Int

    @Query("SELECT COUNT(*) FROM categories WHERE parent_id = :id")
    suspend fun countChildren(id: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE category_id = :id")
    suspend fun countTransactions(id: Long): Int

    @Query("UPDATE transactions SET category_id = :toId WHERE category_id = :fromId")
    suspend fun reassignTransactions(
        fromId: Long,
        toId: Long,
    ): Int

    @Query(
        """
        SELECT EXISTS(
          SELECT 1 FROM categories
          WHERE type = :type
            AND ((:parentId IS NULL AND parent_id IS NULL) OR parent_id = :parentId)
            AND LOWER(TRIM(name)) = LOWER(TRIM(:name))
            AND (:excludeId IS NULL OR id != :excludeId)
        )
        """,
    )
    suspend fun existsSiblingWithName(
        name: String,
        type: CategoryType,
        parentId: Long?,
        excludeId: Long? = null,
    ): Boolean
}
