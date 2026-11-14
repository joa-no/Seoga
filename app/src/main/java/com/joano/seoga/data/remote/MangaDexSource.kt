package com.joano.seoga.data.remote

class MangaDexSource(
    private val repository: MangaDexRepository = MangaDexRepository()
) : MetadataSource {
    override val id: String = "mangadex"
    override val displayName: String = "MangaDex"

    override suspend fun search(query: String, includeAdult: Boolean, languageTag: String): Result<List<MangaSearchResult>> {
        return repository.search(query, includeAdult, preferredLanguage = languageTag)
    }
}
