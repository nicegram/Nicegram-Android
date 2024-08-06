/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.appvillis.assistant_core.app.AppInit;
import com.appvillis.core_network.ApiService;
import com.appvillis.core_resources.domain.TgResourceProvider;
import com.appvillis.feature_ai_chat.domain.AiChatCommandsRepository;
import com.appvillis.feature_ai_chat.domain.AiChatRemoteConfigRepo;
import com.appvillis.feature_ai_chat.domain.ClearDataUseCase;
import com.appvillis.feature_ai_chat.domain.UseResultManager;
import com.appvillis.feature_ai_chat.domain.usecases.GetBalanceTopUpRequestUseCase;
import com.appvillis.feature_ai_chat.domain.usecases.GetChatCommandsUseCase;
import com.appvillis.feature_analytics.domain.AnalyticsManager;
import com.appvillis.feature_avatar_generator.domain.usecases.AvatarsOnboardingUseCase;
import com.appvillis.feature_avatar_generator.domain.usecases.GetAvatarsUseCase;
import com.appvillis.feature_nicegram_assistant.QrCodeHelper;
import com.appvillis.feature_nicegram_assistant.domain.GetNicegramOnboardingStatusUseCase;
import com.appvillis.feature_nicegram_assistant.domain.GetSpecialOfferUseCase;
import com.appvillis.feature_nicegram_billing.NicegramBillingHelper;
import com.appvillis.feature_nicegram_billing.domain.BillingManager;
import com.appvillis.feature_nicegram_billing.domain.RequestInAppsUseCase;
import com.appvillis.feature_nicegram_client.domain.CollectGroupInfoUseCase;
import com.appvillis.feature_nicegram_client.domain.CommonRemoteConfigRepo;
import com.appvillis.feature_nicegram_client.domain.NicegramSessionCounter;
import com.appvillis.nicegram.AiChatBotHelper;
import com.appvillis.nicegram.AnalyticsHelper;
import com.appvillis.nicegram.NicegramAssistantHelper;
import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;

import app.nicegram.DailyRewardsHelper;
import app.nicegram.NicegramAnalyticsHelper;

import com.appvillis.nicegram.NicegramIcWalletHelper;
import com.appvillis.nicegram.NicegramPinChatsPlacementHelper;
import com.appvillis.nicegram.NicegramPrefs;

import app.nicegram.NicegramGroupCollectHelper;
import app.nicegram.NicegramSpeechToTextHelper;
import com.appvillis.nicegram.ReviewHelper;
import com.appvillis.feature_nicegram_client.domain.NicegramClientOnboardingUseCase;
import com.appvillis.nicegram.network.NicegramNetwork;
import com.appvillis.nicegram_wallet.module_bridge.InChatResultManager;
import com.appvillis.nicegram_wallet.wallet_storage.domain.GetCurrentWalletUseCase;
import com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcDeeplinkManager;
import com.appvillis.rep_placements.domain.GetChatPlacementsUseCase;
import com.appvillis.rep_placements.domain.GetPinChatsPlacementsUseCase;
import com.appvillis.rep_user.domain.AppSessionControlUseCase;
import com.appvillis.rep_user.domain.ClaimDailyRewardUseCase;
import com.appvillis.rep_user.domain.GetUserStatusUseCase;
import com.appvillis.rep_user.domain.UserRepository;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONObject;
import org.telegram.messenger.voip.VideoCapturerDevice;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Adapters.DrawerLayoutAdapter;
import org.telegram.ui.Components.ForegroundDetector;
import org.telegram.ui.IUpdateLayout;
import org.telegram.ui.LauncherIconController;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import app.nicegram.NicegramDoubleBottom;
import app.nicegram.NicegramWalletHelper;
import app.nicegram.PrefsHelper;
import app.nicegram.TgThemeProxyImpl;
import timber.log.Timber;

public class ApplicationLoader extends Application {

    public static ApplicationLoader applicationLoaderInstance;

    @SuppressLint("StaticFieldLeak")
    public static volatile Context applicationContext;
    public static volatile NetworkInfo currentNetworkInfo;
    public static volatile Handler applicationHandler;

    private static ConnectivityManager connectivityManager;
    private static volatile boolean applicationInited = false;
    private static volatile  ConnectivityManager.NetworkCallback networkCallback;
    private static long lastNetworkCheckTypeTime;
    private static int lastKnownNetworkType = -1;

    public static long startTime;

