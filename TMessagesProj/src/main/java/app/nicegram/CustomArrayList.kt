package app.nicegram;

import android.view.View
import app.nicegram.ui.AttVH
import com.appvillis.feature_attention_economy.AttEntryPoint
import com.appvillis.feature_attention_economy.domain.entities.AttAd
import com.appvillis.feature_attention_economy.domain.entities.AttPlacement
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.MessageObject
import org.telegram.tgnet.TLRPC
import timber.log.Timber
import kotlin.math.max

class CustomArrayList : ArrayList<MessageObject>() {

    companion object {
        private const val EXTRA_SPACE = 10
    }

    var attCallback: AttCallback? = null
    private val displayedAds = mutableListOf<AttAd>()

    private val customArrayListScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var allowAdsInChat = false
    private var chatId: Long? = null

    init {
        val attEntryPoints = EntryPoints.get(ApplicationLoader.applicationContext, AttEntryPoint::class.java)
        customArrayListScope.launch {
            attEntryPoints.getGetPlacementAdsUseCase().getPlacementAdsFlow(AttPlacement.AttPlacementType.Chat).collect {
                pendingAd = it
            }
        }

        customArrayListScope.launch {
            attEntryPoints.getGetSettingsUseCase().flow().collect {
                if (!it.enableAds || it.placements.find { placement -> placement.id == AttPlacement.AttPlacementType.Chat }?.enabled == false) {
                    displayedAds.remove(pendingAd)
                    pendingAd = null
                    removeAll { msgObject -> msgObject is AttVH.AttMessageObject }
                }
            }
        }
    }

    fun setChat(currentChat: TLRPC.Chat?, chatInfo: TLRPC.ChatFull?) {
        val pplCount = max(currentChat?.participants_count ?: 0, chatInfo?.participants_count ?: 0)

        Timber.d("pplCount $pplCount")

        allowAdsInChat = pplCount >= 1000
        chatId = currentChat?.id
    }

    fun dispose() {
        customArrayListScope.coroutineContext.cancel()
    }

    private var pendingAd: AttAd? = null

    fun onScroll(
        dx: Int,
        dy: Int,
        firstVisible: Int,
        firstCompletelyVisiblePos: Int,
        lastVisible: Int,
        lastCompletelyVisiblePos: Int
    ) {
        if (!allowAdsInChat) return

        pendingAd?.let { pendingAd ->
            if (displayedAds.contains(pendingAd)) return@let

            if (dy < 0) {
                val adPosition = findNotGroupedPosition(lastVisible + EXTRA_SPACE, true)

                displayedAds.add(pendingAd)
                add(adPosition, AttVH.AttMessageObject(pendingAd, chatId))

                Timber.d("added to position $adPosition")
            } else if (dy > 0) {
                val adPosition = findNotGroupedPosition(max(0, firstVisible - EXTRA_SPACE), false)
                if (adPosition > 0) {
                    displayedAds.add(pendingAd)

                    add(adPosition, AttVH.AttMessageObject(pendingAd, chatId))

                    Timber.d("added to position $adPosition")
                }
            }
        }
    }

    private fun findNotGroupedPosition(from: Int, older: Boolean): Int {
        Timber.d("----findNotGroupedPosition older: $from $older, size: $size")
        var previousGroupId = -1L
        if (older) {
            for (i in from..<size) {
                Timber.d("findNotGroupedPosition older:$older grp:${get(i).groupId} from:$from i:$i")
                if (get(i).groupId == 0L) {
                    Timber.d("findNotGroupedPosition return $i")
                    return i
                } else {
                    if (previousGroupId != -1L && previousGroupId != get(i).groupId) {
                        Timber.d("findNotGroupedPosition new group started return $i")
                        return i
                    }

                    previousGroupId = get(i).groupId
                }
            }
        } else {
            for (i in from downTo 0) {
                Timber.d("findNotGroupedPosition older:$older grp:${get(i).groupId} from:$from i:$i")
                val groupId = get(i).groupId
                //val nextGroupId = getOrNull( i - 1)?.groupId ?: 0
                if (groupId == 0L) {
                    Timber.d("findNotGroupedPosition return $i")
                    return i
                } /*else {
                    if (groupId != nextGroupId) {
                        Timber.d("findNotGroupedPosition new group started return $i")
                        return i
                    }
                }*/
            }
        }

        return if (older) size else 0
    }

    interface AttCallback {
        fun getMessagesStartRow(): Int
        fun getMessagesEndRow(): Int
        fun getViewAtPosition(position: Int): View?
    }
}
