-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep *; }

-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
-keep class org.telegram.messenger.* { *; }
-keep class org.telegram.messenger.camera.* { *; }
-keep class org.telegram.messenger.secretmedia.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.support.* { *; }
-keep class org.telegram.messenger.time.* { *; }
-keep class org.telegram.messenger.video.* { *; }
-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.SQLite.** { *; }
-keep class org.telegram.tgnet.ConnectionsManager { *; }
-keep class org.telegram.tgnet.NativeByteBuffer { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegate { *; }
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

# https://developers.google.com/ml-kit/known-issues#android_issues
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }

# Huawei Services
-keep class com.huawei.hianalytics.**{ *; }
-keep class com.huawei.updatesdk.**{ *; }
-keep class com.huawei.hms.**{ *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

-keep class io.nano.tex.** {*;}

-keep class org.telegram.tgnet.** { *; }

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# Use -keep to explicitly keep any other classes shrinking would remove
#-dontoptimize
#-dontobfuscate

-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.fragment.app.Fragment{}

-keep,allowoptimization class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(...);
}

-keep class com.blongho.** {*;}
-keep interface com.blongho.**
# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keeppackagenames com.blongho.country_data
-keepclassmembers class com.blongho.country_data.* {
   public *;
}
-keep class com.blongho.country_data.R$*{
    *;
}

# R8 obfuscation was enabled in 1.60.0 (NCG-13132). Gson binds JSON keys to *field names*, so a
# renamed field silently breaks both Firebase remote-config parsing and anything persisted to
# prefs. The hand-written per-class keep list below already existed and still missed 15 models,
# so field names are kept across our own packages instead. Classes and methods stay obfuscated.
-keepclassmembers class com.appvillis.** {
    <fields>;
}

-keep class androidx.recyclerview.** { *; }
-keep class com.appvillis.core_network.data.** { *; }
-keep class com.appvillis.feature_nicegram_assistant.data.SpecialOffersRepositoryImpl$SpecialOfferJson { *; }
-keep class com.appvillis.feature_nicegram_client.data.NgClientRemoteConfigRepoImpl$DialogsListBannerJson { *; }
-keep class com.appvillis.feature_nicegram_client.data.NgClientRemoteConfigRepoImpl$DialogsListBannerJson$DialogsListBannerLocaleJson { *; }
-keep class com.appvillis.feature_nicegram_client.data.NgClientRemoteConfigRepoImpl$ReferralDrawJson { *; }
-keep class com.appvillis.feature_nicegram_client.data.NgClientRemoteConfigRepoImpl$ReferralDrawJson$ReferralDrawLocaleJson { *; }
-keep class com.appvillis.feature_nicegram_client.data.NgClientRemoteConfigRepoImpl$SharingConfigJson { *; }
-keep class com.appvillis.feature_nicegram_client.data.NgClientRemoteConfigRepoImpl$SharingPopupJson { *; }
-keep class com.appvillis.feature_nicegram_client.data.NgClientRemoteConfigRepoImpl$OnboardingConfigJson { *; }
-keep class com.appvillis.feature_attention_economy.data.AttConfigRepositoryImpl { *; }
-keep class com.appvillis.feature_attention_economy.data.AttConfigRepositoryImpl$* { *; }
-keep class com.appvillis.feature_ai_chat_analysis.data.AiChatAnalysisRemoteRepoImpl { *; }
-keep class com.appvillis.feature_ai_chat_analysis.data.AiChatAnalysisRemoteRepoImpl$* { *; }
-keep class com.appvillis.feature_ai_chat_analysis.domain.entities.SourceData$* { *; }
-keep class com.appvillis.feature_ai_chat_analysis.domain.entities.SourceData { *; }

-keep class com.appvillis.feature_ai_chat.data.** { *; }

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
 -keep,allowobfuscation,allowshrinking interface retrofit2.Call
 -keep,allowobfuscation,allowshrinking class retrofit2.Response

 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
 -keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable
-keepattributes InnerClasses
-keep class org.json.** { *; }

-keep class com.ecommpay.** { *; }

-keep class com.appvillis.nicegram.network.request.** { *; }
-keep class com.appvillis.nicegram.network.response.** { *; }

-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes AnnotationDefault,RuntimeVisibleAnnotations


-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

#Tapjoy
-keep class com.tapjoy.** { *; }
-keep class com.moat.** { *; }
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-keep class * extends java.util.ListResourceBundle {
protected Object[][] getContents();
}
-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
public static final *** NULL;
}
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
@com.google.android.gms.common.annotation.KeepName *;
}
-keepnames class * implements android.os.Parcelable {
public static final ** CREATOR;
}
-keep class com.google.android.gms.ads.identifier.** { *; }
-dontwarn com.tapjoy.**
# Tapjoy end

#web3j
-dontwarn java8.util.**
-dontwarn jnr.posix.**
-dontwarn com.kenai.**
-keep class org.bouncycastle.**
-dontwarn org.bouncycastle.jce.provider.X509LDAPCertStoreSpi
-dontwarn org.bouncycastle.x509.util.LDAPStoreHelper
-keepclassmembers class org.web3j.protocol.** { *; }
-keepclassmembers class org.web3j.crypto.* { *; }
-keep class * extends org.web3j.abi.TypeReference
-keep class * extends org.web3j.abi.datatypes.Type
-dontwarn java.lang.SafeVarargs
-dontwarn org.slf4j.**
#web3j end

