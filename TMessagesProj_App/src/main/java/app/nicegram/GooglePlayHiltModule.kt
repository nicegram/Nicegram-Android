package app.nicegram

import android.content.Context
import android.content.SharedPreferences
import com.appvillis.core_markets.MarketFeatureFlagsProvider
import com.appvillis.core_network.ApiService
import com.appvillis.feature_nicegram_billing.data.BillingManagerImpl
import com.appvillis.feature_nicegram_billing.domain.BillingManager
import com.appvillis.rep_user.domain.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GooglePlayHiltModule {
    @Provides
    @Singleton
    fun provideBillingManger(
        @ApplicationContext context: Context,
        apiService: ApiService,
        userRepository: UserRepository,
        sharedPreferences: SharedPreferences
    ): BillingManager =
        BillingManagerImpl(context, CoroutineScope(SupervisorJob() + Dispatchers.IO), apiService, userRepository, sharedPreferences)

    @Provides
    @Singleton
    fun provideMarketFeatureFlagsProvider(): MarketFeatureFlagsProvider = object : MarketFeatureFlagsProvider {
        override val canShowAds = true
    }
}