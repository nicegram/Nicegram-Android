package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.preview_ic_default, R.drawable.preview_ic_default, R.string.NicegramIconDefault),
        CLASSIC("NicegramClassic", R.drawable.preview_ic_classic, R.drawable.preview_ic_classic, R.string.NicegramIconNicegramClassic),
        MONO_BLACK("MonoBlack", R.drawable.preview_ic_mono_black, R.drawable.preview_ic_mono_black, R.string.NicegramIconMonoBlack),
        FILLED("Filled", R.drawable.preview_ic_filled, R.drawable.preview_ic_filled, R.string.NicegramIconFilled),
        FILLED_BLACK("FilledBlack", R.drawable.preview_ic_filled_black, R.drawable.preview_ic_filled_black, R.string.NicegramIconFilledBlack),
        NICEGRAM("Nicegram", R.drawable.preview_ic_nicegram, R.drawable.preview_ic_nicegram, R.string.NicegramIconNicegram),
        NICEGRAM_LIGHT("NicegramLight", R.drawable.preview_ic_nicegram_light, R.drawable.preview_ic_nicegram_light, R.string.NicegramIconNicegramLight),
        NICEGRAM_SPECIAL("NicegramSpecial", R.drawable.preview_ic_modern, R.drawable.preview_ic_modern, R.string.NicegramIconSpecial),
        COMMUNITY_CLASSIC("CommunityClassic", R.drawable.preview_community_classic, R.drawable.preview_community_classic, R.string.CommunityIconClassic),
        COMMUNITY_ONYX("CommunityOnyx", R.drawable.preview_community_onyx, R.drawable.preview_community_onyx, R.string.CommunityIconOnyx),
        COMMUNITY_RUBY("CommunityRuby", R.drawable.preview_community_ruby, R.drawable.preview_community_ruby, R.string.CommunityIconRuby),
        COMMUNITY_WATERCOLOR("CommunityWatercolor", R.drawable.preview_community_watercolor, R.drawable.preview_community_watercolor, R.string.CommunityIconWatercolor);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
