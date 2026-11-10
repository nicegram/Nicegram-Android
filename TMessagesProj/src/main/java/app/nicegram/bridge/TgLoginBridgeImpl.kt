package app.nicegram.bridge

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Base64
import com.appvillis.feature_telegram_session.api.ExportResult
import com.appvillis.feature_telegram_session.api.LoginHandle
import com.appvillis.feature_telegram_session.api.LoginResult
import com.appvillis.feature_telegram_session.api.TgLoginBridge
import app.nicegram.NicegramReviewerLogin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.BuildVars
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.SRPHelper
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.LaunchActivity
import org.telegram.ui.MainTabsActivity
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class TgLoginBridgeImpl : TgLoginBridge {

    private val acceptanceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val slotLock = Any()
    private val reservedSlots = HashSet<Int>()

    private var deferredChatsSlot: Int? = null
    private var launchResumeCallbacks: Application.ActivityLifecycleCallbacks? = null

    override suspend fun beginExport(): ExportResult {
        val slot = reserveFreeSlot() ?: return ExportResult.NoFreeSlot
        val handle = SlotLoginHandle(slot)
        var result: ExportResult? = null
        try {
            val (response, error) = exportToken(slot)
            result = when {
                response is TLRPC.TL_auth_loginToken -> ExportResult.Token(encodeToken(response.token), handle)
                error?.text == AUTH_TOKEN_EXCEPTION -> ExportResult.AlreadyLoggedIn
                error != null -> ExportResult.Failure(error.text ?: EXPORT_FAILED, error.text)
                else -> ExportResult.Failure(EXPORT_FAILED)
            }
            return result
        } finally {
            if (result !is ExportResult.Token) handle.cancel()
        }
    }

    override fun loggedInTelegramIds(): Set<Long> {
        val ids = HashSet<Long>()
        for (slot in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            val userConfig = UserConfig.getInstance(slot)
            if (userConfig.isClientActivated) ids.add(userConfig.clientUserId)
        }
        return ids
    }

    override fun switchToAccount(telegramId: Long) {
        AndroidUtilities.runOnUIThread {
            val slot = slotOf(telegramId) ?: return@runOnUIThread
            val launch = liveLaunchActivity()
            if (launch == null) deferChatsUntilLaunchResumes(slot)
            bringTelegramToFront()
            if (launch != null) showChats(launch, slot)
        }
    }

    override fun showTelegramAfterLogin() {
        AndroidUtilities.runOnUIThread { bringTelegramToFront() }
    }

    private inner class SlotLoginHandle(private val slot: Int) : LoginHandle {

        private val closed = AtomicBoolean(false)
        private val acceptance = CompletableDeferred<Unit>()
        private val acceptanceJob = acceptanceScope.launch(start = CoroutineStart.UNDISPATCHED) {
            NicegramReviewerLogin.loginTokenAcceptedAccounts.first { it == slot }
            acceptance.complete(Unit)
        }

        private val cancelledResult = LoginResult.Failure(LOGIN_CANCELLED)

        override suspend fun completeLogin(twofaPassword: String?, approvedAtMillis: Long): LoginResult {
            if (closed.get()) return cancelledResult
            try {
                return login(twofaPassword, approvedAtMillis)
            } finally {
                cancel()
            }
        }

        override fun cancel() {
            if (closed.compareAndSet(false, true)) {
                acceptanceJob.cancel()
                releaseSlot(slot)
            }
        }

        private suspend fun login(twofaPassword: String?, approvedAtMillis: Long): LoginResult {
            awaitAcceptance(approvedAtMillis)
            if (closed.get()) return cancelledResult
            var (response, error) = exportToken(slot)
            if (closed.get()) return cancelledResult
            if (response is TLRPC.TL_auth_loginToken) return LoginResult.Failure(NO_ACCEPTANCE)
            var dcId = ConnectionsManager.DEFAULT_DATACENTER_ID
            if (response is TLRPC.TL_auth_loginTokenMigrateTo) {
                val migrate: TLRPC.TL_auth_loginTokenMigrateTo = response
                dcId = migrate.dc_id
                val importReq = TLRPC.TL_auth_importLoginToken().apply { token = migrate.token }
                val imported = sendRequest(slot, importReq, UNAUTHORIZED_FLAGS, datacenterId = dcId)
                response = imported.first
                error = imported.second
                if (closed.get()) return cancelledResult
            }
            return when {
                response is TLRPC.TL_auth_loginTokenSuccess -> {
                    val authorization = response.authorization as? TLRPC.TL_auth_authorization
                        ?: return LoginResult.Failure(SIGN_UP_REQUIRED)
                    finalize(authorization, dcId)
                }

                error?.text == SESSION_PASSWORD_NEEDED -> checkPassword(twofaPassword, dcId)
                error != null -> LoginResult.Failure(error.text ?: LOGIN_FAILED, error.text)
                else -> LoginResult.Failure(LOGIN_FAILED)
            }
        }

        private suspend fun awaitAcceptance(approvedAtMillis: Long) {
            if (acceptance.isCompleted) return
            val remaining = approvedAtMillis + ACCEPTANCE_WINDOW_MS - SystemClock.elapsedRealtime()
            if (remaining <= 0) return
            withTimeoutOrNull(remaining) { acceptance.await() }
        }

        private suspend fun checkPassword(password: String?, dcId: Int): LoginResult {
            if (password.isNullOrEmpty()) return LoginResult.WrongPassword
            val (pwResponse, pwError) = sendRequest(slot, TL_account.getPassword(), UNAUTHORIZED_FLAGS, datacenterId = dcId)
            val pw = pwResponse as? TL_account.Password
                ?: return if (closed.get()) cancelledResult else LoginResult.Failure(pwError?.text ?: GET_PASSWORD_FAILED, pwError?.text)
            val algo =
                pw.current_algo as? TLRPC.TL_passwordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow
                    ?: return LoginResult.Failure(UNSUPPORTED_PASSWORD_ALGO)
            val check = withContext(Dispatchers.Default) {
                val xBytes = SRPHelper.getX(AndroidUtilities.getStringBytes(password), algo)
                SRPHelper.startCheck(xBytes, pw.srp_id, pw.srp_B, algo)
            } ?: return if (closed.get()) cancelledResult else LoginResult.WrongPassword
            if (closed.get()) return cancelledResult
            val req = TLRPC.TL_auth_checkPassword().apply { this.password = check }
            val (response, error) = sendRequest(
                slot,
                req,
                ConnectionsManager.RequestFlagFailOnServerErrors or UNAUTHORIZED_FLAGS,
                datacenterId = dcId
            )
            if (closed.get()) return cancelledResult
            return when {
                response is TLRPC.TL_auth_authorization -> finalize(response, dcId)
                error?.text == PASSWORD_HASH_INVALID -> LoginResult.WrongPassword
                error != null -> LoginResult.Failure(error.text ?: CHECK_PASSWORD_FAILED, error.text)
                else -> LoginResult.Failure(CHECK_PASSWORD_FAILED)
            }
        }

        private suspend fun finalize(authorization: TLRPC.TL_auth_authorization, dcId: Int): LoginResult =
            withContext(Dispatchers.Main) {
                currentCoroutineContext().ensureActive()
                if (!closed.compareAndSet(false, true)) return@withContext cancelledResult
                acceptanceJob.cancel()

                val messagesController = MessagesController.getInstance(slot)
                val connectionsManager = ConnectionsManager.getInstance(slot)
                val userConfig = UserConfig.getInstance(slot)
                val messagesStorage = MessagesStorage.getInstance(slot)

                if (dcId != ConnectionsManager.DEFAULT_DATACENTER_ID) {
                    connectionsManager.setDefaultDatacenterId(dcId)
                }

                messagesController.cleanup()
                connectionsManager.setUserId(authorization.user.id)
                userConfig.clearConfig()
                messagesController.cleanup()
                userConfig.syncContacts = false
                userConfig.setCurrentUser(authorization.user)
                userConfig.saveConfig(true)
                messagesStorage.cleanup(true)
                val users = ArrayList<TLRPC.User>()
                users.add(authorization.user)
                messagesStorage.putUsersAndChats(users, null, true, true)
                messagesController.putUser(authorization.user, false)
                AccountInstance.getInstance(slot).contactsController.checkAppAccount()
                messagesController.checkPromoInfo(true)
                connectionsManager.updateDcSettings()

                unreserveSlot(slot)

                if (slot == 0) {
                    NotificationCenter.getInstance(0).postNotificationName(NotificationCenter.mainUserInfoChanged)
                }

                val launch = liveLaunchActivity()
                if (launch != null) showChats(launch, slot) else deferChatsUntilLaunchResumes(slot)
                LoginResult.Success
            }
    }

    private fun reserveFreeSlot(): Int? = synchronized(slotLock) {
        for (slot in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            if (!UserConfig.getInstance(slot).isClientActivated && slot !in reservedSlots) {
                reservedSlots.add(slot)
                return slot
            }
        }
        null
    }

    private fun releaseSlot(slot: Int) = synchronized(slotLock) {
        val userConfig = UserConfig.getInstance(slot)
        if (!userConfig.isClientActivated) userConfig.clearConfig()
        reservedSlots.remove(slot)
    }

    private fun unreserveSlot(slot: Int) = synchronized(slotLock) {
        reservedSlots.remove(slot)
    }

    private fun slotOf(telegramId: Long): Int? {
        for (slot in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            val userConfig = UserConfig.getInstance(slot)
            if (userConfig.isClientActivated && userConfig.clientUserId == telegramId) return slot
        }
        return null
    }

    private fun productionTelegramIds(): ArrayList<Long> {
        val ids = ArrayList<Long>()
        for (slot in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            val userConfig = UserConfig.getInstance(slot)
            if (userConfig.isClientActivated && !ConnectionsManager.getInstance(slot).isTestBackend) {
                ids.add(userConfig.clientUserId)
            }
        }
        return ids
    }

    private suspend fun exportToken(slot: Int): Pair<TLObject?, TLRPC.TL_error?> {
        val req = TLRPC.TL_auth_exportLoginToken().apply {
            api_id = BuildVars.APP_ID
            api_hash = BuildVars.APP_HASH
            except_ids = productionTelegramIds()
        }
        return sendRequest(slot, req, UNAUTHORIZED_FLAGS)
    }

    private fun liveLaunchActivity(): LaunchActivity? =
        LaunchActivity.instance?.takeIf { !it.isFinishing && !it.isDestroyed }

    private fun bringTelegramToFront() {
        val launch = liveLaunchActivity()
        val context: Context = launch ?: ApplicationLoader.applicationContext
        val intent = Intent(context, LaunchActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun showChats(launch: LaunchActivity, slot: Int) {
        if (!UserConfig.getInstance(slot).isClientActivated) return
        if (slot != UserConfig.selectedAccount) {
            launch.switchToAccount(slot, true)
            return
        }
        val actionBarLayout = launch.actionBarLayout ?: return
        val stack = actionBarLayout.fragmentStack
        if (stack.size == 1 && stack[0] is MainTabsActivity) return
        actionBarLayout.removeAllFragments()
        actionBarLayout.addFragmentToStack(MainTabsActivity(), INavigationLayout.FORCE_ATTACH_VIEW_AS_FIRST)
        actionBarLayout.rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST)
    }

    private fun deferChatsUntilLaunchResumes(slot: Int) {
        deferredChatsSlot = slot
        if (launchResumeCallbacks != null) return
        val application = ApplicationLoader.applicationContext as? Application ?: return
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity !is LaunchActivity) return
                application.unregisterActivityLifecycleCallbacks(this)
                launchResumeCallbacks = null
                val pendingSlot = deferredChatsSlot ?: return
                deferredChatsSlot = null
                showChats(activity, pendingSlot)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        launchResumeCallbacks = callbacks
        application.registerActivityLifecycleCallbacks(callbacks)
    }

    private fun encodeToken(token: ByteArray): String =
        Base64.encodeToString(token, Base64.NO_WRAP)

    private suspend fun sendRequest(
        slot: Int,
        req: TLObject,
        flags: Int,
        datacenterId: Int = ConnectionsManager.DEFAULT_DATACENTER_ID,
    ): Pair<TLObject?, TLRPC.TL_error?> = suspendCancellableCoroutine { cont ->
        val token = ConnectionsManager.getInstance(slot).sendRequest(
            req,
            { response, error -> if (cont.isActive) cont.resume(response to error) },
            null, null, flags, datacenterId, ConnectionsManager.ConnectionTypeGeneric, true
        )
        cont.invokeOnCancellation { ConnectionsManager.getInstance(slot).cancelRequest(token, true) }
    }

    private companion object {
        const val UNAUTHORIZED_FLAGS =
            ConnectionsManager.RequestFlagWithoutLogin or ConnectionsManager.RequestFlagEnableUnauthorized
        const val ACCEPTANCE_WINDOW_MS = 20_000L
        const val AUTH_TOKEN_EXCEPTION = "AUTH_TOKEN_EXCEPTION"
        const val SESSION_PASSWORD_NEEDED = "SESSION_PASSWORD_NEEDED"
        const val PASSWORD_HASH_INVALID = "PASSWORD_HASH_INVALID"
        const val EXPORT_FAILED = "exportLoginToken failed"
        const val LOGIN_FAILED = "completeLogin failed"
        const val GET_PASSWORD_FAILED = "getPassword failed"
        const val CHECK_PASSWORD_FAILED = "checkPassword failed"
        const val UNSUPPORTED_PASSWORD_ALGO = "Unsupported password algo"
        const val SIGN_UP_REQUIRED = "Sign up required"
        const val NO_ACCEPTANCE = "No acceptance"
        const val LOGIN_CANCELLED = "Login cancelled"
    }
}
