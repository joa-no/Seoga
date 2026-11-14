package com.joano.seoga.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit

class MangaDexRepository : MetadataSource {

    override val id: String = "mangadex"
    override val displayName: String = "MangaDex"

    private val apiService: MangaDexApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.mangadex.org/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MangaDexApiService::class.java)
    }

    override suspend fun search(
        query: String,
        includeAdult: Boolean,
        preferredLanguage: String
    ): Result<List<MangaSearchResult>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return Result.success(emptyList())
        }

        val contentRatings = if (includeAdult) {
            listOf("safe", "suggestive", "erotica", "pornographic")
        } else {
            listOf("safe", "suggestive")
        }

        return try {
            val response = apiService.searchManga(
                title = trimmed,
                contentRatings = contentRatings
            )
            val items = response.data ?: emptyList()

            // Resolve target language tags: strict app language ("pt-BR", "es", "en"); default to "en", never device locale
            val targetLang = (preferredLanguage ?: "en").lowercase()

            val mapped = items.map { item ->
                val attrs = item.attributes

                // Extract primary title (prefer en, pt-br, or first available)
                val titlesMap = attrs?.title ?: emptyMap()
                val primaryTitle = titlesMap["en"]
                    ?: titlesMap["pt-br"]
                    ?: titlesMap["es"]
                    ?: titlesMap["es-la"]
                    ?: titlesMap["ja-ro"]
                    ?: titlesMap.values.firstOrNull()
                    ?: "Sem Título"

                // Extract alt titles
                val altTitlesList = mutableListOf<String>()
                attrs?.altTitles?.forEach { map ->
                    map.values.forEach { alt ->
                        if (alt.isNotBlank() && alt != primaryTitle && !altTitlesList.contains(alt)) {
                            altTitlesList.add(alt)
                        }
                    }
                }

                // Extract description: prefer current app language, then en, then first available
                val descMap = attrs?.description ?: emptyMap()
                val description = when {
                    targetLang.startsWith("pt") -> descMap["pt-br"] ?: descMap["pt"] ?: descMap["en"] ?: descMap.values.firstOrNull() ?: ""
                    targetLang.startsWith("es") -> descMap["es-la"] ?: descMap["es"] ?: descMap["en"] ?: descMap.values.firstOrNull() ?: ""
                    else -> descMap["en"] ?: descMap.values.firstOrNull() ?: ""
                }

                // Extract authors & artists from relationships
                val authors = mutableListOf<String>()
                val artists = mutableListOf<String>()
                var coverFileName: String? = null

                item.relationships?.forEach { rel ->
                    when (rel.type) {
                        "author" -> rel.attributes?.name?.let { if (it.isNotBlank()) authors.add(it) }
                        "artist" -> rel.attributes?.name?.let { if (it.isNotBlank()) artists.add(it) }
                        "cover_art" -> rel.attributes?.fileName?.let { coverFileName = it }
                    }
                }

                // Extract genres (tags where group == "genre" or "theme")
                val genres = mutableListOf<String>()
                attrs?.tags?.forEach { tag ->
                    val tagName = tag.attributes?.name?.get("en")
                        ?: tag.attributes?.name?.values?.firstOrNull()
                    if (!tagName.isNullOrBlank() && !genres.contains(tagName)) {
                        genres.add(tagName)
                    }
                }

                // Derive work type
                val origLang = attrs?.originalLanguage?.lowercase() ?: ""
                val workType = when (origLang) {
                    "ko" -> "Manhwa"
                    "zh", "zh-hk" -> "Manhua"
                    "ja" -> "Mangá"
                    else -> "Mangá"
                }

                // Parse total chapters if lastChapter is numeric
                val lastChapterStr = attrs?.lastChapter
                val totalChapters = lastChapterStr?.toIntOrNull()

                // Construct cover URL
                val coverUrl = coverFileName?.let { fileName ->
                    "https://uploads.mangadex.org/covers/${item.id}/$fileName.256.jpg"
                }

                // Determine if adult content (erotica or pornographic)
                val rating = attrs?.contentRating?.lowercase() ?: "safe"
                val isAdult = rating == "erotica" || rating == "pornographic"

                MangaSearchResult(
                    id = item.id,
                    title = primaryTitle,
                    altTitles = altTitlesList.take(5),
                    description = description,
                    type = workType,
                    authors = authors.distinct(),
                    artists = artists.distinct(),
                    genres = genres,
                    coverUrl = coverUrl,
                    totalChapters = totalChapters,
                    source = "MangaDex",
                    isAdult = isAdult
                )
            }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
