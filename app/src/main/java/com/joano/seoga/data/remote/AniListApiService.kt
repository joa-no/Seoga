package com.joano.seoga.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AniListApiService {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("/")
    suspend fun queryManga(
        @Body request: AniListGraphQLRequest
    ): Response<AniListResponse>
}
