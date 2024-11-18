package com.appvillis.nicegram.network.response

import com.google.gson.annotations.SerializedName
import java.util.Date

class CodeDataResponse(
    @SerializedName("code") val code: String,
    @SerializedName("date") val date: Date,
)
