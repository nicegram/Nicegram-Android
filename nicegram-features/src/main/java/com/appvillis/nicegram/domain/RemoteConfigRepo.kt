package com.appvillis.nicegram.domain

interface RemoteConfigRepo {
    val getGroupInfoThrottleSec: Long
    val allowCopyProtectedContent: Boolean

    fun initialize()
}