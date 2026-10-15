package app.nicegram

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Collections
import java.util.WeakHashMap

object NgActivityLocale : Application.ActivityLifecycleCallbacks {
    private val activities: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap())
    private val mainHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun register(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    @JvmStatic
    fun apply(activity: Activity) {
        val locale = activity.applicationContext.resources.configuration.locales[0] ?: return
        val resources = activity.resources
        if (resources.configuration.locales[0] == locale) return
        val config = Configuration()
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    @JvmStatic
    fun applyAll() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { applyAll() }
            return
        }
        activities.toList().forEach(::apply)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activities.add(activity)
        apply(activity)
    }

    override fun onActivityDestroyed(activity: Activity) {
        activities.remove(activity)
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
