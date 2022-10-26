package com.appvillis.nicegram.di

import android.content.Context
import android.content.SharedPreferences
import com.appvillis.nicegram.data.BillingManagerImpl
import com.appvillis.nicegram.data.NicegramFeaturesPrefsRepositoryImpl
import com.appvillis.nicegram.domain.BillingManager
import com.appvillis.nicegram.domain.GetBillingSkusUseCase
import com.appvillis.nicegram.domain.NicegramFeaturesOnboardingUseCase
import com.appvillis.nicegram.domain.NicegramFeaturesPrefsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NicegramFeaturesModule {
    private const val PREFS_NAME = "NicegramFeaturesModulePrefs"

    @Provides
    @Singleton
    fun provideBillingManger(
        @ApplicationContext context: Context
    ): BillingManager =
        BillingManagerImpl(context, CoroutineScope(SupervisorJob() + Dispatchers.IO))

    @Provides
    @Singleton
    fun provideBillingClient(billingManager: BillingManager) = billingManager.billingClient

    @Provides
    @Singleton
    fun provideGetBillingSkusUseCase(billingManager: BillingManager) =
        GetBillingSkusUseCase(billingManager)

    @Provides
    @Singleton
    @Named(PREFS_NAME)
    fun provideNicegramFeaturesPrefs(@ApplicationContext context: Context) =
        context.getSharedPreferences(
            PREFS_NAME, Context.MODE_PRIVATE
        )

    @Provides
    @Singleton
    fun provideNicegramFeaturesPrefsRepositoryImpl(@Named(PREFS_NAME) prefs: SharedPreferences): NicegramFeaturesPrefsRepository =
        NicegramFeaturesPrefsRepositoryImpl(prefs)

    @Provides
    @Singleton
    fun provideNicegramFeaturesOnboardingUseCase(nicegramFeaturesPrefsRepository: NicegramFeaturesPrefsRepository) =
        NicegramFeaturesOnboardingUseCase(nicegramFeaturesPrefsRepository)
}