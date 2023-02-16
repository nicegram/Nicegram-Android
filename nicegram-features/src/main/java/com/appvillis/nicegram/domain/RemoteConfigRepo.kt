package com.appvillis.nicegram.domain

interface RemoteConfigRepo {
    val getGroupInfoThrottleSec: Long

    fun initialize()
}