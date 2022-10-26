package org.telegram.messenger;

import org.telegram.messenger.regular.BuildConfig;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }
}
