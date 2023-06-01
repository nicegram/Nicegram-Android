package com.appvillis.nicegram.di

import android.content.Context
import android.content.SharedPreferences
import com.appvillis.nicegram.data.*
import com.appvillis.nicegram.domain.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NicegramFeaturesModule {
    private const val PREFS_NAME = "NicegramFeaturesModulePrefs"

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

    @Provides
    @Singleton
    fun provideRemoteConfigRepo(): RemoteConfigRepo = RemoteConfigRepoImpl()


    @Provides
    @Singleton
    fun provideCollectGroupInfoUseCase(remoteConfigRepo: RemoteConfigRepo, groupCollectRepo: GroupCollectRepo) = CollectGroupInfoUseCase(remoteConfigRepo, groupCollectRepo)

    @Provides
    @Singleton
    fun provideGroupCollectRepo(@Named(PREFS_NAME) prefs: SharedPreferences): GroupCollectRepo = GroupCollectRepoImpl(prefs)

    @Provides
    @Singleton
    fun provideNicegramSessionCounter(@Named(PREFS_NAME) prefs: SharedPreferences): NicegramSessionCounter = NicegramSessionCounterImpl(prefs)
}
