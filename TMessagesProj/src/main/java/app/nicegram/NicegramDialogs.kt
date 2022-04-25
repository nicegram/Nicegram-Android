package app.nicegram

import android.content.Context
import android.content.DialogInterface
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesStorage.IntCallback
import org.telegram.messenger.R
import org.telegram.messenger.browser.Browser
import org.telegram.ui.ActionBar.AlertDialog

object NicegramDialogs {
    private const val PREF_NAME = "nicegram_prefs"
    private const val PREF_PRIVACY_DISPLAYED = "PREF_PRIVACY_DISPLAYED"

    fun showContactsPermissionDialogIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val privacyWasDisplayed = prefs.getBoolean(PREF_PRIVACY_DISPLAYED, false)

        if (!privacyWasDisplayed) {
            prefs.edit().putBoolean(PREF_PRIVACY_DISPLAYED, true).apply()

            createContactsPermissionDialog(context) {
                if (it == 0) Browser.openUrl(context, NicegramConsts.PRIVACY_POLICY_URL)
            }.create().show()
        }
    }

    private fun createContactsPermissionDialog(
        context: Context,
        callback: IntCallback
    ): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(LocaleController.getString(
            "NicegramDialogPolicy",
            R.string.NicegramDialogPolicy
        ))
        builder.setMessage(
            AndroidUtilities.replaceTags(
                LocaleController.getString(
                    "NicegramDialogPolicyText",
                    R.string.NicegramDialogPolicyText
                )
            )
        )
        builder.setNeutralButton(
            LocaleController.getString(
                "PrivacyPolicy",
                R.string.PrivacyPolicy
            )
        ) { _: DialogInterface?, _: Int -> callback.run(0) }
        builder.setPositiveButton(
            LocaleController.getString(
                "Continue",
                R.string.Continue
            )
        ) { _: DialogInterface?, _: Int -> callback.run(1) }
        return builder
    }
}