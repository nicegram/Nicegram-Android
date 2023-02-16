package com.appvillis.nicegram.network

import com.appvillis.nicegram.network.request.RegDateRequest
import com.appvillis.nicegram.network.response.RegDateResponse
import com.appvillis.nicegram.network.response.RestoreResponse
import retrofit2.http.*

interface NicegramApi {
    @POST("regdate")
    suspend fun getRegDate(@Body body: RegDateRequest, @Header("x-api-key") apiKey: String): RegDateResponse

    @GET("restoreAccess")
    suspend fun restoreAccess(@Query("id") telegramId: Long): RestoreResponse
}