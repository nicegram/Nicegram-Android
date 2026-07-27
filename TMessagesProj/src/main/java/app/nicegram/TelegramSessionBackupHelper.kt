package app.nicegram

import com.appvillis.core_domain.usecase.call
import com.appvillis.nicegram.NicegramAssistantEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.telegram.messenger.ApplicationLoader
import timber.log.Timber

object TelegramSessionBackupHelper {
    private fun entryPoint() = EntryPoints
        .get(ApplicationLoader.applicationContext, NicegramAssistantEntryPoint::class.java)

    fun showBackupIfNeeded(onShouldShow: Runnable) {
        val ep = entryPoint()
        ep.appScope().launch(CoroutineExceptionHandler { _, throwable -> Timber.e(throwable) }) {
            if (ep.isNeedToShowTelegramSessionBackupUseCase().call()) {
                withContext(ep.dispatchersProvider().main()) {
                    onShouldShow.run()
                }
            }
        }
    }
}
