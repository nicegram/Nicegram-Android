package com.appvillis.nicegram.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.res.ResourcesCompat
import com.appvillis.feature_nicegram_billing.presentation.NicegramPremiumFragment
import com.appvillis.feature_nicegram_client.TgThemeHelper
import com.appvillis.lib_android_base.clearLightStatusBar
import com.appvillis.lib_android_base.isNightMode
import com.appvillis.lib_android_base.setLightStatusBar
import com.appvillis.lib_android_base.setTransparentStatusBar
import com.appvillis.nicegram.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NicegramPremiumActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SHOW_CONTINUE_BTN = "EXTRA_SHOW_CONTINUE_BTN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setDayNightTheme()

        super.onCreate(savedInstanceState)

        window.navigationBarColor = Color.parseColor("#131417")
        setTransparentStatusBar()

        setContentView(R.layout.activity_nicegram_fragment)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, NicegramPremiumFragment.newInstance(intent.getBooleanExtra(EXTRA_SHOW_CONTINUE_BTN, false)))
            .commit()

        if (this.isNightMode()) {
            clearLightStatusBar()
        } else {
            setLightStatusBar()
        }

        window.navigationBarColor = ResourcesCompat.getColor(resources, R.color.assistant_bg, null)
    }

    private fun setDayNightTheme() {
        if (TgThemeHelper.TG_RESOURCE_PROVIDER?.theme?.isNightTheme() == true) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)

        super.onBackPressed()
    }
}
