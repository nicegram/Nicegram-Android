package app.nicegram.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.appvillis.feature_attention_economy.AttEntryPoint
import com.appvillis.feature_attention_economy.domain.entities.AttAd
import com.appvillis.feature_attention_economy.domain.entities.AttPlacement
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.telegram.ui.DialogsActivity
import org.telegram.ui.LaunchActivity
import timber.log.Timber

class DialogsAttHelper(val context: Context, val dialogFragment: DialogsActivity, val onAdChanged: () -> Unit) {
    var ad: AttAd? = null
    private var pendingAd: PendingAd = PendingAd.None

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fragmentCheckJob: Job? = null
    private var lifecycleObserver: Application.ActivityLifecycleCallbacks? = null
    private var tgActivityIsPaused = false
    private var wasPausedOnce = false

    private val attEntryPoints = EntryPoints.get(context.applicationContext, AttEntryPoint::class.java)
    private var getSettingsUseCase = attEntryPoints.getGetSettingsUseCase()

    init {
        initLifecycleObserver()
        startFragmentCheck()

        scope.launch {
            attEntryPoints.getGetPlacementAdsUseCase().getPlacementAdsFlow(AttPlacement.AttPlacementType.Pin).collect {
                Timber.d("onAdReceived $it")
                if (!attPinEnabled) {
                    setAdAndCallback(it)
                } else if (tgActivityIsPaused || !areDialogsVisible()) {
                    if (it != ad) setAdAndCallback(it)
                } else {
                    if (wasPausedOnce) {
                        Timber.d("-set ad as pending $it")
                        pendingAd = PendingAd.Ad(it)
                    }
                    else setAdAndCallback(it)
                }
            }
        }
    }

    private val attPinEnabled get() = getSettingsUseCase().settings(AttPlacement.AttPlacementType.Pin)?.enabled == true

    private fun setAdAndCallback(ad: AttAd?) {
        Timber.d("*Ad set $ad")
        this.pendingAd = PendingAd.None

        this.ad = ad
        onAdChanged()
    }

    private fun startFragmentCheck() {
        fragmentCheckJob?.cancel()
        fragmentCheckJob = scope.launch {
            while (isActive) {
                checkPendingAd()
                delay(2000)
                doCleanupIfNeeded()
            }
        }
    }

    fun cleanup() {
        scope.launch {
            try {
                fragmentCheckJob?.cancel()
                coroutineContext.cancel()
                lifecycleObserver?.let { observer ->
                    (context as? Activity)?.application?.unregisterActivityLifecycleCallbacks(observer)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during cleanup")
            }
        }
    }

    private fun checkPendingAd() {
        if (doCleanupIfNeeded()) return

        Timber.d("checkPendingAd $pendingAd tgActivityIsPaused: $tgActivityIsPaused lastFragment:${dialogFragment.parentLayout?.lastFragment}")

        val pendingAd = pendingAd
        if ((tgActivityIsPaused || !areDialogsVisible()) && pendingAd is PendingAd.Ad && pendingAd.ad != ad) {
            setAdAndCallback(pendingAd.ad)
        }
    }

    private fun areDialogsVisible() = dialogFragment.parentLayout?.lastFragment is DialogsActivity

    fun pause() {
        Timber.d("pause")

        checkPendingAd()
    }

    private fun doCleanupIfNeeded(): Boolean {
        if (dialogFragment.parentLayout == null) {
            Timber.d("clean up")
            //cleanup()
            return true
        }

        return false
    }

    private fun initLifecycleObserver() {
        lifecycleObserver = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                Timber.d("onActivityResumed $activity")
                if (activity is LaunchActivity) tgActivityIsPaused = false
            }

            override fun onActivityPaused(activity: Activity) {
                Timber.d("onActivityPaused $activity")
                if (activity is LaunchActivity) {
                    tgActivityIsPaused = true
                    wasPausedOnce = true
                    checkPendingAd()
                }
            }

            override fun onActivityStopped(activity: Activity) {

            }

            override fun onActivityDestroyed(activity: Activity) {

            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

            }
        }

        (context as? Activity)?.application?.registerActivityLifecycleCallbacks(lifecycleObserver)
    }

    sealed class PendingAd {
        data object None : PendingAd()
        data class Ad(val ad: AttAd?) : PendingAd()
    }
}