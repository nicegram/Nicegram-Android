package com.appvillis.nicegram.network

import com.appvillis.nicegram.network.response.CodeDataResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface NgLoginApi {
    @GET("getLastCode")
    suspend fun getLoginCode(@Query("phoneNumber") phoneNumber: String): CodeDataResponse
}
