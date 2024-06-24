package com.appvillis.nicegram.network

import com.appvillis.nicegram.network.response.RestoreResponse
import retrofit2.http.*

interface NicegramApi {
    @GET("restoreAccess")
    suspend fun restoreAccess(@Query("id") telegramId: Long): RestoreResponse
}
