package app.nicegram

import com.appvillis.nicegram.NicegramScopes.ioScope
import com.appvillis.rep_user.domain.ClaimDailyRewardUseCase
import kotlinx.coroutines.launch

object DailyRewardsHelper {
    var useCase: ClaimDailyRewardUseCase? = null

    fun tryClaim() {
        ioScope.launch {
            useCase?.invoke()
        }
    }
}