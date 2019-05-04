package com.example.readittome

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.KeyEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.Toast
import com.example.readittome.R.drawable.ic_pause
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val intentFilter = IntentFilter()
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private var doubleBackToExitPressedOnce: Boolean? = false
    private var audio: AudioManager? = null
    private var tts_language: TTS? = null
    private var languageList: ArrayList<TTS> = ArrayList()
    private var list: ArrayList<TextObj> = ArrayList()
    private var langCode: String = ""
    private var currentSentance: Int = 0
    private var currentTitle: String = ""
    private var playing: Boolean = false
    private var speed: Float = 1.0F
    private val mProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}
        override fun onError(utteranceId: String) {}
        override fun onDone(utteranceId: String) {}
        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            super.onStop(utteranceId, interrupted)
            if(interrupted){currentSentance = utteranceId!!.toInt()}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val tts_english = TTS(this, "en", "GB", "English")
        languageList.add(tts_english)
        addOtherLanguages()
        tts_language = tts_english
        langCode = "en"
        languageButton.text = tts_language!!.lang
        tts_language!!.setListener(mProgressListener)
        playImageButton.visibility = VISIBLE
        pauseImageButton.visibility = INVISIBLE

        playImageButton.setOnClickListener{
            if(!playing) {play()}
        }
        //TODO("Improve skip functionality")
        skipBackImageButton.setOnClickListener{
            if(playing){
                pause()
                currentSentance -= 1
                play()
            }
            else{currentSentance -= 1}
        }

        skipForwardImageButton.setOnClickListener{
            if(playing){
                pause()
                currentSentance += 1
                play()
            }
            else{currentSentance += 1}
        }

        pauseImageButton.setOnClickListener{
            if(playing) {pause()}
        }

        clearImageButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Are you sure you want to clear the text?")
            builder.setPositiveButton("Yes") { dialog, which ->
                textToSay.text.clear()
                Toast.makeText(this,"Text cleared", Toast.LENGTH_LONG).show()
            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
            true
        }

        fastForwardImageButton.setOnClickListener{
            if(playing) {
                pause()
                if (speed >= 2.0F) {
                    Toast.makeText(this, "Already at maximum speed", Toast.LENGTH_LONG).show()
                } else {
                    speed += 0.2F
                    tts_language!!.fastForward(speed)
                }
                play()
            }
            else{
                if (speed >= 2.0F) {
                    Toast.makeText(this, "Already at maximum speed", Toast.LENGTH_LONG).show()
                } else {
                    speed += 0.2F
                    tts_language!!.fastForward(speed)
                }
            }
        }

        rewindImageButton.setOnClickListener{
            if(playing) {
                pause()
                if (speed <= 0.2F) {
                    Toast.makeText(this, "Already at minimum speed", Toast.LENGTH_LONG).show()
                } else {
                    speed -= 0.2F
                    tts_language!!.slowDown(speed)
                }
                play()
            }
            else{
                if (speed <= 0.2F) {
                    Toast.makeText(this, "Already at minimum speed", Toast.LENGTH_LONG).show()
                } else {
                    speed -= 0.2F
                    tts_language!!.slowDown(speed)
                }
            }
        }

        saveButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Save Text")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            if(currentTitle.isNotEmpty()){input.text.insert(0, currentTitle)}
            builder.setView(input)

            builder.setPositiveButton("Save") { dialog, which ->
                var title = input.text.toString()
                var newText: TextObj = TextObj()
                newText.title = title
                newText.text = textToSay.text.toString()
                newText.currentSentance = currentSentance

                list.add(newText)

                Toast.makeText(this,title + " saved", Toast.LENGTH_LONG).show()
            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
            true
        }

        loadButton.setOnClickListener{
            var textArray = listToCharSeq(list)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Text")
            builder.setItems(textArray, DialogInterface.OnClickListener { dialog, i  ->

                val selected = list[i]
                textToSay.text.clear()
                textToSay.text.insert(0,selected.text)
                currentSentance = selected.currentSentance

                dialog.dismiss()
            })
            builder.show()
            true
        }

        sendButton.setOnClickListener{

        }

        recieveButton.setOnClickListener{

        }

        voiceButton.setOnClickListener{
            var setOfAvailableVoices = tts_language!!.getVoices()
            var availableVoices = setOfAvailableVoices!!.toTypedArray()
            var voiceSubSet: ArrayList<Voice> = ArrayList<Voice>()
            for (i in 0 until availableVoices.size) {
                if(availableVoices[i].locale.toString().contains(langCode)) {
                    voiceSubSet.add(availableVoices[i])
                }
            }
            Collections.sort(voiceSubSet, VoiceComparator());
            var voiceArray: Array<CharSequence?>  = voiceListToCharSeq(voiceSubSet)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Voice")
            builder.setItems(voiceArray, DialogInterface.OnClickListener { dialog, i  ->
                val connectionManager: ConnectionManager = ConnectionManager(this)
                if(voiceArray[i]!!.toString().contains("Network") && !connectionManager.checkConnection())
                    {Toast.makeText(this, getString(R.string.networkRequired), Toast.LENGTH_SHORT).show()}
                else { tts_language!!.setVoice(voiceSubSet[i]) }
                dialog.dismiss()
            })
            builder.show()
            true
        }

        languageButton.setOnClickListener{
            var langArray: Array<CharSequence?>  = languageListToCharSeq(languageList)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Language")
            builder.setItems(langArray, DialogInterface.OnClickListener { dialog, i  ->

                tts_language = languageList[i]
                langCode = setLangCode(tts_language!!)
                languageButton.text = tts_language!!.lang

                dialog.dismiss()
            })
            builder.show()
            true
        }
    }

    private fun setLangCode(tts: TTS): String {
        if(tts.lang.equals("English")){return "en"}
        else if(tts.lang.equals("French")){return "fr"}
        else if(tts.lang.equals("Spanish")){return "es"}
        else if(tts.lang.equals("German")){return "de"}
        else if(tts.lang.equals("Russian")){return "ru"}
        else if(tts.lang.equals("Italian")){return "it"}
        else if(tts.lang.equals("Chinese")){return "zh"}
        else if(tts.lang.equals("Japanese")){return "ja"}
        else{return "en"}
    }

    private fun addOtherLanguages(){
        val tts_french = TTS(this, "fr", "FR", "French")
        languageList.add(tts_french)
        val tts_spanish = TTS(this, "es", "ES", "Spanish")
        languageList.add(tts_spanish)
        val tts_german = TTS(this, "de", "DE", "German")
        languageList.add(tts_german)
        val tts_russian = TTS(this, "ru", "RU", "Russian")
        languageList.add(tts_russian)
        val tts_italian = TTS(this, "it", "IT", "Italian")
        languageList.add(tts_italian)
        val tts_chinese = TTS(this, "zh", "CN", "Chinese")
        languageList.add(tts_chinese)
        val tts_japanese = TTS(this, "ja", "JP", "Japanese")
        languageList.add(tts_japanese)
    }

    private fun languageListToCharSeq(languageList: ArrayList<TTS>): Array<CharSequence?> {
        val myArray = arrayOfNulls<CharSequence>(languageList.size)
        for (i in 0 until languageList.size) {
            myArray[i] = languageList[i].lang
        }
        return myArray
    }

    private fun voiceListToCharSeq(voiceList: ArrayList<Voice>): Array<CharSequence?> {
        val myArray = arrayOfNulls<CharSequence>(voiceList.size)
        var found = true
        var gbCount: Int = 1; var usCount: Int = 1; var auCount: Int = 1; var inCount: Int = 1;
        var frCount: Int = 1; var caCount: Int = 1; var esCount: Int = 1; var deCount: Int = 1;
        var ruCount: Int = 1; var itCount: Int = 1; var cnCount: Int = 1; var jpCount: Int = 1;
        var twCount: Int = 1;
        for (i in 0 until voiceList.size) {
            if(voiceList[i].name.toLowerCase().toLowerCase().contains("gb")){ myArray[i] = "Great Britain " + gbCount; gbCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-us-")){ myArray[i] = "United States " + usCount; usCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-au-")){ myArray[i] = "Australia " + auCount; auCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-in-")){ myArray[i] = "India " + inCount; inCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-ca-")){ myArray[i] = "Canada " + caCount; caCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-fr-")){ myArray[i] = "France " + frCount; frCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-es-")){ myArray[i] = "Mexico " + esCount; esCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-de-")){ myArray[i] = "Germany " + deCount; deCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-ru-")){ myArray[i] = "Russia " + ruCount; ruCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-it-")){ myArray[i] = "Italy " + itCount; itCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-cn-")){ myArray[i] = "China " + cnCount; cnCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-tw-")){ myArray[i] = "Taiwan " + twCount; twCount++; }
            else if(voiceList[i].name.toLowerCase().contains("-jp-")){ myArray[i] = "Japan " + jpCount; jpCount++; }
            else{
                myArray[i] = voiceList[i].name.toString()
                found = false
            }
            if(found){
                if(voiceList[i].name.toLowerCase().contains("female")){ myArray[i] = myArray[i].toString() + ", Female" }
                else if(voiceList[i].name.toLowerCase().contains("male")){ myArray[i] = myArray[i].toString() + ", Male" }
                if(voiceList[i].name.toLowerCase().contains("local")){ myArray[i] = myArray[i].toString() + ", Local" }
                else if(voiceList[i].name.toLowerCase().contains("network")){ myArray[i] = myArray[i].toString() + ", Network" }
            }
        }
        return myArray
    }

    private fun listToCharSeq(list: List<TextObj>): Array<CharSequence?> {
        val myArray = arrayOfNulls<CharSequence>(list.size)
        for (i in 0 until list.size) {
            myArray[i] = list[i].title
        }
        return myArray
    }

    private fun play(){
        playing = true
        playImageButton.visibility = INVISIBLE
        pauseImageButton.visibility = VISIBLE
        val textList = textToSay.text.split('.', '?', '!')
        if(currentSentance >= textList.size || currentSentance < 0){currentSentance = 0}
        for (i in currentSentance until textList.size){
            tts_language!!.speak(textList.get(i), i.toString())
        }
    }

    private fun pause(){
        pauseImageButton.visibility = INVISIBLE
        playImageButton.visibility = VISIBLE
        playing = false
        tts_language!!.stop()
    }

    private fun playOrPause(){
        if(!playing) {play()}
        else{pause()}
    }

    public override fun onDestroy() {
        tts_language!!.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //replaces the default 'Back' button action
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (doubleBackToExitPressedOnce!!) {
                super.onBackPressed()
                return true
            }
            this.doubleBackToExitPressedOnce = true
            Toast.makeText(this, getString(R.string.doubleBackToExit), Toast.LENGTH_SHORT).show()
            Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            audio!!.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            audio!!.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        return true
    }


    override fun onSensorChanged(event: SensorEvent?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}