package com.joano.seoga.data.backup

import android.content.Context
import android.net.Uri
import com.joano.seoga.data.local.AppDatabase
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.WorkCollectionCrossRef
import com.joano.seoga.data.local.WorkEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@JsonClass(generateAdapter = true)
data class BackupManifest(
    val version: Int = 4,
    val app: String = "Seoga",
    val exportedAt: Long = System.currentTimeMillis(),
    val allCollectionName: String = "",
    val works: List<BackupWorkItem>,
    val collections: List<BackupCollectionItem>,
    val crossRefs: List<BackupCrossRefItem>
)

@JsonClass(generateAdapter = true)
data class BackupWorkItem(
    val id: Long,
    val title: String,
    val coverFileName: String? = null,
    val coverUri: String? = null,
    val authors: String = "",
    val artists: String = "",
    val type: String = "Mangá",
    val genres: String = "",
    val chaptersRead: Int = 0,
    val totalChapters: Int? = null,
    val synopsis: String = "",
    val personalNotes: String = "",
    val rating: Float = 0f,
    val externalId: String? = null,
    val externalSource: String? = null,
    val totalSource: String = "MANUAL",
    val limitReleased: Boolean = false,
    val hiddenFromAll: Boolean = false,
    val isAdult: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class BackupCollectionItem(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val orderPosition: Int
)

@JsonClass(generateAdapter = true)
data class BackupCrossRefItem(
    val workId: Long,
    val collectionId: Long
)

data class RestorePreview(
    val worksCount: Int,
    val collectionsCount: Int,
    val exportedAt: Long,
    val version: Int
)

class BackupManager(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupManifest::class.java)

    suspend fun exportBackup(outputStream: OutputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getInstance(context)
            val works = db.workDao().getAllWorksDirect()
            val collections = db.collectionDao().getAllCollectionsDirect()
            val crossRefs = db.workDao().getAllCrossRefsDirect()

            val coversDir = File(context.filesDir, "covers")
            val backupWorks = mutableListOf<BackupWorkItem>()
            val filesToInclude = mutableListOf<File>()

            for (work in works) {
                var coverFileName: String? = null
                if (work.coverUri != null && work.coverUri.startsWith("/")) {
                    val localFile = File(work.coverUri)
                    if (localFile.exists()) {
                        coverFileName = localFile.name
                        filesToInclude.add(localFile)
                    }
                }
                backupWorks.add(
                    BackupWorkItem(
                        id = work.id,
                        title = work.title,
                        coverFileName = coverFileName,
                        coverUri = if (coverFileName != null) null else work.coverUri,
                        authors = work.authors,
                        artists = work.artists,
                        type = work.type,
                        genres = work.genres,
                        chaptersRead = work.chaptersRead,
                        totalChapters = work.totalChapters,
                        synopsis = work.synopsis,
                        personalNotes = work.personalNotes,
                        rating = work.rating,
                        externalId = work.externalId,
                        externalSource = work.externalSource,
                        totalSource = work.totalSource,
                        limitReleased = work.limitReleased,
                        hiddenFromAll = work.hiddenFromAll,
                        isAdult = work.isAdult,
                        createdAt = work.createdAt,
                        updatedAt = work.updatedAt
                    )
                )
            }

            val backupCollections = collections.map {
                BackupCollectionItem(
                    id = it.id,
                    name = it.name,
                    createdAt = it.createdAt,
                    orderPosition = it.orderPosition
                )
            }

            val backupCrossRefs = crossRefs.map {
                BackupCrossRefItem(
                    workId = it.workId,
                    collectionId = it.collectionId
                )
            }

            val currentSettings = com.joano.seoga.data.preferences.UserPreferencesRepository(context)
                .userSettingsFlow.firstOrNull()
            val allCustomName = currentSettings?.allCollectionName ?: ""

            val manifest = BackupManifest(
                version = 3,
                allCollectionName = allCustomName,
                works = backupWorks,
                collections = backupCollections,
                crossRefs = backupCrossRefs
            )

            val jsonString = adapter.toJson(manifest)

            ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                // Write JSON manifest
                val jsonEntry = ZipEntry("library_backup.json")
                zipOut.putNextEntry(jsonEntry)
                zipOut.write(jsonString.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // Write covers
                for (file in filesToInclude) {
                    val entry = ZipEntry("covers/${file.name}")
                    zipOut.putNextEntry(entry)
                    FileInputStream(file).use { inStream ->
                        inStream.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }

            Result.success(works.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun readBackupPreview(inputStream: InputStream): Result<RestorePreview> = withContext(Dispatchers.IO) {
        try {
            var manifest: BackupManifest? = null
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == "library_backup.json") {
                        val json = zipIn.bufferedReader(Charsets.UTF_8).readText()
                        manifest = adapter.fromJson(json)
                        break
                    }
                    entry = zipIn.nextEntry
                }
            }

            if (manifest == null) {
                return@withContext Result.failure(IllegalArgumentException("Arquivo de backup inválido: 'library_backup.json' não encontrado."))
            }

            Result.success(
                RestorePreview(
                    worksCount = manifest!!.works.size,
                    collectionsCount = manifest!!.collections.size,
                    exportedAt = manifest!!.exportedAt,
                    version = manifest!!.version
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(
        inputStream: InputStream,
        replaceExisting: Boolean
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var manifestJson: String? = null
            val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
            val extractedCovers = mutableMapOf<String, String>() // coverFileName -> newAbsolutePath

            // Unpack ZIP
            val tempZipFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.zip")
            FileOutputStream(tempZipFile).use { out ->
                inputStream.copyTo(out)
            }

            ZipInputStream(BufferedInputStream(FileInputStream(tempZipFile))).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    if (entry.name == "library_backup.json") {
                        manifestJson = zipIn.bufferedReader(Charsets.UTF_8).readText()
                    } else if (entry.name.startsWith("covers/") && !entry.isDirectory) {
                        val fileName = entry.name.removePrefix("covers/")
                        val destFile = File(coversDir, fileName)
                        FileOutputStream(destFile).use { out ->
                            zipIn.copyTo(out)
                        }
                        extractedCovers[fileName] = destFile.absolutePath
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            tempZipFile.delete()

            if (manifestJson == null) {
                return@withContext Result.failure(IllegalArgumentException("Formato de backup inválido."))
            }

            val manifest = adapter.fromJson(manifestJson!!)
                ?: return@withContext Result.failure(IllegalArgumentException("Não foi possível ler o arquivo de dados."))

            val db = AppDatabase.getInstance(context)

            if (replaceExisting) {
                db.workDao().clearAllCrossRefs()
                db.workDao().clearAllWorks()
                db.collectionDao().clearAllCollections()
            }

            // Restore collections and keep map of oldId -> newId
            val collectionIdMap = mutableMapOf<Long, Long>()
            for (colItem in manifest.collections) {
                val entity = CollectionEntity(
                    id = if (replaceExisting) colItem.id else 0L,
                    name = colItem.name,
                    createdAt = colItem.createdAt,
                    orderPosition = colItem.orderPosition
                )
                val newId = db.collectionDao().insertCollection(entity)
                collectionIdMap[colItem.id] = newId
            }

            // Restore works and keep map of oldId -> newId
            val workIdMap = mutableMapOf<Long, Long>()
            for (workItem in manifest.works) {
                val resolvedCoverUri = when {
                    workItem.coverFileName != null && extractedCovers.containsKey(workItem.coverFileName) -> {
                        extractedCovers[workItem.coverFileName]
                    }
                    else -> workItem.coverUri
                }

                // Handle backward compatibility for v1 backups without totalSource/limitReleased
                val resolvedTotalSource = if (workItem.totalSource.isNotBlank()) {
                    workItem.totalSource
                } else if (workItem.externalSource == "MangaDex" && workItem.totalChapters != null) {
                    "MANGADEX"
                } else {
                    "MANUAL"
                }

                val workEntity = WorkEntity(
                    id = if (replaceExisting) workItem.id else 0L,
                    title = workItem.title,
                    coverUri = resolvedCoverUri,
                    authors = workItem.authors,
                    artists = workItem.artists,
                    type = workItem.type,
                    genres = workItem.genres,
                    chaptersRead = workItem.chaptersRead,
                    totalChapters = workItem.totalChapters,
                    synopsis = workItem.synopsis,
                    personalNotes = workItem.personalNotes,
                    rating = workItem.rating,
                    externalId = workItem.externalId,
                    externalSource = workItem.externalSource,
                    totalSource = resolvedTotalSource,
                    limitReleased = workItem.limitReleased,
                    hiddenFromAll = workItem.hiddenFromAll,
                    isAdult = workItem.isAdult,
                    createdAt = workItem.createdAt,
                    updatedAt = workItem.updatedAt
                )

                val newWorkId = db.workDao().insertWork(workEntity)
                workIdMap[workItem.id] = newWorkId
            }

            // Restore cross refs
            for (ref in manifest.crossRefs) {
                val actualWorkId = if (replaceExisting) ref.workId else workIdMap[ref.workId]
                val actualColId = if (replaceExisting) ref.collectionId else collectionIdMap[ref.collectionId]

                if (actualWorkId != null && actualColId != null) {
                    db.workDao().insertCrossRef(
                        WorkCollectionCrossRef(workId = actualWorkId, collectionId = actualColId)
                    )
                }
            }

            if (manifest.allCollectionName.isNotBlank() || replaceExisting) {
                val prefRepo = com.joano.seoga.data.preferences.UserPreferencesRepository(context)
                prefRepo.setAllCollectionName(manifest.allCollectionName)
            }

            Result.success(manifest.works.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
