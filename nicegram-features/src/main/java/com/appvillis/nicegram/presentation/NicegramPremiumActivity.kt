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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.navigationBarColor = Color.BLACK
        setTransparentStatusBar()

        setContentView(R.layout.activity_nicegram_fragment)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainerView, NicegramPremiumFragment())
            .commit()
    }

    override fun onBackPressed() {
        setResult(RESULT_OK)

        super.onBackPressed()
    }
}
