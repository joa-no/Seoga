package com.joano.seoga.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkDao {

    @Transaction
    suspend fun saveWithCollections(work: WorkEntity, collectionIds: List<Long>): Long {
        val safeWork = if (collectionIds.isEmpty()) work.copy(hiddenFromAll = false) else work
        val id = if (safeWork.id == 0L) insertWork(safeWork) else {
            updateWork(safeWork)
            safeWork.id
        }
        setCollections(id, collectionIds, safeWork.hiddenFromAll)
        return id
    }

    @Transaction
    suspend fun setCollections(workId: Long, collectionIds: List<Long>, hidden: Boolean?) {
        deleteCrossRefsForWork(workId)
        if (collectionIds.isNotEmpty()) {
            insertCrossRefs(collectionIds.map { WorkCollectionCrossRef(workId, it) })
        }
        if (collectionIds.isEmpty()) updateHiddenFromAll(workId, false)
        else if (hidden != null) updateHiddenFromAll(workId, hidden)
    }

    @Transaction
    suspend fun moveFromAll(workIds: List<Long>, collectionId: Long) {
        addWorksToCollection(workIds, collectionId)
        updateHiddenFromAllForWorks(workIds, true)
    }

    @Transaction
    suspend fun moveToAll(workIds: List<Long>, collectionId: Long) {
        deleteCrossRefsForWorks(workIds, collectionId)
        updateHiddenFromAllForWorks(workIds, false)
    }

    @Transaction
    suspend fun setSafeHidden(workId: Long, hidden: Boolean) {
        updateHiddenFromAll(workId, hidden && getCollectionIdsForWork(workId).isNotEmpty())
    }


    @Query("SELECT * FROM works ORDER BY updatedAt DESC")
    fun getAllWorks(): Flow<List<WorkEntity>>

    @Transaction
    @Query("SELECT * FROM works ORDER BY updatedAt DESC")
    fun getAllWorksWithCollections(): Flow<List<WorkWithCollections>>

    @Query("SELECT * FROM works WHERE id = :id")
    fun getWorkById(id: Long): Flow<WorkEntity?>

    @Transaction
    @Query("SELECT * FROM works WHERE id = :id")
    fun getWorkWithCollectionsById(id: Long): Flow<WorkWithCollections?>

    @Transaction
    @Query("""
        SELECT w.* FROM works w 
        INNER JOIN work_collection_cross_ref r ON w.id = r.workId 
        WHERE r.collectionId = :collectionId 
        ORDER BY w.updatedAt DESC
    """)
    fun getWorksForCollection(collectionId: Long): Flow<List<WorkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWork(work: WorkEntity): Long

    @Update
    suspend fun updateWork(work: WorkEntity)

    @Delete
    suspend fun deleteWork(work: WorkEntity)

    @Query("DELETE FROM works WHERE id = :id")
    suspend fun deleteWorkById(id: Long)

    @Query("UPDATE works SET chaptersRead = :chaptersRead, updatedAt = :updatedAt WHERE id = :workId")
    suspend fun updateProgress(workId: Long, chaptersRead: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE works SET limitReleased = :released, updatedAt = :updatedAt WHERE id = :workId")
    suspend fun updateLimitReleased(workId: Long, released: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE works SET totalSource = :totalSource, totalChapters = :totalChapters, updatedAt = :updatedAt WHERE id = :workId")
    suspend fun updateTotalSourceAndChapters(workId: Long, totalSource: String, totalChapters: Int?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE works SET rating = :rating, updatedAt = :updatedAt WHERE id = :workId")
    suspend fun updateRating(workId: Long, rating: Float, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE works SET hiddenFromAll = :hidden, updatedAt = :updatedAt WHERE id = :workId")
    suspend fun updateHiddenFromAll(workId: Long, hidden: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE works SET hiddenFromAll = :hidden, updatedAt = :updatedAt WHERE id IN (:workIds)")
    suspend fun updateHiddenFromAllForWorks(workIds: List<Long>, hidden: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: WorkCollectionCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<WorkCollectionCrossRef>)

    @Delete
    suspend fun deleteCrossRef(crossRef: WorkCollectionCrossRef)

    @Query("DELETE FROM work_collection_cross_ref WHERE workId = :workId")
    suspend fun deleteCrossRefsForWork(workId: Long)

    @Query("DELETE FROM work_collection_cross_ref WHERE collectionId = :collectionId")
    suspend fun deleteCrossRefsForCollection(collectionId: Long)

    @Query("DELETE FROM work_collection_cross_ref WHERE workId = :workId AND collectionId = :collectionId")
    suspend fun deleteCrossRef(workId: Long, collectionId: Long)

    @Query("DELETE FROM work_collection_cross_ref WHERE workId IN (:workIds) AND collectionId = :collectionId")
    suspend fun deleteCrossRefsForWorks(workIds: List<Long>, collectionId: Long)

    @Transaction
    suspend fun addWorksToCollection(workIds: List<Long>, collectionId: Long) {
        val refs = workIds.map { WorkCollectionCrossRef(workId = it, collectionId = collectionId) }
        insertCrossRefs(refs)
    }

    @Transaction
    suspend fun moveWorksToCollection(workIds: List<Long>, fromCollectionId: Long, toCollectionId: Long) {
        deleteCrossRefsForWorks(workIds, fromCollectionId)
        val refs = workIds.map { WorkCollectionCrossRef(workId = it, collectionId = toCollectionId) }
        insertCrossRefs(refs)
    }

    @Transaction
    suspend fun applyBatchCollectionChanges(
        workIds: List<Long>,
        collectionsToAdd: List<Long>,
        collectionsToRemove: List<Long>,
        hiddenFromAllChange: Boolean?
    ) {
        if (workIds.isEmpty()) return
        for (colId in collectionsToAdd) {
            val refs = workIds.map { WorkCollectionCrossRef(workId = it, collectionId = colId) }
            insertCrossRefs(refs)
        }
        for (colId in collectionsToRemove) {
            deleteCrossRefsForWorks(workIds, colId)
        }
        if (hiddenFromAllChange != null) {
            updateHiddenFromAllForWorks(workIds, hiddenFromAllChange)
        }
        for (workId in workIds) {
            val remainingCols = getCollectionIdsForWork(workId)
            if (remainingCols.isEmpty()) {
                updateHiddenFromAll(workId, false)
            }
        }
    }

    @Query("SELECT collectionId FROM work_collection_cross_ref WHERE workId = :workId")
    suspend fun getCollectionIdsForWork(workId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM works")
    fun getTotalWorksCount(): Flow<Int>

    @Query("SELECT SUM(chaptersRead) FROM works")
    fun getTotalChaptersRead(): Flow<Int?>

    @Query("DELETE FROM works")
    suspend fun clearAllWorks()

    @Query("DELETE FROM work_collection_cross_ref")
    suspend fun clearAllCrossRefs()

    @Query("DELETE FROM work_collection_cross_ref WHERE workId IN (:workIds)")
    suspend fun deleteAllCrossRefsForWorks(workIds: List<Long>)

    @Query("DELETE FROM works WHERE id IN (:workIds)")
    suspend fun deleteWorksByIds(workIds: List<Long>)

    @Transaction
    suspend fun deleteWorks(workIds: List<Long>) {
        if (workIds.isEmpty()) return
        deleteAllCrossRefsForWorks(workIds)
        deleteWorksByIds(workIds)
    }

    @Query("SELECT * FROM works")
    suspend fun getAllWorksDirect(): List<WorkEntity>

    @Query("SELECT * FROM work_collection_cross_ref")
    suspend fun getAllCrossRefsDirect(): List<WorkCollectionCrossRef>
}
