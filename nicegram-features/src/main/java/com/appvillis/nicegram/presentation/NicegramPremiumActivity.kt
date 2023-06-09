package com.appvillis.nicegram.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.appvillis.feature_nicegram_billing.presentation.NicegramPremiumFragment
import com.appvillis.lib_android_base.setTransparentStatusBar
import com.appvillis.nicegram.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NicegramPremiumActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_SHOW_CONTINUE_BTN = "EXTRA_SHOW_CONTINUE_BTN"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.navigationBarColor = Color.parseColor("#131417")
        setTransparentStatusBar()

        setContentView(R.layout.activity_nicegram_fragment)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, NicegramPremiumFragment.newInstance(intent.getBooleanExtra(EXTRA_SHOW_CONTINUE_BTN, false)))
            .commit()
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)

        super.onBackPressed()
    }
}
