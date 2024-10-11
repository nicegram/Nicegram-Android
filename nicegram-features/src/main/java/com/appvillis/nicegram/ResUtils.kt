package com.appvillis.nicegram

import android.content.Context

object ResUtils {
    fun openRawRes(context: Context, id: Int) = context.resources.openRawResource(id).bufferedReader()
        .use { it.readText() }
}