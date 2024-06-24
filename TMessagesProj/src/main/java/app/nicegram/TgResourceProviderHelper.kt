package app.nicegram

import com.appvillis.core_resources.domain.TgResourceProvider
import org.telegram.ui.ActionBar.Theme

class TgThemeProxyImpl : TgResourceProvider.ThemeProxy {

    override fun isNightTheme(): Boolean {
        return Theme.isCurrentThemeNight() || Theme.isCurrentThemeDark()
    }

    override fun typeAllTgColors() {

    }
}
