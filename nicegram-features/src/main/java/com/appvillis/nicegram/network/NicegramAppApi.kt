package com.appvillis.nicegram.network

import com.appvillis.nicegram.network.response.SettingsResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface NicegramAppApi {
    @GET("v4/settings/{userId}")
    suspend fun getSettings(@Path("userId") userId: Long): SettingsResponse
}