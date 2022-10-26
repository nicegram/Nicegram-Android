package com.appvillis.nicegram.network.response

class GoogleSpeech2TextResponse(val results: List<Results>?) {
    class Results(val alternatives: List<Alternative>?)

    class Alternative(val transcript: String?)
}