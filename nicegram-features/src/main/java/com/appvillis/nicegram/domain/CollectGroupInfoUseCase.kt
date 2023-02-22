package com.appvillis.nicegram.domain

import android.os.Build
import android.util.Base64
import com.appvillis.nicegram.BuildConfig
import com.appvillis.nicegram.NicegramNetworkConsts
import com.appvillis.nicegram.NicegramScopes.ioScope
import com.appvillis.nicegram.network.NicegramNetwork
import com.appvillis.nicegram.network.request.ChannelInfoRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class CollectGroupInfoUseCase(
    private val remoteConfigRepo: RemoteConfigRepo,
    private val groupCollectRepo: GroupCollectRepo
) {
    fun collectInfo(
        groupId: Long,
        inviteLinks: List<InviteLink>,
        iconBase64: String?,
        restrictions: List<Restriction>,
        verified: Boolean,
        about: String?,
        hasGeo: Boolean,
        title: String?,
        fake: Boolean,
        scam: Boolean,
        date: Long,
        username: String?,
        gigagroup: Boolean,
        lastMessageLang: String?,
        participantsCount: Int,
        photo: Any?,
        geoLocation: Geo?
    ) {
        if (!canCollect(groupId)) {
            return
        }

        groupCollectRepo.setLastTimeMsGroupCollected(groupId, System.currentTimeMillis())

        ioScope.launch {
            try {
                val payload = Gson().toJson(Payload(verified, restrictions, about ?: "empty", hasGeo, title ?: "empty", fake, scam, date, username, gigagroup, lastMessageLang ?: "--", participantsCount, geoLocation))
                val request = ChannelInfoRequest(
                    "-100$groupId".toLong(),
                    inviteLinks.map { ChannelInfoRequest.InviteLinkRequest(it.date, it.requestApproval, it.adminId, it.isPermanent, it.isRevoked, it.link) },
                    iconBase64,
                    "channel",
                    payload
                )
                val bodyJson = Gson().toJson(request)
                val bodyBase64 = Base64.encodeToString(bodyJson.toByteArray(), Base64.NO_WRAP)
                val key = NicegramNetworkConsts.NG_CLOUD_KEY
                val stringToHash = "$bodyBase64$key"
                val hash = sha256(stringToHash)
                NicegramNetwork.ngCloudApi.collectChannelInfo(request, hash, "${Build.MANUFACTURER} ${Build.MODEL}")
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) e.printStackTrace()
            }
        }
    }

    fun canCollect(groupId: Long): Boolean {
        val throttleMs = TimeUnit.SECONDS.toMillis(remoteConfigRepo.getGroupInfoThrottleSec)
        val lastMsCollected = groupCollectRepo.getLastTimeMsGroupCollected(groupId)
        return System.currentTimeMillis() - lastMsCollected >= throttleMs
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return BigInteger(1, md.digest(input.toByteArray())).toString(16).padStart(32, '0')
    }

    class InviteLink(val date: Long, val requestApproval: Boolean, val adminId: Long, val isPermanent: Boolean, val isRevoked: Boolean, val link: String)
    class Restriction(val platform: String, val text: String, val reason: String)
    class Geo(val address: String, val longitude: Double, val latitude: Double)
    class Payload(
        val verified: Boolean,
        val restrictions: List<Restriction>,
        val about: String,
        val hasGeo: Boolean,
        val title: String,
        val fake: Boolean,
        val scam: Boolean,
        val date: Long,
        val username: String?,
        val gigagroup: Boolean,
        val lastMessageLang: String,
        val participantsCount: Int,
        val geoLocation: Geo?)
}