package com.appvillis.nicegram

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.appvillis.lib_android_base.Intents

object NicegramDialogs {
    private const val PREF_NAME = "nicegram_prefs"
    private const val PREF_PRIVACY_DISPLAYED = "PREF_PRIVACY_DISPLAYED"

    fun showContactsPermissionDialogIfNeeded(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val privacyWasDisplayed = prefs.getBoolean(PREF_PRIVACY_DISPLAYED, false)

        if (!privacyWasDisplayed) {
            prefs.edit().putBoolean(PREF_PRIVACY_DISPLAYED, true).apply()

            createContactsPermissionDialog(activity) {
                if (it == 0) Intents.openUrl(activity, NicegramConsts.PRIVACY_POLICY_URL)
            }.create().show()
        }
    }

    private fun createContactsPermissionDialog(
        context: Context,
        callback: (Int) -> Unit
    ): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.NicegramDialogPolicy))
        builder.setCancelable(false)
        builder.setMessage(context.getString(R.string.NicegramDialogPolicyText))
        builder.setNeutralButton(R.string.NicegramPrivacyPolicy) { _: DialogInterface?, _: Int -> callback(0) }
        builder.setPositiveButton(R.string.NicegramContinue) { _: DialogInterface?, _: Int -> callback(1) }
        return builder
    }
}