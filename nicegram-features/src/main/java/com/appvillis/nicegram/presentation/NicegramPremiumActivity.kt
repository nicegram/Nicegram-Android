package com.appvillis.nicegram.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.appvillis.nicegram.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NicegramPremiumActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.BLACK
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

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