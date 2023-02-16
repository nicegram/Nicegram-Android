package com.appvillis.nicegram.data

import android.util.Log
import com.appvillis.nicegram.domain.RemoteConfigRepo
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject

class RemoteConfigRepoImpl : RemoteConfigRepo {
    override fun initialize() {
        loadRemoteConfig()
    }

    private fun loadRemoteConfig() {
        val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)

        fetchConfig()
    }

    private fun fetchConfig() {
        FirebaseRemoteConfig.getInstance().fetchAndActivate()
            .addOnCompleteListener { task ->
                try {
                    if (task.isSuccessful) {
                        val jsonObj = JSONObject(FirebaseRemoteConfig.getInstance().getString("shareChannelsConfig"))
                        _getGroupInfoThrottleSec = jsonObj.getLong("throttlingInterval")
                    } else {
                        task.exception?.printStackTrace()
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) e.printStackTrace()
                }

            }
    }

    private var _getGroupInfoThrottleSec = 86401L
    override val getGroupInfoThrottleSec get() = _getGroupInfoThrottleSec
}