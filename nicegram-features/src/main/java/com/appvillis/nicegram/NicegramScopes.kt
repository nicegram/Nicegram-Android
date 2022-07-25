package com.appvillis.nicegram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object NicegramScopes {
    val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}