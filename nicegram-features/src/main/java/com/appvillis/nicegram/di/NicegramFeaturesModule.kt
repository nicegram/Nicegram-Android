package com.appvillis.nicegram.di

import android.content.SharedPreferences
import com.appvillis.feature_nicegram_client.NicegramClientModule
import com.appvillis.feature_nicegram_client.domain.CommonRemoteConfigRepo
import com.appvillis.nicegram.data.*
import com.appvillis.nicegram.domain.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NicegramFeaturesModule {

    @Provides
    @Singleton
    fun provideCollectGroupInfoUseCase(remoteConfigRepo: CommonRemoteConfigRepo, groupCollectRepo: GroupCollectRepo) =
        CollectGroupInfoUseCase(remoteConfigRepo, groupCollectRepo)

    @Provides
    @Singleton
    fun provideGroupCollectRepo(@Named(NicegramClientModule.NG_CLIENT_PREFS_NAME) prefs: SharedPreferences): GroupCollectRepo =
        GroupCollectRepoImpl(prefs)
}
