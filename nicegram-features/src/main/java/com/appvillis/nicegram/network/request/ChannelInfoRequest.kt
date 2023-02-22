package com.appvillis.nicegram.network.request

class ChannelInfoRequest(
    val id: Long,
    val inviteLinks: List<InviteLinkRequest>,
    val icon: String?,
    val type: String,
    val payload: String
) {
    class InviteLinkRequest(
        val date: Long,
        val requestApproval: Boolean,
        val adminId: Long,
        val isPermanent: Boolean,
        val isRevoked: Boolean,
        val link: String
    )
}