    public static volatile boolean isScreenOn = false;
    public static volatile boolean mainInterfacePaused = true;
    public static volatile boolean mainInterfaceStopped = true;
    public static volatile boolean externalInterfacePaused = true;
    public static volatile boolean mainInterfacePausedStageQueue = true;
    public static boolean canDrawOverlays;
    public static volatile long mainInterfacePausedStageQueueTime;

    private static PushListenerController.IPushListenerServiceProvider pushProvider;
    private static IMapsProvider mapsProvider;
    private static ILocationServiceProvider locationServiceProvider;

    @Inject
    public ClaimDailyRewardUseCase claimDailyRewardUseCase;
    @Inject
    public GetUserStatusUseCase getUserStatusUseCase;
    @Inject
    public GetCurrentWalletUseCase getCurrentWalletUseCase;
    @Inject
    public GetChatCommandsUseCase getChatCommandsUseCase;
    @Inject
    public AiChatCommandsRepository commandsRepo;
    @Inject
    public GetBalanceTopUpRequestUseCase getBalanceTopUpRequestUseCase;
    @Inject
    public RequestInAppsUseCase requestInAppsUseCase;
    @Inject
    public NicegramClientOnboardingUseCase nicegramClientOnboardingUseCase;
    @Inject
    public GetNicegramOnboardingStatusUseCase getNicegramOnboardingStatusUseCase;
    @Inject
    public CollectGroupInfoUseCase collectGroupInfoUseCase;
    @Inject
    public AppSessionControlUseCase appSessionControlUseCase;
    @Inject
    public GetPinChatsPlacementsUseCase pinChatsPlacementsUseCase;
    @Inject
    public GetChatPlacementsUseCase chatPlacementsUseCase;
    @Inject
    public GetSpecialOfferUseCase getSpecialOfferUseCase;
    @Inject
    public AvatarsOnboardingUseCase avatarsOnboardingUseCase;
    @Inject
    public GetAvatarsUseCase getAvatarsUseCase;
    @Inject
    public BillingManager billingManager;
    @Inject
    public UserRepository userRepository;
    @Inject
    public CommonRemoteConfigRepo remoteConfigRepo;

    @Inject
    public AiChatRemoteConfigRepo remoteConfigRepoAi;
    @Inject
    public NicegramSessionCounter nicegramSessionCounter;
    @Inject
    public UseResultManager useResultManager;
    @Inject
    public ClearDataUseCase clearDataUseCase;
    @Inject
    public AppInit appInit;
    @Inject
    public TgResourceProvider tgResourceProvider;
    @Inject
    public AnalyticsManager analyticsManager;
    @Inject
    public ApiService apiService;

    @Inject
    public TcDeeplinkManager tcDeeplinkManager;

    @Inject
    public InChatResultManager inChatResultManager;