#wallet
-keep class com.appvillis.nicegram_wallet.wallet_walletconnect.data.WcWalletDelegate** { *;}
-keep class com.appvillis.nicegram_wallet.wallet_web3auth.domain.JwtDecoder** { *;}
-keep class com.appvillis.nicegram_wallet.wallet_storage.data.WalletRepositoryImpl$* { *; }
-keep class com.appvillis.nicegram_wallet.wallet_dapps.data.DAppsRepositoryImpl$* { *; }
-keep class com.appvillis.nicegram_wallet.wallet_dapps.data.DAppsPermissionsStorageImpl$SavedPermission { *; }
-keep class com.appvillis.nicegram_wallet.wallet_web3auth.data.DeviceShareStorageImpl$Share { *; }
-keep class com.appvillis.nicegram_wallet.wallet_dapps_storage.data.FavoriteDAppsRepositoryImpl$FavoriteDApp { *; }
-keep class com.appvillis.nicegram_wallet.wallet_remote_cofig.data.WalletRemoteRepoImpl { *; }
-keep class com.appvillis.nicegram_wallet.wallet_remote_cofig.data.WalletRemoteRepoImpl$* { *; }
-keep class com.appvillis.feature_gods_eye.data.GodsEyeConfigRepositoryImpl { *; }
-keep class com.appvillis.feature_gods_eye.data.GodsEyeConfigRepositoryImpl$* { *; }
-keep class com.appvillis.feature_ai_shortcuts.data.SettingsRepositoryImpl { *; }
-keep class com.appvillis.feature_ai_shortcuts.data.SettingsRepositoryImpl$* { *; }
-keep class com.appvillis.core_network.NicegramEvmSwapApi$* { *; }
-keep class com.appvillis.core_network.NicegramTonSwapApi$* { *; }
-keep class com.appvillis.core_network.ApiService$* { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcController { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcController$* { *; }
-keep class com.appvillis.nicegram_wallet.wallet_external_ton.domain.TcConnectionsManager$TcConnection { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TonDAppManifest { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcLink { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcLink$TcR { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.domain.TcLink$TcR$Item { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.data.TonHelperImpl$SendTransactionPayloadJson { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.data.TonHelperImpl$SendTransactionPayloadJson$Message { *; }
-keep class com.appvillis.nicegram_wallet.wallet_tonconnect.data.TcSseManagerImpl$SseMessageJson { *; }

-keep class com.appvillis.feature_attention_economy.domain.entities.** { *; }
-keep class com.appvillis.nicegram_wallet.wallet_swap.data.SwapNetworkServiceImpl.** { *; }
-keep class com.appvillis.nicegram_wallet.wallet_swap.data.SwapNetworkServiceImpl$* { *; }
-keep class com.appvillis.nicegram_wallet.wallet_swap.data.SwapEthNetworkServiceImpl.** { *; }
-keep class com.appvillis.nicegram_wallet.wallet_swap.data.SwapEthNetworkServiceImpl$* { *; }
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-dontwarn java.awt.Component
-dontwarn java.awt.GraphicsEnvironment
-dontwarn java.awt.HeadlessException
-dontwarn java.awt.Window
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
#wallet end

#web3auth
-keep class com.web3auth.singlefactorauth.types.* { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.web3auth.singlefactorauth.* {*;}
-keep class com.web3auth.singlefactorauth.** {*;}
-keep class org.web3j.abi.datatypes.** {*;}

-keepclassmembers class com.web3auth.singlefactorauth.**
-keep class org.bouncycastle.jcajce.provider.** { *; }
-keep class org.bouncycastle.jce.provider.** { *; }
-dontwarn javax.annotation.**
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

-keep class org.web3j.abi.datatypes.Function { *; }
-keep class org.web3j.abi.TypeReference { *; }
-keepclassmembers class org.web3j.abi.datatypes.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

-keep class org.torusresearch.* {*;}
-keep class org.torusresearch.** {*;}
-keep class com.web3auth.tkey.** {*;}
-keep class com.web3auth.tkey.* {*;}
-keep class com.web3auth.tkey.ThresholdKey.StorageLayer { *; }
#web3auth end

#trustwallet core
-keep class wallet.core.jni.** {*;}
-keep class wallet.core.jni.* {*;}

#tg fix
-keep class org.telegram.ui.Components.RLottieDrawable$LottieMetadata { *; }
-keep class org.telegram.ui.Stars.StarsIntroActivity$** { *; }

#missing_ryles.txt
-dontwarn java.lang.invoke.StringConcatFactory
-keep class app.nicegram.bridge.* { *; }
-keep class app.nicegram.bridge.** { *; }
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient

-keep class androidx.recyclerview.widget.PagerSnapHelper { *; }
-dontwarn org.telegram.ui.LaunchActivity_GeneratedInjector
-dontwarn org.telegram.ui.BasePermissionsActivity_GeneratedInjector
-keep class org.telegram.ui.LaunchActivity_GeneratedInjector
-keep class org.telegram.ui.BasePermissionsActivity_GeneratedInjector

-keep,allowobfuscation @dagger.hilt.android.EarlyEntryPoint class *
-keep,allowobfuscation @dagger.hilt.android.EntryPoint class *
-keep,allowobfuscation @dagger.hilt.android.AndroidEntryPoint class *

-keep,allowobfuscation class * extends dagger.hilt.internal.GeneratedComponent
-keep,allowobfuscation class * extends dagger.hilt.internal.GeneratedEntryPoint

-ignorewarnings
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class com.huawei.hianalytics.**{*;}
-keep class com.huawei.updatesdk.**{*;}
-keep class com.huawei.hms.**{*;}

-keep class com.appvillis.nicegram_wallet.wallet_swap.data.SwapNetworkServiceImpl.** { *; }
-keep class app.nicegram.HuaweiBillingManagerImpl$* { *; }