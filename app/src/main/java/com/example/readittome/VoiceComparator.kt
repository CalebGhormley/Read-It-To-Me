package com.example.readittome

import android.speech.tts.Voice
import java.util.ArrayList
import android.R.attr.name


class VoiceComparator : Comparator<Voice> {
    override fun compare(left: Voice, right: Voice): Int {
        return left.name.toLowerCase().compareTo(right.name.toLowerCase())
    }
}