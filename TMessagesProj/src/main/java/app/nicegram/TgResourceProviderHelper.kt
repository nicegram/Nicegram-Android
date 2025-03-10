package app.nicegram

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
}
