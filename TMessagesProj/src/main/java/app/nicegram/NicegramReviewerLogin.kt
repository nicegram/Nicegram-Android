package app.nicegram

import android.util.Base64
import com.appvillis.core_domain.entry.appreview.AppReviewLoginTokenResultEntry
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.BuildVars
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.tgnet.tl.TL_account
import org.telegram.tgnet.tl.TL_update
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

object NicegramReviewerLogin {

    interface Callback {
        fun onPasswordNeeded(password: TL_account.Password)
        fun onAuthorized(authorization: TLRPC.TL_auth_authorization)
        fun onFailed()
    }

    fun interface Cancellable {
        fun cancel()
    }

    private sealed interface Outcome {
        data class PasswordNeeded(val password: TL_account.Password) : Outcome
        data class Authorized(val authorization: TLRPC.TL_auth_authorization) : Outcome
        data object Failed : Outcome
    }

    private const val FLAGS =
        ConnectionsManager.RequestFlagWithoutLogin or ConnectionsManager.RequestFlagEnableUnauthorized
    private const val SESSION_PASSWORD_NEEDED = "SESSION_PASSWORD_NEEDED"
    private val ACCEPTANCE_TIMEOUT = 20.seconds

    private val loginTokenAccepted = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    private fun entryPoint() =
        EntryPoints.get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    @JvmStatic
    fun onNumberConfirmed(phone: String): Boolean {
        val entryPoint = entryPoint()
        val isReviewer = entryPoint.isAppReviewPhoneUseCase().invoke(phone)
        entryPoint.setAppReviewerAttemptUseCase().invoke(isReviewer)
        return isReviewer
    }

    @JvmStatic
    fun clearAttempt() {
        entryPoint().setAppReviewerAttemptUseCase().invoke(false)
    }

    @JvmStatic
    fun start(account: Int, callback: Callback): Cancellable {
        val finished = AtomicBoolean(false)
        val entryPoint = entryPoint()
        val job = entryPoint.appScope().launch {
            val outcome = try {
                login(account, entryPoint)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
                Outcome.Failed
            }
            AndroidUtilities.runOnUIThread {
                if (finished.compareAndSet(false, true)) {
                    when (outcome) {
                        is Outcome.PasswordNeeded -> callback.onPasswordNeeded(outcome.password)
                        is Outcome.Authorized -> callback.onAuthorized(outcome.authorization)
                        Outcome.Failed -> callback.onFailed()
                    }
                }
            }
        }
        return Cancellable {
            finished.set(true)
            job.cancel()
        }
    }

    @JvmStatic
    fun onLoginCompleted() {
        val entryPoint = entryPoint()
        entryPoint.appScope().launch {
            try {
                entryPoint.completeAppReviewerLoginUseCase().invoke(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    @JvmStatic
    fun onUpdates(account: Int, updates: TLRPC.Updates) {
        if (updates is TLRPC.TL_updateShort && updates.update is TL_update.TL_updateLoginToken) {
            loginTokenAccepted.tryEmit(account)
        }
    }

    private suspend fun login(account: Int, entryPoint: NicegramAssistantEntryPoint): Outcome = coroutineScope {
        val (firstResponse, firstError) = exportToken(account)
        if (firstResponse !is TLRPC.TL_auth_loginToken) {
            return@coroutineScope resolve(account, firstResponse, firstError, migrated = false)
        }
        val accepted = async(start = CoroutineStart.UNDISPATCHED) { loginTokenAccepted.first { it == account } }
        val token = Base64.encodeToString(firstResponse.token, Base64.NO_WRAP)
        val approved = when (entryPoint.sendAppReviewLoginTokenUseCase().invoke(token)) {
            AppReviewLoginTokenResultEntry.Accepted -> true
            AppReviewLoginTokenResultEntry.Timeout -> withTimeoutOrNull(ACCEPTANCE_TIMEOUT) { accepted.await() } != null
            AppReviewLoginTokenResultEntry.Failed -> false
        }
        accepted.cancel()
        if (!approved) return@coroutineScope Outcome.Failed
        val (response, error) = exportToken(account)
        resolve(account, response, error, migrated = false)
    }

    private suspend fun resolve(
        account: Int,
        response: TLObject?,
        error: TLRPC.TL_error?,
        migrated: Boolean,
        datacenterId: Int = ConnectionsManager.DEFAULT_DATACENTER_ID,
    ): Outcome = when {
        response is TLRPC.TL_auth_loginTokenMigrateTo -> {
            if (migrated) {
                Outcome.Failed
            } else {
                ConnectionsManager.getInstance(account).setDefaultDatacenterId(response.dc_id)
                val request = TLRPC.TL_auth_importLoginToken().apply { token = response.token }
                val (importResponse, importError) = sendRequest(account, request, response.dc_id)
                resolve(account, importResponse, importError, migrated = true, datacenterId = response.dc_id)
            }
        }

        response is TLRPC.TL_auth_loginTokenSuccess -> {
            (response.authorization as? TLRPC.TL_auth_authorization)
                ?.let { Outcome.Authorized(it) }
                ?: Outcome.Failed
        }

        error?.text == SESSION_PASSWORD_NEEDED -> requestPassword(account, datacenterId)

        else -> Outcome.Failed
    }

    private suspend fun requestPassword(account: Int, datacenterId: Int): Outcome {
        val (response, _) = sendRequest(account, TL_account.getPassword(), datacenterId)
        return (response as? TL_account.Password)?.let { Outcome.PasswordNeeded(it) } ?: Outcome.Failed
    }

    private suspend fun exportToken(account: Int): Pair<TLObject?, TLRPC.TL_error?> {
        val request = TLRPC.TL_auth_exportLoginToken().apply {
            api_id = BuildVars.APP_ID
            api_hash = BuildVars.APP_HASH
        }
        return sendRequest(account, request)
    }

    private suspend fun sendRequest(
        account: Int,
        request: TLObject,
        datacenterId: Int = ConnectionsManager.DEFAULT_DATACENTER_ID,
    ): Pair<TLObject?, TLRPC.TL_error?> = suspendCancellableCoroutine { cont ->
        val token = ConnectionsManager.getInstance(account).sendRequest(
            request,
            { response, error -> if (cont.isActive) cont.resume(response to error) },
            null, null, FLAGS, datacenterId, ConnectionsManager.ConnectionTypeGeneric, true
        )
        cont.invokeOnCancellation { ConnectionsManager.getInstance(account).cancelRequest(token, true) }
    }
}
