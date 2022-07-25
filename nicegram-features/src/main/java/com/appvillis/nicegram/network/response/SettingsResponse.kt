package com.appvillis.nicegram.network.response

import com.google.gson.annotations.SerializedName

class SettingsResponse(val settings: Settings) {
    class Settings(@SerializedName("sync_chats") val syncChats: Boolean)
}