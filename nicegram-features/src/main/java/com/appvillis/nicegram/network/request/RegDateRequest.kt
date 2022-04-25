package com.appvillis.nicegram.network.request

import com.google.gson.annotations.SerializedName

class RegDateRequest(@SerializedName("telegramId") val userId: Long)