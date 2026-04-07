package app.nicegram.domain.usecases.user

import androidx.annotation.Keep
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.UserConfig
import javax.inject.Inject

@Keep
class HasSignedInUserUseCase @Inject constructor() {

    fun getFirstAccountAuthFlow(): Flow<Boolean> = callbackFlow {
        val accountIndex = 0

        val isAuthorized = (0 until UserConfig.MAX_ACCOUNT_COUNT).any { index ->
            UserConfig.getInstance(index).isClientActivated
        }
        trySend(isAuthorized)

        val observer = NotificationCenter.NotificationCenterDelegate { id, account, _ ->
            if (id == NotificationCenter.mainUserInfoChanged && account == accountIndex) {
                val newState = UserConfig.getInstance(accountIndex).isClientActivated
                trySend(newState)
            }
        }

        NotificationCenter.getInstance(accountIndex)
            .addObserver(observer, NotificationCenter.mainUserInfoChanged)

        awaitClose {
            NotificationCenter.getInstance(accountIndex)
                .removeObserver(observer, NotificationCenter.mainUserInfoChanged)
        }
    }
        .distinctUntilChanged()
}
