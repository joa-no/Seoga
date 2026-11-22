package com.joano.seoga.data.repository

import android.content.Context
import android.net.Uri
import com.joano.seoga.data.local.AppDatabase
import com.joano.seoga.data.local.CollectionDao
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.CollectionWithCount
import com.joano.seoga.data.local.WorkDao
import com.joano.seoga.data.local.WorkEntity
import com.joano.seoga.data.local.WorkWithCollections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.UUID

class LibraryRepository(
    private val workDao: WorkDao,
    private val collectionDao: CollectionDao,
    private val context: Context
) {

    val allWorks: Flow<List<WorkEntity>> = workDao.getAllWorks()
    val allWorksWithCollections: Flow<List<WorkWithCollections>> = workDao.getAllWorksWithCollections()
    val collectionsWithCounts: Flow<List<CollectionWithCount>> = collectionDao.getCollectionsWithCounts()
    val allCollections: Flow<List<CollectionEntity>> = collectionDao.getAllCollections()

    val totalWorksCount: Flow<Int> = workDao.getTotalWorksCount()
    val totalChaptersRead: Flow<Int?> = workDao.getTotalChaptersRead()
    val totalCollectionsCount: Flow<Int> = collectionDao.getTotalCollectionsCount()

    fun getWorkWithCollections(workId: Long): Flow<WorkWithCollections?> {
        return workDao.getWorkWithCollectionsById(workId)
    }

    fun getWorksForCollection(collectionId: Long): Flow<List<WorkEntity>> {
        return workDao.getWorksForCollection(collectionId)
    }

    suspend fun saveWork(work: WorkEntity, collectionIds: List<Long>): Long = withContext(Dispatchers.IO) {
        workDao.saveWithCollections(work, collectionIds)
    }

    suspend fun updateProgress(workId: Long, chaptersRead: Int) = withContext(Dispatchers.IO) {
        val current = workDao.getWorkById(workId).firstOrNull()
        val target = if (current != null && current.isLimitActive) {
            chaptersRead.coerceAtMost(current.totalChapters ?: chaptersRead)
        } else {
            chaptersRead
        }
        workDao.updateProgress(workId, target.coerceAtLeast(0), System.currentTimeMillis())
    }

    suspend fun releaseLimit(workId: Long) = withContext(Dispatchers.IO) {
        workDao.updateLimitReleased(workId, true)
    }

    suspend fun setTotalManual(workId: Long, totalChapters: Int?) = withContext(Dispatchers.IO) {
        workDao.updateTotalSourceAndChapters(workId, "MANUAL", totalChapters)
    }

    suspend fun updateRating(workId: Long, rating: Float) = withContext(Dispatchers.IO) {
        val clamped = rating.coerceIn(0f, 10f)
        val rounded = kotlin.math.round(clamped * 10f) / 10f
        workDao.updateRating(workId, rounded, System.currentTimeMillis())
    }

    suspend fun deleteWork(workId: Long) = withContext(Dispatchers.IO) {
        workDao.deleteWorks(listOf(workId))
    }

    suspend fun deleteWorks(workIds: List<Long>) = withContext(Dispatchers.IO) {
        if (workIds.isEmpty()) return@withContext
        workDao.deleteWorks(workIds)
    }

    suspend fun createCollection(name: String): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext -1L
        val maxPos = collectionDao.getMaxOrderPosition() ?: 0
        val entity = CollectionEntity(
            name = trimmed,
            orderPosition = maxPos + 1
        )
        collectionDao.insertCollection(entity)
    }

    suspend fun renameCollection(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return@withContext
        collectionDao.getAllCollectionsDirect().find { it.id == id }?.let { col ->
            collectionDao.updateCollection(col.copy(name = trimmed))
        }
    }

    suspend fun deleteCollection(collectionId: Long) = withContext(Dispatchers.IO) {
        // Deleting collection cascades to cross_ref, leaves works untouched
        collectionDao.deleteCollectionById(collectionId)
    }

    suspend fun reorderCollections(collections: List<CollectionEntity>) = withContext(Dispatchers.IO) {
        val updated = collections.mapIndexed { index, col ->
            col.copy(orderPosition = index)
        }
        collectionDao.updateCollections(updated)
    }

    suspend fun setWorkCollections(workId: Long, collectionIds: List<Long>, hiddenFromAll: Boolean? = null) = withContext(Dispatchers.IO) {
        workDao.setCollections(workId, collectionIds, hiddenFromAll)
    }

    suspend fun setWorkHiddenFromAll(workId: Long, hidden: Boolean) = withContext(Dispatchers.IO) {
        workDao.setSafeHidden(workId, hidden)
    }

    suspend fun addWorksToCollection(workIds: List<Long>, collectionId: Long) = withContext(Dispatchers.IO) {
        if (workIds.isEmpty()) return@withContext
        workDao.addWorksToCollection(workIds, collectionId)
    }

    suspend fun moveWorksToCollection(workIds: List<Long>, fromCollectionId: Long, toCollectionId: Long) = withContext(Dispatchers.IO) {
        if (workIds.isEmpty()) return@withContext
        workDao.moveWorksToCollection(workIds, fromCollectionId, toCollectionId)
    }

    suspend fun moveWorksFromAllToCollection(workIds: List<Long>, toCollectionId: Long) = withContext(Dispatchers.IO) {
        if (workIds.isEmpty()) return@withContext
        workDao.moveFromAll(workIds, toCollectionId)
    }

    suspend fun addWorksToAll(workIds: List<Long>) = withContext(Dispatchers.IO) {
        if (workIds.isEmpty()) return@withContext
        workDao.updateHiddenFromAllForWorks(workIds, false)
    }

    suspend fun moveWorksToAll(workIds: List<Long>, fromCollectionId: Long) = withContext(Dispatchers.IO) {
        if (workIds.isEmpty()) return@withContext
        workDao.moveToAll(workIds, fromCollectionId)
    }

    suspend fun applyBatchCollectionChanges(
        workIds: List<Long>,
        collectionsToAdd: List<Long>,
        collectionsToRemove: List<Long>,
        hiddenFromAllChange: Boolean?
    ) = withContext(Dispatchers.IO) {
        if (workIds.isEmpty()) return@withContext
        workDao.applyBatchCollectionChanges(workIds, collectionsToAdd, collectionsToRemove, hiddenFromAllChange)
    }

    // Persist cover image locally from Uri
    suspend fun persistCoverFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val fileName = "cover_${UUID.randomUUID()}.jpg"
            val destFile = File(coversDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Download remote cover (e.g. MangaDex) to local persistent storage
    suspend fun persistCoverFromUrl(imageUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val fileName = "cover_${UUID.randomUUID()}.jpg"
            val destFile = File(coversDir, fileName)

            val connection = URL(imageUrl).openConnection().apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            connection.getInputStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            // If download fails, fallback to keeping the remote URL
            imageUrl
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LibraryRepository? = null

        fun getInstance(context: Context): LibraryRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val instance = LibraryRepository(db.workDao(), db.collectionDao(), context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
