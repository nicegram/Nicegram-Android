package app.nicegram.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.ViewCompat
import org.telegram.messenger.databinding.NgViewImportAccountLoginBinding
import org.telegram.ui.ActionBar.Theme

class ImportAccountLoginView(context: Context, listener: () -> Unit) : FrameLayout(context) {
    init {
        NgViewImportAccountLoginBinding.inflate(LayoutInflater.from(context), this, true).apply {
            btn.setOnClickListener { listener() }
            btn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            //text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText))

            val color = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
            val dividerColor = Color.argb(
                24,
                color.red,
                color.green,
                color.blue
            )

            ViewCompat.setBackgroundTintList(
                btn,
                ColorStateList.valueOf(dividerColor)
            )
        }
    }
}