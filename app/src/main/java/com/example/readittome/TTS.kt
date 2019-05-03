package com.example.readittome

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.speech.tts.Voice.LATENCY_VERY_LOW
import android.speech.tts.Voice.QUALITY_VERY_HIGH
import android.util.Log
import java.util.*

class TTS : TextToSpeech.OnInitListener {

    private val ctx: Context
    private val loc: Locale
    private val tts : TextToSpeech
    private val set: Set<String>? = null
    public var lang: String = ""

    constructor(context : Context, countryCode: String, regionCode : String, title: String) {
        ctx = context
        loc = Locale(countryCode, regionCode)
        tts = TextToSpeech(ctx, this, "com.google.android.tts")
        lang = title
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(loc)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.i("TTS", "This Language is not supported")
            } else {
                Log.i("TTS", "Initilization Success!")
            }
        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }

    fun speak(value: String) {
        tts.speak(value, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun getVoices(): MutableSet<Voice>? {
        return tts.voices
    }

    fun setVoice(voice:Voice){
        tts.voice = voice
    }
    fun destroy() {
        tts.stop()
        tts.shutdown()
    }
}