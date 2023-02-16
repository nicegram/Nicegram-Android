package com.appvillis.nicegram.network.response

import com.google.gson.annotations.SerializedName

class SettingsResponse(val settings: Settings, val reasons: List<String>, val premium: Boolean) {
    class Settings(@SerializedName("sync_chats") val syncChats: Boolean)
}