package com.appvillis.nicegram.data

import android.content.SharedPreferences
import com.appvillis.nicegram.BuildConfig
import com.appvillis.nicegram.domain.GroupCollectRepo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class GroupCollectRepoImpl(private val prefs: SharedPreferences) : GroupCollectRepo {
    companion object {
        const val PREF_NAME_GROUPS_COLLECTED = "PREF_NAME_GROUPS_COLLECTED"
    }
    private val gson = Gson()
    private var infoMap = mutableMapOf<Long, Long>()

    init {
        try {
            infoMap = gson.fromJson(prefs.getString(PREF_NAME_GROUPS_COLLECTED, ""), object : TypeToken<MutableMap<Long, Long>>() {}.type)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) e.printStackTrace()
        }
    }

    override fun getLastTimeMsGroupCollected(id: Long): Long {
        return infoMap[id] ?: 0L
    }

    override fun setLastTimeMsGroupCollected(id: Long, time: Long) {
        infoMap[id] = time

        prefs.edit().putString(PREF_NAME_GROUPS_COLLECTED, gson.toJson(infoMap)).apply()
    }
}