    private static ApplicationLoader appInstance = null;
    public static ApplicationLoader getInstance() {
        return appInstance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static ILocationServiceProvider getLocationServiceProvider() {
        if (locationServiceProvider == null) {
            locationServiceProvider = applicationLoaderInstance.onCreateLocationServiceProvider();
            locationServiceProvider.init(applicationContext);
        }
        return locationServiceProvider;
    }

    protected ILocationServiceProvider onCreateLocationServiceProvider() {
        return new GoogleLocationProvider();
    }

    public static IMapsProvider getMapsProvider() {
        if (mapsProvider == null) {
            mapsProvider = applicationLoaderInstance.onCreateMapsProvider();
        }
        return mapsProvider;
    }

    protected IMapsProvider onCreateMapsProvider() {
        return new GoogleMapsProvider();
    }

    public static PushListenerController.IPushListenerServiceProvider getPushProvider() {
        if (pushProvider == null) {
            pushProvider = applicationLoaderInstance.onCreatePushProvider();
        }
        return pushProvider;
    }

    protected PushListenerController.IPushListenerServiceProvider onCreatePushProvider() {
        return PushListenerController.GooglePushListenerServiceProvider.INSTANCE;
    }

    public static String getApplicationId() {
        return applicationLoaderInstance.onGetApplicationId();
    }

    protected String onGetApplicationId() {
        return null;
    }

    public static boolean isHuaweiStoreBuild() {
        return applicationLoaderInstance.isHuaweiBuild();
    }

    public static boolean isStandaloneBuild() {
        return applicationLoaderInstance.isStandalone();
    }

    protected boolean isHuaweiBuild() {
        return false;
    }

    protected boolean isStandalone() {
        return false;
    }

    public static File getFilesDirFixed() {
        for (int a = 0; a < 10; a++) {
            File path = ApplicationLoader.applicationContext.getFilesDir();
            if (path != null) {
                return path;
            }
        }
        try {
            ApplicationInfo info = applicationContext.getApplicationInfo();
            File path = new File(info.dataDir, "files");
            path.mkdirs();
            return path;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new File("/data/data/org.telegram.messenger/files");
    }

    public static void postInitApplication() {
        if (applicationInited || applicationContext == null) {
            return;
        }
        applicationInited = true;
        NativeLoader.initNativeLibs(ApplicationLoader.applicationContext);

        try {
            LocaleController.getInstance(); //TODO improve
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            connectivityManager = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        currentNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    } catch (Throwable ignore) {

                    }

                    boolean isSlow = isConnectionSlow();
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        ConnectionsManager.getInstance(a).checkConnection();
                        FileLoader.getInstance(a).onNetworkChanged(isSlow);
                    }
                }
            };
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            ApplicationLoader.applicationContext.registerReceiver(networkStateReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            final BroadcastReceiver mReceiver = new ScreenReceiver();
            applicationContext.registerReceiver(mReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            isScreenOn = pm.isScreenOn();
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("screen state = " + isScreenOn);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedConfig.loadConfig();
        SharedPrefsHelper.init(applicationContext);
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) { //TODO improve account
            UserConfig.getInstance(a).loadConfig();
            MessagesController.getInstance(a);
            if (a == 0) {
                SharedConfig.pushStringStatus = "__FIREBASE_GENERATING_SINCE_" + ConnectionsManager.getInstance(a).getCurrentTime() + "__";
            } else {
                ConnectionsManager.getInstance(a);
            }
            TLRPC.User user = UserConfig.getInstance(a).getCurrentUser();
            if (user != null) {
                MessagesController.getInstance(a).putUser(user, true);
                SendMessagesHelper.getInstance(a).checkUnsentMessages();
            }
        }

        ApplicationLoader app = (ApplicationLoader) ApplicationLoader.applicationContext;
        app.initPushServices();
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("app initied");
        }

