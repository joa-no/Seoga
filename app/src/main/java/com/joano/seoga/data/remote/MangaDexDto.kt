package com.joano.seoga.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MangaDexResponse(
    val result: String? = null,
    val response: String? = null,
    val data: List<MangaDexItemDto>? = null
)

@JsonClass(generateAdapter = true)
data class MangaDexItemDto(
    val id: String,
    val type: String,
    val attributes: MangaAttributesDto? = null,
    val relationships: List<RelationshipDto>? = null
)

@JsonClass(generateAdapter = true)
data class MangaAttributesDto(
    val title: Map<String, String>? = null,
    val altTitles: List<Map<String, String>>? = null,
    val description: Map<String, String>? = null,
    val originalLanguage: String? = null,
    val lastVolume: String? = null,
    val lastChapter: String? = null,
    val status: String? = null,
    val contentRating: String? = null,
    val tags: List<TagDto>? = null
)

@JsonClass(generateAdapter = true)
data class TagDto(
    val id: String,
    val attributes: TagAttributesDto? = null
)

@JsonClass(generateAdapter = true)
data class TagAttributesDto(
    val name: Map<String, String>? = null,
    val group: String? = null
)

@JsonClass(generateAdapter = true)
data class RelationshipDto(
    val id: String,
    val type: String,
    val attributes: RelationshipAttributesDto? = null
)

@JsonClass(generateAdapter = true)
data class RelationshipAttributesDto(
    val name: String? = null,
    val fileName: String? = null
)

// Domain model mapped from external sources
data class MangaSearchResult(
    val id: String,
    val title: String,
    val altTitles: List<String> = emptyList(),
    val description: String = "",
    val type: String = "Mangá",               // "Mangá", "Manhwa", "Manhua", "HQ"
    val authors: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val coverUrl: String? = null,
    val totalChapters: Int? = null,
    val source: String = "MangaDex",
    val isAdult: Boolean = false
)
