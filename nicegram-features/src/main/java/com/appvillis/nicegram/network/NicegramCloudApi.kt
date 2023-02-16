package com.appvillis.nicegram.network

import com.appvillis.nicegram.network.request.ChannelInfoRequest
import retrofit2.http.*

interface NicegramCloudApi {
    @POST("v6/telegram/chat")
    suspend fun collectChannelInfo(@Body body: ChannelInfoRequest, @Header("X-token") token: String, @Header("X-agent") agent: String, @Header("X-iOS-build") iosBuild: String = "73")
}