package app.nicegram.bridge

import android.util.Base64
import com.appvillis.feature_telegram_session.api.ExportResult
import com.appvillis.feature_telegram_session.api.LoginResult
import com.appvillis.feature_telegram_session.api.TgLoginBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.BuildVars
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.SRPHelper
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.LaunchActivity
import org.telegram.ui.MainTabsActivity
import kotlin.coroutines.resume

/**
 * Native (Telegram-core) implementation of the QR login-token flow.
 *
 * Mirrors the MTProto sequence of [org.telegram.ui.LoginActivity]:
 *  - auth.exportLoginToken to obtain a QR token
 *  - re-export / DC migration (auth.importLoginToken) once the QR is scanned
 *  - SRP 2FA via account.getPassword -> SRPHelper -> auth.checkPassword
 *  - non-UI account activation mirrored from LoginActivity.onAuthSuccess
 */
class TgLoginBridgeImpl : TgLoginBridge {

    @Volatile
    private var pendingAccount: Int = -1

    override suspend fun beginExport(): ExportResult {
        val account = allocateFreeAccount() ?: return ExportResult.Failure("No free account slot")
        pendingAccount = account
        val req = TLRPC.TL_auth_exportLoginToken().apply {
            api_id = BuildVars.APP_ID
            api_hash = BuildVars.APP_HASH
        }
        val (response, error) = sendRequest(
            account, req,
            ConnectionsManager.RequestFlagWithoutLogin or ConnectionsManager.RequestFlagEnableUnauthorized
        )
        return when {
            response is TLRPC.TL_auth_loginToken -> ExportResult.Token(encodeToken(response.token))
            error?.text == "AUTH_TOKEN_EXCEPTION" -> {
                releasePending(); ExportResult.AlreadyLoggedIn
            }

            else -> {
                releasePending(); ExportResult.Failure(error?.text ?: "exportLoginToken failed")
            }
        }
    }

    override suspend fun completeLogin(twofaPassword: String): LoginResult {
        val account = pendingAccount
        if (account < 0) return LoginResult.Failure("No pending login")
        val req = TLRPC.TL_auth_exportLoginToken().apply {
            api_id = BuildVars.APP_ID
            api_hash = BuildVars.APP_HASH
        }
        var (response, error) = sendRequest(
            account, req,
            ConnectionsManager.RequestFlagWithoutLogin or ConnectionsManager.RequestFlagEnableUnauthorized
        )


        // The password-pending session lives on whichever DC the (last) token call
        // ran against. After a migration, getPassword/checkPassword MUST be routed
        // to the migrated DC, otherwise getPassword hits a DC with no session.
        var dcId = ConnectionsManager.DEFAULT_DATACENTER_ID
        if (response is TLRPC.TL_auth_loginTokenMigrateTo) {
            val migrate = response
            dcId = migrate.dc_id
            val importReq = TLRPC.TL_auth_importLoginToken().apply { token = migrate.token }
            val pair = sendRequest(
                account, importReq,
                ConnectionsManager.RequestFlagWithoutLogin or ConnectionsManager.RequestFlagEnableUnauthorized,
                datacenterId = migrate.dc_id
            )
            response = pair.first
            error = pair.second
        }

        return when {
            response is TLRPC.TL_auth_loginTokenSuccess -> {
                finalize(account, response.authorization, dcId)
            }
            error?.text == "SESSION_PASSWORD_NEEDED" -> {
                checkPassword(account, twofaPassword, dcId)
            }
            else -> {
                releasePending(); LoginResult.Failure(error?.text ?: "completeLogin failed")
            }
        }
    }

    override fun cancel() = releasePending()

    override fun loggedInTelegramIds(): Set<Long> {
        val ids = HashSet<Long>()
        for (a in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            val uc = UserConfig.getInstance(a)
            if (uc.isClientActivated) ids.add(uc.clientUserId)
        }
        return ids
    }

    private suspend fun checkPassword(account: Int, password: String, dcId: Int): LoginResult {
        if (password.isEmpty()) {
            releasePending(); return LoginResult.WrongPassword
        }
        val pwResp = sendRequest(
            account, TL_account.getPassword(),
            // EnableUnauthorized is required: the session on this DC is only
            // password-pending, so without it ConnectionsManager queues the
            // request waiting for a login that never comes -> infinite hang.
            ConnectionsManager.RequestFlagWithoutLogin or ConnectionsManager.RequestFlagEnableUnauthorized,
            datacenterId = dcId
        )
        val pw = pwResp.first as? TL_account.Password ?: run {
            releasePending()
            return LoginResult.Failure("getPassword failed: ${pwResp.second?.text ?: "no password object"}")
        }
        val algo =
            pw.current_algo as? TLRPC.TL_passwordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow
                ?: run {
                    releasePending()
                    return LoginResult.Failure("Unsupported password algo")
                }
        // PBKDF2 (100k iterations) is CPU-heavy and this runs on the caller's
        // dispatcher (viewModelScope == Main), so push it to Default to avoid
        // blocking the UI thread.
        val check = withContext(Dispatchers.Default) {
            val xBytes = SRPHelper.getX(AndroidUtilities.getStringBytes(password), algo)
            SRPHelper.startCheck(xBytes, pw.srp_id, pw.srp_B, algo)
        } ?: run {
            releasePending()
            return LoginResult.WrongPassword
        }
        val req = TLRPC.TL_auth_checkPassword().apply { this.password = check }
        val (resp, err) = sendRequest(
            account, req,
            ConnectionsManager.RequestFlagFailOnServerErrors or
                ConnectionsManager.RequestFlagWithoutLogin or
                ConnectionsManager.RequestFlagEnableUnauthorized,
            datacenterId = dcId
        )
        return when {
            resp is TLRPC.TL_auth_authorization -> {
                finalize(account, resp, dcId)
            }
            err?.text == "PASSWORD_HASH_INVALID" -> {
                releasePending(); LoginResult.WrongPassword
            }

            else -> {
                releasePending(); LoginResult.Failure(err?.text ?: "checkPassword failed")
            }
        }
    }

