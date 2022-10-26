package app.nicegram

import android.app.NotificationManager
import android.content.Context
import android.util.Base64
import org.telegram.messenger.FileLog
import org.telegram.messenger.Utilities


object NicegramDoubleBottom {
    private const val PREFS_NG_DBOT = "userconfig_ng_dbot"
    private const val PREFS_NG_DBOT_SALT = "PREFS_NG_DBOT_SALT"
    private const val PREFS_NG_DBOT_HASH = "PREFS_NG_DBOT_HASH"
    private const val PREFS_NG_DBOT_ACC_ID = "PREFS_NG_DBOT_ACC_ID"
    private const val PREF_NG_LOGGED_IN_DBOT = "PREF_NG_LOGGED_IN_DBOT"

    private var passcodeSalt = byteArrayOf()
    private var passcodeHash = ""

    private var dbotAccountId = -1L

    var loggedToDbot = false
        private set

    var needToReloadDrawer = false

    fun init(context: Context) {
        loadPrefs(context)
    }

    private fun loadPrefs(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS_NG_DBOT,
            Context.MODE_PRIVATE
        )

        val passcodeSaltString = prefs.getString(PREFS_NG_DBOT_SALT, "")
        passcodeSalt = if ((passcodeSaltString?.length ?: 0) > 0) {
            Base64.decode(passcodeSaltString, Base64.DEFAULT)
        } else {
            ByteArray(0)
        }
        passcodeHash = prefs.getString(PREFS_NG_DBOT_HASH, "") ?: ""
        dbotAccountId = prefs.getLong(PREFS_NG_DBOT_ACC_ID, -1)
        loggedToDbot = prefs.getBoolean(PREF_NG_LOGGED_IN_DBOT, false)
    }

    fun hasDbot() = passcodeHash.isNotEmpty()

    fun disableDbot(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS_NG_DBOT,
            Context.MODE_PRIVATE
        )

        passcodeSalt = byteArrayOf()
        passcodeHash = ""
        dbotAccountId = -1
        loggedToDbot = false
        needToReloadDrawer = true

        prefs.edit().clear().apply()
    }

    fun clearNotifications(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
    }

    fun setLoggedToDbot(context: Context, logged: Boolean) {
        if (logged) { // clear all notifications
            clearNotifications(context)
        }

        needToReloadDrawer = true

        loggedToDbot = logged

        val prefs = context.getSharedPreferences(
            PREFS_NG_DBOT,
            Context.MODE_PRIVATE
        )

        prefs.edit().putBoolean(PREF_NG_LOGGED_IN_DBOT, logged).apply()
    }

    fun needToHideAccount(id: Long): Boolean {
        if (loggedToDbot && id != dbotAccountId) return true
        return false
    }

    fun isDbotAccountId(id: Long) = id == dbotAccountId

    fun isDbotPasscode(passCode: String): Boolean {
        try {
            val passcodeBytes = passCode.toByteArray(charset("UTF-8"))
            val bytes = ByteArray(32 + passcodeBytes.size)
            System.arraycopy(passcodeSalt, 0, bytes, 0, 16)
            System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.size)
            System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.size + 16, 16)
            val hash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.size.toLong()))
            return passcodeHash == hash
        } catch (e: Exception) {
            FileLog.e(e)
        }

        return false
    }

    fun setPassCode(context: Context, hash: String, salt: ByteArray, dbotAccountId: Long) {
        NicegramDoubleBottom.dbotAccountId = dbotAccountId

        val prefs = context.getSharedPreferences(
            PREFS_NG_DBOT,
            Context.MODE_PRIVATE
        )
        prefs.edit().apply {
            putString(PREFS_NG_DBOT_HASH, hash)
            putString(PREFS_NG_DBOT_SALT, if (salt.isNotEmpty()) Base64.encodeToString(salt, Base64.DEFAULT) else "")
            putLong(PREFS_NG_DBOT_ACC_ID, dbotAccountId)
            apply()
        }

        passcodeHash = hash
        passcodeSalt = salt
    }
}