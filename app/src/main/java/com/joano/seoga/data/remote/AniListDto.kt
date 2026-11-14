package com.joano.seoga.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AniListGraphQLRequest(
    val query: String,
    val variables: Map<String, Any?>
)

@JsonClass(generateAdapter = true)
data class AniListResponse(
    val data: AniListDataDto? = null,
    val errors: List<AniListErrorDto>? = null
)

@JsonClass(generateAdapter = true)
data class AniListErrorDto(
    val message: String? = null,
    val status: Int? = null
)

@JsonClass(generateAdapter = true)
data class AniListDataDto(
    val Page: AniListPageDto? = null
)

@JsonClass(generateAdapter = true)
data class AniListPageDto(
    val media: List<AniListMediaDto>? = null
)

@JsonClass(generateAdapter = true)
data class AniListMediaDto(
    val id: Int,
    val title: AniListTitleDto? = null,
    val synonyms: List<String>? = null,
    val description: String? = null,
    val format: String? = null,
    val countryOfOrigin: String? = null,
    val chapters: Int? = null,
    val genres: List<String>? = null,
    val isAdult: Boolean? = null,
    val coverImage: AniListCoverImageDto? = null,
    val staff: AniListStaffConnectionDto? = null
)

@JsonClass(generateAdapter = true)
data class AniListTitleDto(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListCoverImageDto(
    val large: String? = null
)

@JsonClass(generateAdapter = true)
data class AniListStaffConnectionDto(
    val edges: List<AniListStaffEdgeDto>? = null
)

@JsonClass(generateAdapter = true)
data class AniListStaffEdgeDto(
    val role: String? = null,
    val node: AniListStaffNodeDto? = null
)

@JsonClass(generateAdapter = true)
data class AniListStaffNodeDto(
    val name: AniListNameDto? = null
)

@JsonClass(generateAdapter = true)
data class AniListNameDto(
    val full: String? = null
)
