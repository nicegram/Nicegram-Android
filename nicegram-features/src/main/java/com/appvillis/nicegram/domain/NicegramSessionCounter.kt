package com.appvillis.nicegram.domain

interface NicegramSessionCounter {
    val sessionsCount: Int
    fun increaseSessionCount()
}