    /**
     * Mirrors the NON-UI parts of [org.telegram.ui.LoginActivity.onAuthSuccess]:
     * order matters (setUserId first, then clearConfig + cleanups, then persist
     * the user, then storage + putUser, then post-login refreshes). UI,
     * navigation, theming, analytics and sticker preloading are intentionally
     * omitted.
     *
     * Runs on the main thread because [org.telegram.ui.LoginActivity.onAuthSuccess]
     * (which this mirrors) is a UI-thread callback and the MessagesController /
     * UserConfig / NotificationCenter writes below assume the main thread.
     */
    private suspend fun finalize(account: Int, auth: TLRPC.auth_Authorization, dcId: Int): LoginResult =
        withContext(Dispatchers.Main) {
        val authorization = auth as TLRPC.TL_auth_authorization
        val messagesController = MessagesController.getInstance(account)
        val connectionsManager = ConnectionsManager.getInstance(account)
        val userConfig = UserConfig.getInstance(account)
        val messagesStorage = MessagesStorage.getInstance(account)

        // Pin this slot's home DC to the one the login authorized on. After a
        // loginTokenMigrateTo the authorized session lives on the migrated DC;
        // without this the new slot defaults to the wrong DC, has no valid auth
        // key there, and gets logged out on switch (account never persists).
        if (dcId != ConnectionsManager.DEFAULT_DATACENTER_ID) {
            connectionsManager.setDefaultDatacenterId(dcId)
        }

        messagesController.cleanup()
        connectionsManager.setUserId(authorization.user.id)
        userConfig.clearConfig()
        messagesController.cleanup()
        userConfig.syncContacts = true
        userConfig.setCurrentUser(authorization.user)
        userConfig.saveConfig(true)
        messagesStorage.cleanup(true)
        val users = ArrayList<TLRPC.User>()
        users.add(authorization.user)
        messagesStorage.putUsersAndChats(users, null, true, true)
        messagesController.putUser(authorization.user, false)
        AccountInstance.getInstance(account).contactsController.checkAppAccount()
        messagesController.checkPromoInfo(true)
        connectionsManager.updateDcSettings()

        pendingAccount = -1
        val launch = LaunchActivity.instance
        if (launch != null) {
            if (account != UserConfig.selectedAccount) {
                // Additional account: the new slot differs from the selected one, so
                // switchToAccount performs the account switch and rebuilds the UI.
                // (It sets selectedAccount itself; do NOT set it manually.)
                launch.switchToAccount(account, true)
            } else {
                // First login: the activated slot is ALREADY selectedAccount, so
                // switchToAccount() early-returns (it guards account == selectedAccount)
                // and the login screen never rebuilds — the user is stuck on
                // LoginActivity until an app restart. Mirror LoginActivity
                // .needFinishActivity: replace the login fragment with the chat list.
                val actionBarLayout = launch.actionBarLayout
                actionBarLayout.removeAllFragments()
                actionBarLayout.addFragmentToStack(
                    MainTabsActivity(),
                    INavigationLayout.FORCE_ATTACH_VIEW_AS_FIRST
                )
                actionBarLayout.rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST)
            }
        } else {
            // App not in foreground: leave the slot activated; usable later. No live switch.
            UserConfig.getInstance(account).saveConfig(true)
        }
        LoginResult.Success
    }

    private fun allocateFreeAccount(): Int? {
        for (a in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            if (!UserConfig.getInstance(a).isClientActivated) return a
        }
        return null
    }

    private fun releasePending() {
        val account = pendingAccount
        if (account >= 0) {
            UserConfig.getInstance(account).clearConfig(); pendingAccount = -1
        }
    }

    // Standard Base64 (alphabet +/ , with '=' padding) to match the server/iOS,
    // NOT URL-safe. NO_WRAP keeps it on a single line without trailing newline.
    private fun encodeToken(token: ByteArray): String =
        Base64.encodeToString(token, Base64.NO_WRAP)

    private suspend fun sendRequest(
        account: Int,
        req: TLObject,
        flags: Int,
        datacenterId: Int = ConnectionsManager.DEFAULT_DATACENTER_ID,
    ): Pair<TLObject?, TLRPC.TL_error?> = suspendCancellableCoroutine { cont ->
        val token = ConnectionsManager.getInstance(account).sendRequest(
            req,
            { response, error -> if (cont.isActive) cont.resume(response to error) },
            null, null, flags, datacenterId, ConnectionsManager.ConnectionTypeGeneric, true
        )
        cont.invokeOnCancellation { ConnectionsManager.getInstance(account).cancelRequest(token, true) }
    }
}