        MediaController.getInstance();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) { //TODO improve account
            ContactsController.getInstance(a).checkAppAccount();
            DownloadController.getInstance(a);
        }
        BillingController.getInstance().startConnection();
    }

    public ApplicationLoader() {
        super();
    }

    @Override
    public void onCreate() {
        applicationLoaderInstance = this;
        appInstance = this;
        try {
            applicationContext = getApplicationContext();
        } catch (Throwable ignore) {

        }

        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant((Timber.Tree) new AppInit.DebugTree());
        } else {
            Timber.plant(new AppInit.CrashReportingTree());
        }

        setMaxAccountCount(); // ng
        initNicegram();

        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("app start time = " + (startTime = SystemClock.elapsedRealtime()));
            try {
                FileLog.d("buildVersion = " + ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0).versionCode);
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (applicationContext == null) {
            applicationContext = getApplicationContext();
        }

        NativeLoader.initNativeLibs(ApplicationLoader.applicationContext);
        try {
            ConnectionsManager.native_setJava(false);
        } catch (UnsatisfiedLinkError error) {
            throw new RuntimeException("can't load native libraries " +  Build.CPU_ABI + " lookup folder " + NativeLoader.getAbiFolder());
        }
        new ForegroundDetector(this) {
            @Override
            public void onActivityStarted(Activity activity) {
                boolean wasInBackground = isBackground();
                super.onActivityStarted(activity);
                if (wasInBackground) {
                    ensureCurrentNetworkGet(true);
                }
            }
        };
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("load libs time = " + (SystemClock.elapsedRealtime() - startTime));
        }

        applicationHandler = new Handler(applicationContext.getMainLooper());

        AndroidUtilities.runOnUIThread(ApplicationLoader::startPushService);

        LauncherIconController.tryFixLauncherIconIfNeeded();
        ProxyRotationController.init();
    }

    public static void startPushService() {
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        boolean enabled;
        if (preferences.contains("pushService")) {
            enabled = preferences.getBoolean("pushService", true);
        } else {
            enabled = MessagesController.getMainSettings(UserConfig.selectedAccount).getBoolean("keepAliveService", false);
        }
        if (enabled) {
            try {
                applicationContext.startService(new Intent(applicationContext, NotificationsService.class));
            } catch (Throwable ignore) {

            }
        } else {
            applicationContext.stopService(new Intent(applicationContext, NotificationsService.class));
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        try {
            LocaleController.getInstance().onDeviceConfigurationChange(newConfig);
            AndroidUtilities.checkDisplaySize(applicationContext, newConfig);
            VideoCapturerDevice.checkScreenCapturerSize();
            AndroidUtilities.resetTabletFlag();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initPushServices() {
        AndroidUtilities.runOnUIThread(() -> {
            if (getPushProvider().hasServices()) {
                getPushProvider().onRequestPushToken();
            } else {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("No valid " + getPushProvider().getLogTitle() + " APK found.");
                }
                SharedConfig.pushStringStatus = "__NO_GOOGLE_PLAY_SERVICES__";
                PushListenerController.sendRegistrationToServer(getPushProvider().getPushType(), null);
            }
        }, 1000);
    }

    private boolean checkPlayServices() {
        try {
            int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            return resultCode == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return true;
    }

    private static long lastNetworkCheck = -1;
    private static void ensureCurrentNetworkGet() {
        final long now = System.currentTimeMillis();
        ensureCurrentNetworkGet(now - lastNetworkCheck > 5000);
        lastNetworkCheck = now;
    }

    private static void ensureCurrentNetworkGet(boolean force) {
        if (force || currentNetworkInfo == null) {
            try {
                if (connectivityManager == null) {
                    connectivityManager = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                }
                currentNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (networkCallback == null) {
                        networkCallback = new ConnectivityManager.NetworkCallback() {
                            @Override
                            public void onAvailable(@NonNull Network network) {
                                lastKnownNetworkType = -1;
                            }

                            @Override
                            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                                lastKnownNetworkType = -1;
                            }
                        };
                        connectivityManager.registerDefaultNetworkCallback(networkCallback);
                    }
                }
            } catch (Throwable ignore) {

            }
        }
    }

    public static boolean isRoaming() {
        try {
            ensureCurrentNetworkGet(false);
            return currentNetworkInfo != null && currentNetworkInfo.isRoaming();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static boolean isConnectedOrConnectingToWiFi() {
        try {
            ensureCurrentNetworkGet(false);
            if (currentNetworkInfo != null && (currentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI || currentNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET)) {
                NetworkInfo.State state = currentNetworkInfo.getState();
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING || state == NetworkInfo.State.SUSPENDED) {
                    return true;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static boolean isConnectedToWiFi() {
        try {
            ensureCurrentNetworkGet(false);
            if (currentNetworkInfo != null && (currentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI || currentNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) && currentNetworkInfo.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static boolean isConnectionSlow() {
        try {
            ensureCurrentNetworkGet(false);
            if (currentNetworkInfo != null && currentNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                switch (currentNetworkInfo.getSubtype()) {
                    case TelephonyManager.NETWORK_TYPE_1xRTT:
                    case TelephonyManager.NETWORK_TYPE_CDMA:
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                    case TelephonyManager.NETWORK_TYPE_IDEN:
                        return true;
                }
            }
        } catch (Throwable ignore) {

        }
        return false;
    }

    public static int getAutodownloadNetworkType() {
        try {
            ensureCurrentNetworkGet(false);
            if (currentNetworkInfo == null) {
                return StatsController.TYPE_MOBILE;
            }
            if (currentNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI || currentNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (lastKnownNetworkType == StatsController.TYPE_MOBILE || lastKnownNetworkType == StatsController.TYPE_WIFI) && System.currentTimeMillis() - lastNetworkCheckTypeTime < 5000) {
                    return lastKnownNetworkType;
                }
                if (connectivityManager.isActiveNetworkMetered()) {
                    lastKnownNetworkType = StatsController.TYPE_MOBILE;
                } else {
                    lastKnownNetworkType = StatsController.TYPE_WIFI;
                }
                lastNetworkCheckTypeTime = System.currentTimeMillis();
                return lastKnownNetworkType;
            }
            if (currentNetworkInfo.isRoaming()) {
                return StatsController.TYPE_ROAMING;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return StatsController.TYPE_MOBILE;
    }

    public static int getCurrentNetworkType() {
        if (isConnectedOrConnectingToWiFi()) {
            return StatsController.TYPE_WIFI;
        } else if (isRoaming()) {
            return StatsController.TYPE_ROAMING;
        } else {
            return StatsController.TYPE_MOBILE;
        }
    }

    public static boolean isNetworkOnlineFast() {
        try {
            ensureCurrentNetworkGet(false);
            if (currentNetworkInfo == null) {
                return true;
            }
            if (currentNetworkInfo.isConnectedOrConnecting() || currentNetworkInfo.isAvailable()) {
                return true;
            }

            NetworkInfo netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                return true;
            } else {
                netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    return true;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            return true;
        }
        return false;
    }

    public static boolean isNetworkOnlineRealtime() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && (netInfo.isConnectedOrConnecting() || netInfo.isAvailable())) {
                return true;
            }

            netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                return true;
            } else {
                netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (netInfo != null && netInfo.isConnectedOrConnecting()) {
                    return true;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
            return true;
        }
        return false;
    }

    public static boolean isNetworkOnline() {
        boolean result = isNetworkOnlineRealtime();
        if (BuildVars.DEBUG_PRIVATE_VERSION) {
            boolean result2 = isNetworkOnlineFast();
            if (result != result2) {
                FileLog.d("network online mismatch");
            }
        }
        return result;
    }

    private void initNicegram() {
        NicegramDoubleBottom.INSTANCE.init(this);

        nicegramSessionCounter.increaseSessionCount();
        ReviewHelper.INSTANCE.setNicegramSessionCounter(nicegramSessionCounter);

        appInit.initialize();

        appSessionControlUseCase.increaseSessionCount();
        tgResourceProvider.setThemeProxy(new TgThemeProxyImpl());

        AnalyticsHelper.INSTANCE.setAnalyticsManager(analyticsManager);

        DailyRewardsHelper.INSTANCE.setUseCase(claimDailyRewardUseCase);

        AiChatBotHelper.INSTANCE.setGetChatCommandsUseCase(getChatCommandsUseCase);
        AiChatBotHelper.INSTANCE.setGetUserStatusUseCase(getUserStatusUseCase);
        AiChatBotHelper.INSTANCE.setClearDataUseCase(clearDataUseCase);
        AiChatBotHelper.INSTANCE.setRequestInAppsUseCase(requestInAppsUseCase);
        AiChatBotHelper.INSTANCE.setGetBalanceTopUpRequestUseCase(getBalanceTopUpRequestUseCase);
        AiChatBotHelper.INSTANCE.setUseResultManager(useResultManager);
        AiChatBotHelper.INSTANCE.setTgResourceProvider(tgResourceProvider);

        NicegramAssistantHelper.INSTANCE.setGetNicegramOnboardingStatusUseCase(getNicegramOnboardingStatusUseCase);
        NicegramAssistantHelper.INSTANCE.setGetSpecialOfferUseCase(getSpecialOfferUseCase);
        NicegramAssistantHelper.INSTANCE.setAppSessionControlUseCase(appSessionControlUseCase);
        NicegramAssistantHelper.INSTANCE.setGetChatPlacementsUseCase(chatPlacementsUseCase);
        NicegramAssistantHelper.INSTANCE.setAiChatConfigRepo(remoteConfigRepoAi);
        NicegramAssistantHelper.INSTANCE.setAvatarsOnboardingUseCase(avatarsOnboardingUseCase);
        NicegramAssistantHelper.INSTANCE.setGetAvatarsUseCase(getAvatarsUseCase);
        NicegramPinChatsPlacementHelper.INSTANCE.setGetPinChatsPlacementsUseCase(pinChatsPlacementsUseCase);
        NicegramAnalyticsHelper.INSTANCE.setAnalyticsManager(analyticsManager);
        NicegramBillingHelper.INSTANCE.setBillingManager(billingManager);
        NicegramBillingHelper.INSTANCE.setUserRepository(userRepository);
        NicegramGroupCollectHelper.INSTANCE.setCollectGroupInfoUseCase(collectGroupInfoUseCase);
        PrefsHelper.INSTANCE.setRemoteConfigRepo(remoteConfigRepo);
        NicegramSpeechToTextHelper.INSTANCE.setApiService(apiService);
        NicegramWalletHelper.INSTANCE.setTcDeeplinkManager(tcDeeplinkManager);
        NicegramWalletHelper.INSTANCE.setGetUserStatusUseCase(getUserStatusUseCase);
        NicegramWalletHelper.INSTANCE.setGetCurrentWalletUseCase(getCurrentWalletUseCase);
        NicegramIcWalletHelper.INSTANCE.setInChatResultManager(inChatResultManager);

        AnalyticsHelper.INSTANCE.logEvent(getUserStatusUseCase.isUserLoggedIn() ? "nicegram_session_authenticated" : "nicegram_session_anon", null);
        new Handler().postDelayed(() -> NicegramNetwork.INSTANCE.getSettings(UserConfig.getInstance(UserConfig.selectedAccount).clientUserId), 3000);
        new Handler().postDelayed(() -> {
            int accountCount = 0;
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    accountCount++;
                }
            }
            int accountCountToLog = accountCount;
            if (accountCount > 1 && accountCount <= 5) {
                accountCountToLog = 5;
            } else if (accountCount > 1 && accountCount <= 100) {
                accountCountToLog = (int) Math.round(accountCount / 10.0) * 10;
            }
            Map<String, String> paramsMap = new HashMap<>();
            paramsMap.put("profiles_count", String.valueOf(accountCount));
            AnalyticsHelper.INSTANCE.logEvent("user_set_"+accountCountToLog+"_profiles", paramsMap);
        }, 5000);

        setQrRenderer();
    }

    private void setQrRenderer() {
        QrCodeHelper.INSTANCE.setQrRenderer((s, width, height) -> {
            try {
                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(s, BarcodeFormat.QR_CODE, width, height);

                int w = bitMatrix.getWidth();
                int h = bitMatrix.getHeight();
                int[] pixels = new int[w * h];
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        pixels[y * w + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                    }
                }

                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private void setMaxAccountCount() {
        if (PrefsHelper.INSTANCE.getMaxAccountCountWasSet(this)) {
            UserConfig.MAX_ACCOUNT_COUNT = PrefsHelper.INSTANCE.getMaxAccountCount(this);
            UserConfig.MAX_ACCOUNT_DEFAULT_COUNT = PrefsHelper.INSTANCE.getMaxAccountCount(this);
        } else {

            int accountCount = 0;
            for (int a = 0; a < NicegramPrefs.PREF_MAX_ACCOUNTS_MAX; a++) {
                if (UserConfig.getInstance(a).isClientActivatedEarlyCheck()) {
                    accountCount++;
                }
            }

            int accountsToSet = NicegramPrefs.PREF_MAX_ACCOUNTS_MAX;
            if (accountCount <= 1) accountsToSet = NicegramPrefs.PREF_MAX_ACCOUNTS_DEFAULT;

            PrefsHelper.INSTANCE.setMaxAccountCount(this, accountsToSet);
            UserConfig.MAX_ACCOUNT_COUNT = accountsToSet;
            UserConfig.MAX_ACCOUNT_DEFAULT_COUNT = accountsToSet;

            PrefsHelper.INSTANCE.setMaxAccountCountWasSet(this);
        }
    }

    public static void startAppCenter(Activity context) {
        applicationLoaderInstance.startAppCenterInternal(context);
    }

    public static void checkForUpdates() {
        applicationLoaderInstance.checkForUpdatesInternal();
    }

    public static void appCenterLog(Throwable e) {
        applicationLoaderInstance.appCenterLogInternal(e);
    }

    protected void appCenterLogInternal(Throwable e) {

    }

    protected void checkForUpdatesInternal() {

    }

    protected void startAppCenterInternal(Activity context) {

    }

    public static void logDualCamera(boolean success, boolean vendor) {
        applicationLoaderInstance.logDualCameraInternal(success, vendor);
    }

    protected void logDualCameraInternal(boolean success, boolean vendor) {

    }

    public boolean checkApkInstallPermissions(final Context context) {
        return false;
    }

    public boolean openApkInstall(Activity activity, TLRPC.Document document) {
        return false;
    }

    public boolean showUpdateAppPopup(Context context, TLRPC.TL_help_appUpdate update, int account) {
        return false;
    }

    public IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenu, ViewGroup sideMenuContainer) {
        return null;
    }

    public TLRPC.Update parseTLUpdate(int constructor) {
        return null;
    }

    public void processUpdate(int currentAccount, TLRPC.Update update) {

    }

    public boolean onSuggestionFill(String suggestion, CharSequence[] output, boolean[] closeable) {
        return false;
    }

    public boolean onSuggestionClick(String suggestion) {
        return false;
    }

    public boolean extendDrawer(ArrayList<DrawerLayoutAdapter.Item> items) {
        return false;
    }

    public boolean checkRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        return false;
    }

    public boolean consumePush(int account, JSONObject json) {
        return false;
    }

    public void onResume() {

    }

    public boolean onPause() {
        return false;
    }

    public BaseFragment openSettings(int n) {
        return null;
    }

}
