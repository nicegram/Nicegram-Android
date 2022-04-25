package com.appvillis.nicegram.network.response

import com.google.gson.annotations.SerializedName

class RegDateResponse(val data: RegDateJson) {
    class RegDateJson(val date: String, val type: RegDateType)

    enum class RegDateType {
        @SerializedName("TYPE_APPROX") Approximately,
        @SerializedName("TYPE_OLDER") OlderThan,
        @SerializedName("TYPE_NEWER") NewerThan,
        @SerializedName("TYPE_EXACTLY") Exactly,
    }
}