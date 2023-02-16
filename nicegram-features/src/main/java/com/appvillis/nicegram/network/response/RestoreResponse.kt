package com.appvillis.nicegram.network.response

class RestoreResponse(val data: RestoreJson) {
    class RestoreJson(val premiumAccess: Boolean)
}