package com.appvillis.nicegram.network

import com.appvillis.nicegram.network.request.RegDateRequest
import com.appvillis.nicegram.network.response.RegDateResponse
import com.appvillis.nicegram.network.response.SettingsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NicegramAppApi {
    @POST("v7/regdate")
    suspend fun getRegDate(@Body body: RegDateRequest): RegDateResponse

    @GET("v7/unblock-feature/settings/{telegramId}")
    suspend fun getSettings(@Path("telegramId") userId: Long): SettingsResponse

}
