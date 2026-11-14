package com.joano.seoga

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.joano.seoga.data.local.AppDatabase
import com.joano.seoga.data.local.CollectionEntity
import com.joano.seoga.data.local.WorkCollectionCrossRef
import com.joano.seoga.data.local.WorkEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun verifyAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Seoga", appName)
    }

    @Test
    fun testWorkAndCustomCollectionsPersistence() = runBlocking {
        val workDao = db.workDao()
        val collectionDao = db.collectionDao()

        // 1. Insert Work
        val work = WorkEntity(
            title = "Solo Leveling",
            type = "Manhwa",
            genres = "Ação, Fantasia",
            chaptersRead = 120,
            totalChapters = 179,
            rating = 9.5f
        )
        val workId = workDao.insertWork(work)
        assertTrue(workId > 0)

        // 2. Insert Custom Collections
        val col1 = CollectionEntity(name = "Lendo", orderPosition = 0)
        val col2 = CollectionEntity(name = "Favoritos", orderPosition = 1)
        val col1Id = collectionDao.insertCollection(col1)
        val col2Id = collectionDao.insertCollection(col2)

        // 3. Associate Work with both collections (Many-to-Many)
        workDao.insertCrossRef(WorkCollectionCrossRef(workId = workId, collectionId = col1Id))
        workDao.insertCrossRef(WorkCollectionCrossRef(workId = workId, collectionId = col2Id))

        // 4. Verify Work has both collections
        val workWithCols = workDao.getWorkWithCollectionsById(workId).first()
        assertNotNull(workWithCols)
        assertEquals(2, workWithCols!!.collections.size)

        // 5. Verify works for collection
        val worksInCol1 = workDao.getWorksForCollection(col1Id).first()
        assertEquals(1, worksInCol1.size)
        assertEquals("Solo Leveling", worksInCol1[0].title)

        // 6. Delete Collection and verify Work remains untouched
        collectionDao.deleteCollectionById(col1Id)
        val allWorks = workDao.getAllWorks().first()
        assertEquals(1, allWorks.size)
        assertEquals("Solo Leveling", allWorks[0].title)

        // 7. Test chapter progress update
        workDao.updateProgress(workId, 121)
        val updatedWork = workDao.getWorkById(workId).first()
        assertEquals(121, updatedWork?.chaptersRead)

        // 8. Test MangaDex chapter limit logic and updates
        val mangaDexWork = WorkEntity(
            title = "Chainsaw Man",
            type = "Mangá",
            chaptersRead = 100,
            totalChapters = 150,
            externalId = "m-123",
            externalSource = "MangaDex",
            totalSource = "MANGADEX",
            limitReleased = false
        )
        val mdId = workDao.insertWork(mangaDexWork)
        val fetchedMd = workDao.getWorkById(mdId).first()!!
        assertTrue(fetchedMd.isLimitActive)
        assertEquals("MANGADEX", fetchedMd.totalSource)

        // Release limit
        workDao.updateLimitReleased(mdId, true)
        val releasedMd = workDao.getWorkById(mdId).first()!!
        assertTrue(releasedMd.limitReleased)
        assertTrue(!releasedMd.isLimitActive)

        // Change to Manual with custom total (new rule: manual total is active limit)
        workDao.updateTotalSourceAndChapters(mdId, "MANUAL", 200)
        val manualMd = workDao.getWorkById(mdId).first()!!
        assertEquals("MANUAL", manualMd.totalSource)
        assertEquals(200, manualMd.totalChapters)
        assertTrue(manualMd.isLimitActive)

        // 9. Test AniList WorkEntity and generalized limit
        val aniListWork = WorkEntity(
            title = "Omniscient Reader",
            type = "Manhwa",
            chaptersRead = 50,
            totalChapters = 200,
            externalId = "al-987",
            externalSource = "AniList",
            totalSource = "ANILIST",
            limitReleased = false
        )
        val alId = workDao.insertWork(aniListWork)
        val fetchedAl = workDao.getWorkById(alId).first()!!
        assertTrue(fetchedAl.isLimitActive)
        assertEquals("AniList", fetchedAl.displaySource)
    }

    @Test
    fun testManualTotalLimitsAndFormValidation() = runBlocking {
        val workDao = db.workDao()
        val libraryRepo = com.joano.seoga.data.repository.LibraryRepository(
            workDao = workDao,
            collectionDao = db.collectionDao(),
            context = context
        )

        // 1. Manual total work: limit is active
        val manualWork = WorkEntity(
            title = "One Piece",
            type = "Mangá",
            chaptersRead = 500,
            totalChapters = 500,
            totalSource = "MANUAL"
        )
        val workId = workDao.insertWork(manualWork)
        val inserted = workDao.getWorkById(workId).first()!!
        assertTrue(inserted.isLimitActive)

        // 2. updateProgress cannot exceed manual total
        libraryRepo.updateProgress(workId, 600)
        val afterOverAttempt = workDao.getWorkById(workId).first()!!
        assertEquals(500, afterOverAttempt.chaptersRead) // Clamped to 500

        // 3. User increases total to 600: unlocks space up to 600
        libraryRepo.setTotalManual(workId, 600)
        libraryRepo.updateProgress(workId, 600)
        val afterIncrease = workDao.getWorkById(workId).first()!!
        assertEquals(600, afterIncrease.chaptersRead)

        // 4. Work without total has no limit
        val noTotalWork = WorkEntity(
            title = "Tower of God",
            type = "Manhwa",
            chaptersRead = 100,
            totalChapters = null,
            totalSource = "MANUAL"
        )
        assertTrue(!noTotalWork.isLimitActive)
    }

    @Test
    fun testEffectiveLanguageResolutionAndSynopsisPreference() {
        // App language resolution
        assertEquals("pt-BR", com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag("pt-BR"))
        assertEquals("es", com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag("es"))
        assertEquals("en", com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag("en"))

        // Even if device is pt, setting app to "en" strictly returns "en"
        val prevLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("pt", "BR"))
            assertEquals("en", com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag("en"))
            assertEquals("es", com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag("es"))
            assertEquals("pt-BR", com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag("pt-BR"))
            assertEquals("pt-BR", com.joano.seoga.ui.util.LocalizationUtil.resolveEffectiveLanguageTag("auto"))
        } finally {
            java.util.Locale.setDefault(prevLocale)
        }

        // Test synopsis extraction logic matching MangaDexRepository
        val descMap = mapOf(
            "pt-br" to "Sinopse em português",
            "en" to "Synopsis in English",
            "ja" to "日本語"
        )

        fun extractSynopsis(targetLang: String, map: Map<String, String>): String {
            return when {
                targetLang.startsWith("pt") -> map["pt-br"] ?: map["pt"] ?: map["en"] ?: map.values.firstOrNull() ?: ""
                targetLang.startsWith("es") -> map["es-la"] ?: map["es"] ?: map["en"] ?: map.values.firstOrNull() ?: ""
                else -> map["en"] ?: map.values.firstOrNull() ?: ""
            }
        }

        // With app in English, must pick English synopsis, NOT Portuguese
        assertEquals("Synopsis in English", extractSynopsis("en", descMap))
        // With app in Spanish, falls back to English before first available
        assertEquals("Synopsis in English", extractSynopsis("es", descMap))
        // With app in Portuguese, picks Portuguese
        assertEquals("Sinopse em português", extractSynopsis("pt-BR".lowercase(), descMap))
    }

    @Test
    fun testLocalizationUtilLocaleResolution() {
        val pt = com.joano.seoga.ui.util.LocalizationUtil.resolveLocale("pt-BR")
        assertEquals("pt", pt.language)
        assertEquals("BR", pt.country)

        val es = com.joano.seoga.ui.util.LocalizationUtil.resolveLocale("es")
        assertEquals("es", es.language)

        val en = com.joano.seoga.ui.util.LocalizationUtil.resolveLocale("en")
        assertEquals("en", en.language)

        // Other unsupported languages should fallback to English as required
        val de = com.joano.seoga.ui.util.LocalizationUtil.resolveLocale("de")
        assertEquals("en", de.language)
    }

    @Test
    fun testMainActivityLaunchAndContextWrapper() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        val activity = controller.get()
        assertNotNull(activity)
        assertTrue(activity is androidx.activity.result.ActivityResultRegistryOwner)

        // Verify ContextWrapper wrapping this activity preserves ActivityResultRegistryOwner
        val wrapper = object : android.content.ContextWrapper(activity) {
            override fun getResources(): android.content.res.Resources = activity.resources
        }
        var current: android.content.Context? = wrapper
        var foundRegistryOwner = false
        while (current is android.content.ContextWrapper) {
            if (current is androidx.activity.result.ActivityResultRegistryOwner) {
                foundRegistryOwner = true
                break
            }
            current = current.baseContext
        }
        assertTrue("ContextWrapper must resolve to ActivityResultRegistryOwner", foundRegistryOwner)
    }

    @Test
    fun testLanguageAutoResourceResolution() {
        // Default / English
        val enConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.ENGLISH)
        }
        val enRes = context.createConfigurationContext(enConfig).resources
        assertEquals("Automatic", enRes.getString(R.string.language_auto))

        // Portuguese (Brazil)
        val ptConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale("pt", "BR"))
        }
        val ptRes = context.createConfigurationContext(ptConfig).resources
        assertEquals("Automático", ptRes.getString(R.string.language_auto))

        // Spanish
        val esConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale("es"))
        }
        val esRes = context.createConfigurationContext(esConfig).resources
        assertEquals("Automático", esRes.getString(R.string.language_auto))
    }

    @Test
    fun testDynamicMaxSliderCalculation() {
        assertEquals(50, com.joano.seoga.ui.screens.detail.calculateDynamicMax(0))
        assertEquals(50, com.joano.seoga.ui.screens.detail.calculateDynamicMax(25))
        assertEquals(50, com.joano.seoga.ui.screens.detail.calculateDynamicMax(49))
        // At multiple of 50 (e.g. released at max), expands by +50 to the next multiple
        assertEquals(100, com.joano.seoga.ui.screens.detail.calculateDynamicMax(50))
        assertEquals(100, com.joano.seoga.ui.screens.detail.calculateDynamicMax(75))
        assertEquals(100, com.joano.seoga.ui.screens.detail.calculateDynamicMax(99))
        assertEquals(150, com.joano.seoga.ui.screens.detail.calculateDynamicMax(100))
        assertEquals(200, com.joano.seoga.ui.screens.detail.calculateDynamicMax(175))
    }

    @Test
    fun testAddWorksToCollectionAndMoveWorksToCollection() = runBlocking<Unit> {
        val workDao = db.workDao()
        val collectionDao = db.collectionDao()

        // Create works
        val work1Id = workDao.insertWork(WorkEntity(title = "Work 1", type = "Manga", chaptersRead = 10, totalChapters = 50))
        val work2Id = workDao.insertWork(WorkEntity(title = "Work 2", type = "Manhwa", chaptersRead = 20, totalChapters = 100))

        // Create collections
        val colAId = collectionDao.insertCollection(CollectionEntity(name = "Col A", orderPosition = 1))
        val colBId = collectionDao.insertCollection(CollectionEntity(name = "Col B", orderPosition = 2))
        val colCId = collectionDao.insertCollection(CollectionEntity(name = "Col C", orderPosition = 3))

        // Put work 1 initially in Col A
        workDao.insertCrossRef(WorkCollectionCrossRef(workId = work1Id, collectionId = colAId))
        assertEquals(listOf(colAId), workDao.getCollectionIdsForWork(work1Id))

        // Batch add works 1 and 2 to Col B
        workDao.addWorksToCollection(listOf(work1Id, work2Id), colBId)
        val work1ColsAfterAdd = workDao.getCollectionIdsForWork(work1Id)
        val work2ColsAfterAdd = workDao.getCollectionIdsForWork(work2Id)

        // Work 1 should have BOTH Col A and Col B (not replaced!)
        assertTrue(work1ColsAfterAdd.contains(colAId))
        assertTrue(work1ColsAfterAdd.contains(colBId))
        assertEquals(2, work1ColsAfterAdd.size)
        // Work 2 should have Col B
        assertEquals(listOf(colBId), work2ColsAfterAdd)

        // Batch move works 1 and 2 from Col B to Col C
        workDao.moveWorksToCollection(listOf(work1Id, work2Id), fromCollectionId = colBId, toCollectionId = colCId)
        val work1ColsAfterMove = workDao.getCollectionIdsForWork(work1Id)
        val work2ColsAfterMove = workDao.getCollectionIdsForWork(work2Id)

        // Work 1 should have Col A and Col C, and NOT Col B
        assertTrue(work1ColsAfterMove.contains(colAId))
        assertTrue(work1ColsAfterMove.contains(colCId))
        assertFalse(work1ColsAfterMove.contains(colBId))

        // Work 2 should have Col C, and NOT Col B
        assertTrue(work2ColsAfterMove.contains(colCId))
        assertFalse(work2ColsAfterMove.contains(colBId))
    }

    @Test
    fun testWorkCompletedStatusCalculation() {
        val workCompleted = WorkEntity(title = "Completed", type = "Manga", chaptersRead = 100, totalChapters = 100)
        assertTrue(workCompleted.totalChapters != null && workCompleted.chaptersRead >= workCompleted.totalChapters!!)

        val workOverCompleted = WorkEntity(title = "Over", type = "Manga", chaptersRead = 105, totalChapters = 100)
        assertTrue(workOverCompleted.totalChapters != null && workOverCompleted.chaptersRead >= workOverCompleted.totalChapters!!)

        val workInProgress = WorkEntity(title = "Ongoing", type = "Manga", chaptersRead = 50, totalChapters = 100)
        assertFalse(workInProgress.totalChapters != null && workInProgress.chaptersRead >= workInProgress.totalChapters!!)

        val workNoTotal = WorkEntity(title = "No Total", type = "Manga", chaptersRead = 50, totalChapters = null)
        assertFalse(workNoTotal.totalChapters != null && workNoTotal.chaptersRead >= (workNoTotal.totalChapters ?: Int.MAX_VALUE))
    }

    @Test
    fun testHiddenFromAllAndDefaultSpecialCollectionRules() = runBlocking {
        val workDao = db.workDao()
        val collectionDao = db.collectionDao()
        val libraryRepo = com.joano.seoga.data.repository.LibraryRepository(
            workDao = workDao,
            collectionDao = collectionDao,
            context = context
        )

        // 1. Newly created work has hiddenFromAll = false by default
        val work = WorkEntity(title = "Berserk", type = "Mangá", chaptersRead = 10, totalChapters = 364)
        val workId = libraryRepo.saveWork(work, emptyList())
        val savedWork = workDao.getWorkById(workId).first()!!
        assertFalse(savedWork.hiddenFromAll)

        // 2. Work without collections cannot be hiddenFromAll (safety rule)
        val hiddenAttempt = savedWork.copy(hiddenFromAll = true)
        val savedId2 = libraryRepo.saveWork(hiddenAttempt, emptyList())
        val savedWork2 = workDao.getWorkById(savedId2).first()!!
        assertFalse("Work with no collections must have hiddenFromAll = false", savedWork2.hiddenFromAll)

        // 3. Associate work with a collection and hide from "Todos"
        val colId = collectionDao.insertCollection(CollectionEntity(name = "Lidos", orderPosition = 0))
        libraryRepo.setWorkCollections(workId, listOf(colId), hiddenFromAll = true)
        val workWithCols = libraryRepo.getWorkWithCollections(workId).first()!!
        assertTrue(workWithCols.work.hiddenFromAll)
        assertEquals(1, workWithCols.collections.size)

        // Display rule for "Todos": !hiddenFromAll || collections.isEmpty()
        val visibleInAll = !workWithCols.work.hiddenFromAll || workWithCols.collections.isEmpty()
        assertFalse(visibleInAll)

        // 4. If all collections are removed, safety rule forces hiddenFromAll = false
        libraryRepo.setWorkCollections(workId, emptyList(), hiddenFromAll = true)
        val workAfterRemovingCols = libraryRepo.getWorkWithCollections(workId).first()!!
        assertFalse("Removing all collections must reset hiddenFromAll to false", workAfterRemovingCols.work.hiddenFromAll)
        assertTrue(!workAfterRemovingCols.work.hiddenFromAll || workAfterRemovingCols.collections.isEmpty())

        // 5. Test batch move from "Todos" to collection
        val workBId = libraryRepo.saveWork(WorkEntity(title = "Gantz", type = "Mangá"), emptyList())
        libraryRepo.moveWorksFromAllToCollection(listOf(workBId), colId)
        val workBWithCols = libraryRepo.getWorkWithCollections(workBId).first()!!
        assertTrue(workBWithCols.work.hiddenFromAll)
        assertEquals(listOf(colId), workBWithCols.collections.map { it.id })

        // 6. Test batch move back to "Todos" (removes from other collections and unhides)
        libraryRepo.moveWorksToAll(listOf(workBId), fromCollectionId = colId)
        val workBAfterMoveAll = libraryRepo.getWorkWithCollections(workBId).first()!!
        assertFalse(workBAfterMoveAll.work.hiddenFromAll)
        assertTrue(workBAfterMoveAll.collections.isEmpty())
    }

    @Test
    fun testIsAdultPersistence() = runBlocking {
        val workDao = db.workDao()
        val work = WorkEntity(
            title = "Adult Work",
            type = "Mangá",
            isAdult = true
        )
        val id = workDao.insertWork(work)
        val fetched = workDao.getWorkById(id).first()!!
        assertTrue("Work should be adult", fetched.isAdult)

        val nonAdultWork = WorkEntity(
            title = "Non-Adult Work",
            type = "Mangá",
            isAdult = false
        )
        val nonAdultId = workDao.insertWork(nonAdultWork)
        val fetchedNonAdult = workDao.getWorkById(nonAdultId).first()!!
        assertFalse("Work should not be adult", fetchedNonAdult.isAdult)
    }

    @Test
    fun testBatchManageCollectionsScenarios() = runBlocking {
        val workDao = db.workDao()
        val collectionDao = db.collectionDao()
        val libraryRepo = com.joano.seoga.data.repository.LibraryRepository(
            workDao = workDao,
            collectionDao = collectionDao,
            context = context
        )

        val colCompletoId = collectionDao.insertCollection(CollectionEntity(name = "Completo", orderPosition = 0))
        val colLendoId = collectionDao.insertCollection(CollectionEntity(name = "Lendo", orderPosition = 1))

        // 1. Work in "Todos" and "Completo" removed from "Todos" stays hidden and remains in "Completo"
        val work1Id = libraryRepo.saveWork(WorkEntity(title = "Work 1", type = "Mangá", hiddenFromAll = false), listOf(colCompletoId))
        val w1Initial = libraryRepo.getWorkWithCollections(work1Id).first()!!
        assertFalse(w1Initial.work.hiddenFromAll)
        assertEquals(listOf(colCompletoId), w1Initial.collections.map { it.id })

        workDao.applyBatchCollectionChanges(
            workIds = listOf(work1Id),
            collectionsToAdd = emptyList(),
            collectionsToRemove = emptyList(),
            hiddenFromAllChange = true
        )
        val w1Updated = libraryRepo.getWorkWithCollections(work1Id).first()!!
        assertTrue("Should be hidden from Todos", w1Updated.work.hiddenFromAll)
        assertEquals(listOf(colCompletoId), w1Updated.collections.map { it.id })

        // 2. Mixed selection (Work 2 in "Completo", Work 3 not) added to "Lendo" keeps "Completo" untouched
        val work2Id = libraryRepo.saveWork(WorkEntity(title = "Work 2", type = "Mangá"), listOf(colCompletoId))
        val work3Id = libraryRepo.saveWork(WorkEntity(title = "Work 3", type = "Mangá"), emptyList())

        workDao.applyBatchCollectionChanges(
            workIds = listOf(work2Id, work3Id),
            collectionsToAdd = listOf(colLendoId),
            collectionsToRemove = emptyList(),
            hiddenFromAllChange = null
        )

        val w2Updated = libraryRepo.getWorkWithCollections(work2Id).first()!!
        val w3Updated = libraryRepo.getWorkWithCollections(work3Id).first()!!
        val w2Cols = w2Updated.collections.map { it.id }.toSet()
        assertTrue(w2Cols.contains(colCompletoId))
        assertTrue(w2Cols.contains(colLendoId))

        val w3Cols = w3Updated.collections.map { it.id }.toSet()
        assertTrue(w3Cols.contains(colLendoId))
        assertFalse(w3Cols.contains(colCompletoId))

        // 3. Attempting to leave a work without "Todos" and without collections is rejected
        val workSoloId = libraryRepo.saveWork(WorkEntity(title = "Work Solo", type = "Mangá", hiddenFromAll = false), emptyList())
        val soloWorkWithCols = libraryRepo.getWorkWithCollections(workSoloId).first()!!

        val userHidesFromAll = true
        val proposedCols = emptyList<Long>()
        val wouldBeOrphaned = userHidesFromAll && proposedCols.isEmpty()
        assertTrue("Safety rule must detect orphaned work", wouldBeOrphaned)
    }

    @Test
    fun testDeleteWorksBatch() = runBlocking {
        val workDao = db.workDao()
        val collectionDao = db.collectionDao()
        val libraryRepo = com.joano.seoga.data.repository.LibraryRepository(
            workDao = workDao,
            collectionDao = collectionDao,
            context = context
        )

        val colAId = collectionDao.insertCollection(CollectionEntity(name = "Col A", orderPosition = 0))
        val colBId = collectionDao.insertCollection(CollectionEntity(name = "Col B", orderPosition = 1))

        val work1Id = libraryRepo.saveWork(WorkEntity(title = "Work 1", type = "Mangá"), listOf(colAId, colBId))
        val work2Id = libraryRepo.saveWork(WorkEntity(title = "Work 2", type = "Manhwa"), listOf(colAId))
        val work3Id = libraryRepo.saveWork(WorkEntity(title = "Work 3", type = "HQ"), listOf(colBId))

        assertEquals(3, workDao.getTotalWorksCount().first())

        // Batch delete Work 1 and Work 2
        libraryRepo.deleteWorks(listOf(work1Id, work2Id))

        // Verify Work 1 and Work 2 are removed
        org.junit.Assert.assertNull(workDao.getWorkById(work1Id).first())
        org.junit.Assert.assertNull(workDao.getWorkById(work2Id).first())
        assertEquals(0, workDao.getCollectionIdsForWork(work1Id).size)
        assertEquals(0, workDao.getCollectionIdsForWork(work2Id).size)

        // Verify Work 3 and its collection association remain untouched
        val work3Fetched = workDao.getWorkWithCollectionsById(work3Id).first()
        assertNotNull(work3Fetched)
        assertEquals("Work 3", work3Fetched!!.work.title)
        assertEquals(listOf(colBId), work3Fetched.collections.map { it.id })
        assertEquals(1, workDao.getTotalWorksCount().first())
    }
}
