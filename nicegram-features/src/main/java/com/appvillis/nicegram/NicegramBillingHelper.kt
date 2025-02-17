package com.appvillis.nicegram

import android.content.Context
import com.appvillis.core_ui.BuildConfig
import dagger.hilt.EntryPoints

object NicegramBillingHelper {
    private fun entryPoint(context: Context) =
        EntryPoints.get(context.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun getUserHasNgPremiumSub(context: Context): Boolean {
        val ep = entryPoint(context)
        val billingManager = ep.billingManager()
        val userRepository = ep.userRepository()
        return if (BuildConfig.IS_LITE_CLIENT) false
        else billingManager.hasAnyPremium || (userRepository.userData.value?.hasAnySub ?: false)
    }
}
