package com.appvillis.nicegram.presentation

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.util.DisplayMetrics
import androidx.constraintlayout.widget.ConstraintLayout
import com.appvillis.lib_android_base.viewBinding
import com.appvillis.nicegram.R
import com.appvillis.nicegram.databinding.DialogSmsTutorialBinding
import kotlin.math.roundToInt

class NicegramTutorialSmsDialog(context: Context) : Dialog(context) {
    private val binding by viewBinding<DialogSmsTutorialBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adjustSizeForLandscape()

        setCancelable(false)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.okBtn.setOnClickListener { dismiss() }

        binding.text.text = Html.fromHtml(context.getString(R.string.NicegarmTutorial_Dialog_Steps_Styled))
    }

    private fun adjustSizeForLandscape() {
        val displayMetrics = DisplayMetrics()
        window!!.windowManager!!.defaultDisplay.getMetrics(displayMetrics)
        val constraintLp = binding.text.layoutParams as ConstraintLayout.LayoutParams
        constraintLp.matchConstraintMaxWidth = (0.35f * displayMetrics.heightPixels).roundToInt()
    }
}
