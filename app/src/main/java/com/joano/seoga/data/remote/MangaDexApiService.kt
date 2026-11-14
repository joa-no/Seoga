package com.joano.seoga.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface MangaDexApiService {

    @GET("manga")
    suspend fun searchManga(
        @Query("title") title: String,
        @Query("limit") limit: Int = 20,
        @Query("includes[]") includes: List<String> = listOf("cover_art", "author", "artist"),
        @Query("contentRating[]") contentRatings: List<String> = listOf("safe", "suggestive"),
        @Query("order[relevance]") order: String = "desc"
    ): MangaDexResponse
}
