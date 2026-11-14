package com.joano.seoga.data.remote

interface MetadataSource {
    val id: String
    val displayName: String
    suspend fun search(query: String, includeAdult: Boolean, languageTag: String = "en"): Result<List<MangaSearchResult>>
}
