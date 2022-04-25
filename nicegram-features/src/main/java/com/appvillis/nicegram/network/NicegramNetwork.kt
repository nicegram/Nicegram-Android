package com.appvillis.nicegram.network

import android.content.Context
import com.appvillis.nicegram.BuildConfig
import com.appvillis.nicegram.NicegramConstsAndKeys.API_KEY
import com.appvillis.nicegram.NicegramConstsAndKeys.BASE_URL
import com.appvillis.nicegram.R
import com.appvillis.nicegram.network.request.RegDateRequest
import com.appvillis.nicegram.network.response.RegDateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NicegramNetwork {
    private const val TIMEOUT = 30L

    private val nicegramApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(NicegramApi::class.java)
    }

    private val okHttpClient by lazy {
         OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().setLevel(
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
                )
            )
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    fun getRegDate(context: Context, userId: Long, callback: (regDate: String?) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = nicegramApi.getRegDate(RegDateRequest((userId)), API_KEY)

                GlobalScope.launch(Dispatchers.Main) {
                    val prefix = when (result.data.type) {
                        RegDateResponse.RegDateType.Approximately -> {
                            context.getString(R.string.NicegramApproximately)
                        }
                        RegDateResponse.RegDateType.OlderThan -> {
                            context.getString(R.string.NicegramOlderThan)
                        }
                        RegDateResponse.RegDateType.NewerThan -> {
                            context.getString(R.string.NicegramNewerThan)
                        }
                        else -> ""
                    }
                    callback("$prefix ${result.data.date}")
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) e.printStackTrace()

                GlobalScope.launch(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }
}