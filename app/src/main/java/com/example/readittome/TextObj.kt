package com.example.readittome

import android.speech.tts.TextToSpeech

class TextObj {
    public var title: String = ""
    public var text: String = ""
    public var currentSentance: Int = 0
    public var tts: TTS? = null
}