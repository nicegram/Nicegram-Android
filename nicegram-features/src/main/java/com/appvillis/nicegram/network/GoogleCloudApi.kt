package com.appvillis.nicegram.network

import com.appvillis.nicegram.NicegramNetworkConsts
import com.appvillis.nicegram.network.request.GoogleSpeechRecognizeRequest
import com.appvillis.nicegram.network.response.GoogleSpeech2TextResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface GoogleCloudApi {
    @POST("v1p1beta1/speech:recognize?key=${NicegramNetworkConsts.GOOGLE_CLOUD_KEY}")
    suspend fun recognizeSpeech(@Body request: GoogleSpeechRecognizeRequest): GoogleSpeech2TextResponse
}