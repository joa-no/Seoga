package com.joano.seoga.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class AniListRateLimitException(message: String = "Muitas buscas, aguarde um instante.") : IOException(message)

class AniListSource : MetadataSource {

    override val id: String = "anilist"
    override val displayName: String = "AniList"

    private val apiService: AniListApiService by lazy {
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
            .baseUrl("https://graphql.anilist.co/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AniListApiService::class.java)
    }

    companion object {
        private const val GRAPHQL_QUERY = """
            query (${'$'}search: String, ${'$'}isAdult: Boolean) {
              Page(perPage: 20) {
                media(type: MANGA, search: ${'$'}search, isAdult: ${'$'}isAdult, format_not: NOVEL, sort: SEARCH_MATCH) {
                  id
                  title {
                    romaji
                    english
                    native
                  }
                  synonyms
                  description
                  format
                  countryOfOrigin
                  chapters
                  genres
                  isAdult
                  coverImage {
                    large
                  }
                  staff(perPage: 6) {
                    edges {
                      role
                      node {
                        name {
                          full
                        }
                      }
                    }
                  }
                }
              }
            }
        """
    }

    override suspend fun search(query: String, includeAdult: Boolean, languageTag: String): Result<List<MangaSearchResult>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return Result.success(emptyList())
        }

        val variables = mutableMapOf<String, Any?>("search" to trimmed)
        if (!includeAdult) {
            variables["isAdult"] = false
        }
        // If includeAdult is true, isAdult variable is omitted/null so AniList returns both adult and non-adult items

        val request = AniListGraphQLRequest(
            query = GRAPHQL_QUERY,
            variables = variables
        )

        return try {
            val response = apiService.queryManga(request)

            if (response.code() == 429) {
                return Result.failure(AniListRateLimitException())
            }

            if (!response.isSuccessful) {
                return Result.failure(IOException("AniList HTTP ${response.code()}: ${response.message()}"))
            }

            val body = response.body()
            val mediaList = body?.data?.Page?.media ?: emptyList()

            val results = mediaList.map { media ->
                // Title mapping: english ?: romaji ?: native
                val title = media.title?.english
                    ?: media.title?.romaji
                    ?: media.title?.native
                    ?: "Untitled"

                // Alt titles
                val altTitlesList = mutableListOf<String>()
                media.title?.romaji?.let { if (it != title && !altTitlesList.contains(it)) altTitlesList.add(it) }
                media.title?.english?.let { if (it != title && !altTitlesList.contains(it)) altTitlesList.add(it) }
                media.title?.native?.let { if (it != title && !altTitlesList.contains(it)) altTitlesList.add(it) }
                media.synonyms?.forEach { syn ->
                    if (syn.isNotBlank() && syn != title && !altTitlesList.contains(syn)) {
                        altTitlesList.add(syn)
                    }
                }

                // Type from country of origin: KR = Manhwa, CN/TW/HK = Manhua, others = Mangá
                val workType = when (media.countryOfOrigin?.uppercase()) {
                    "KR" -> "Manhwa"
                    "CN", "TW", "HK" -> "Manhua"
                    else -> "Mangá"
                }

                // Parse staff
                val authors = mutableListOf<String>()
                val artists = mutableListOf<String>()

                media.staff?.edges?.forEach { edge ->
                    val name = edge.node?.name?.full?.trim()
                    val role = edge.role?.lowercase() ?: ""
                    if (!name.isNullOrBlank()) {
                        val isStory = role.contains("story") || role.contains("original") || role.contains("writer") || role.contains("author")
                        val isArt = role.contains("art") || role.contains("illustrat") || role.contains("draw") || role.contains("design")
                        if (isStory && !authors.contains(name)) authors.add(name)
                        if (isArt && !artists.contains(name)) artists.add(name)
                        if (!isStory && !isArt && authors.isEmpty()) {
                            authors.add(name)
                        }
                    }
                }

                // Strip HTML tags from description
                val cleanDescription = stripHtml(media.description)

                MangaSearchResult(
                    id = media.id.toString(),
                    title = title,
                    altTitles = altTitlesList.take(5),
                    description = cleanDescription,
                    type = workType,
                    authors = authors.distinct(),
                    artists = artists.distinct(),
                    genres = media.genres ?: emptyList(),
                    coverUrl = media.coverImage?.large,
                    totalChapters = media.chapters,
                    source = "AniList",
                    isAdult = media.isAdult == true
                )
            }

            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun stripHtml(html: String?): String {
        if (html.isNullOrBlank()) return ""
        return html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .trim()
    }
}
