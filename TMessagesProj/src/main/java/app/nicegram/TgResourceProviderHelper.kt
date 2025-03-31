package app.nicegram

import android.graphics.Color
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.appvillis.core_resources.domain.TgResourceProvider
import org.telegram.messenger.ApplicationLoader
import org.telegram.ui.ActionBar.Theme

class TgThemeProxyImpl : TgResourceProvider.ThemeProxy {

    private val resources get() = ApplicationLoader.applicationContext.resources

    override fun isNightTheme(): Boolean {
        return Theme.isCurrentThemeNight() || Theme.isCurrentThemeDark()
    }

    override fun typeAllTgColors() {

    }

    override fun profileBgColor(): Int {
        return Theme.getColor(Theme.key_windowBackgroundGray)
    }

    override fun accentColor(): Int {
        return Theme.getColor(Theme.key_switch2TrackChecked)
    }

    override fun profilePrimaryColor(): Int {
        return Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
    }

    override fun profileSecondaryColor(): Int {
        return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
    }

    override fun profileDividerColor(): Int {
        return Theme.getColor(Theme.key_divider)
    }

    override fun profileSecondaryBgColor(): Int {
        return Theme.getColor(Theme.key_windowBackgroundWhite)
    }

    override fun onAccentColor(): Int {
        return Color.WHITE
    }

    override fun textPrimaryColor(): Int {
        return Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)
    }

    override fun textSecondaryColor(): Int {
        return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText)
    }

    override fun bgSecondaryColor(): Int {
        return if (isNightTheme()) Theme.getColor(Theme.key_windowBackgroundWhite) else Theme.getColor(Theme.key_windowBackgroundGray)
    }

    override fun bgPrimaryColor(): Int {
        return if (isNightTheme()) Theme.getColor(Theme.key_windowBackgroundGray) else Theme.getColor(Theme.key_windowBackgroundWhite)
    }

    override fun bgTertiaryColor(): Int {
        val primary = bgPrimaryColor()
        val secondary = bgSecondaryColor()

        return Color.rgb(
            (primary.red + secondary.red) / 2,
            (primary.green + secondary.green) / 2,
            (primary.blue + secondary.blue) / 2,
        )
    }

    override fun badgeBg(): Int {
        val color = Theme.getColor(Theme.key_chats_unreadCounterMuted)
        return Color.rgb(
            //30,
            color.red,
            color.green,
            color.blue
        )
    }

    override fun badgeText(): Int {
        return Theme.getColor(Theme.key_chats_unreadCounterText)
    }

    override fun borderPrimary(): Int {
        //return Theme.getColor(Theme.key_divider)
        val color = textSecondaryColor()
        return Color.argb(
            24,
            color.red,
            color.green,
            color.blue
        )
    }
}
