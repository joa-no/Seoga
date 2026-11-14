package com.joano.seoga.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CollectionWithCount(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val orderPosition: Int,
    val workCount: Int
)

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections ORDER BY orderPosition ASC, createdAt ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("""
        SELECT c.id, c.name, c.createdAt, c.orderPosition, 
               COUNT(r.workId) as workCount
        FROM collections c
        LEFT JOIN work_collection_cross_ref r ON c.id = r.collectionId
        GROUP BY c.id
        ORDER BY c.orderPosition ASC, c.createdAt ASC
    """)
    fun getCollectionsWithCounts(): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE id = :id")
    fun getCollectionById(id: Long): Flow<CollectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollections(collections: List<CollectionEntity>): List<Long>

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Update
    suspend fun updateCollections(collections: List<CollectionEntity>)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollectionById(id: Long)

    @Query("SELECT COUNT(*) FROM collections")
    fun getTotalCollectionsCount(): Flow<Int>

    @Query("SELECT MAX(orderPosition) FROM collections")
    suspend fun getMaxOrderPosition(): Int?

    @Query("DELETE FROM collections")
    suspend fun clearAllCollections()

    @Query("SELECT * FROM collections")
    suspend fun getAllCollectionsDirect(): List<CollectionEntity>
}
