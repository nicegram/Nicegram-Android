package com.appvillis.nicegram.presentation

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.appvillis.nicegram.NicegramDialogs
import com.appvillis.nicegram.R
import com.appvillis.nicegram.domain.NicegramFeaturesOnboardingUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NicegramOnboardingActivity : AppCompatActivity() {
    companion object {
        private const val FORCE_ONBOARDING = false
        var instance: NicegramOnboardingActivity? = null
    }
    @Inject
    lateinit var nicegramFeaturesOnboardingUseCase: NicegramFeaturesOnboardingUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        instance = this

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

        setContentView(R.layout.activity_nicegram_fragment)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, NicegramOnboardingFragment())
            .commit()

        Handler(Looper.getMainLooper()).postDelayed({
            showPermissionDialog()
        }, 1000)
    }

    override fun onDestroy() {
        instance = null

        super.onDestroy()
    }

    override fun onBackPressed() {
        startActivityForResult(Intent(this, NicegramPremiumActivity::class.java), 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val isOk = nicegramFeaturesOnboardingUseCase.hasSeenOnboarding

        setResult(if (isOk) RESULT_OK else if (FORCE_ONBOARDING) RESULT_CANCELED else RESULT_OK)

        super.onBackPressed()
    }

    fun showPermissionDialog() {
        NicegramDialogs.showContactsPermissionDialogIfNeeded(this)
    }
}