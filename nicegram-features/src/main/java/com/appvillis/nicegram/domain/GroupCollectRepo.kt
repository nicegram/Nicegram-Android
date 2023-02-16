package com.appvillis.nicegram.domain

interface GroupCollectRepo {
    fun getLastTimeMsGroupCollected(id: Long): Long
    fun setLastTimeMsGroupCollected(id: Long, time: Long)
}