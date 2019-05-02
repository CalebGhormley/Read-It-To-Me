package com.example.readittome

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.AudioManager
import android.net.wifi.p2p.WifiP2pManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.text.InputType
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val intentFilter = IntentFilter()
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private var doubleBackToExitPressedOnce: Boolean? = false
    private var audio: AudioManager? = null
    private var tts_english: TTS? = null
    private var tts_french: TTS? = null
    private var tts_german: TTS? = null
    private var tts_spanish: TTS? = null
    public var tts_language: TTS? = null
    public var textToBeSaid: String = ""
    private var list: ArrayList<TextObj> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        if(intent != null) {
            if (intent.getStringExtra("textToBeSaid") != null && textToBeSaid!!.isNotEmpty()) {
                textToBeSaid = intent.getStringExtra("textToBeSaid")
                textToSay.text.insert(0, textToBeSaid)
            }
        }
        */
        /*
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        */

        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts_english = TTS(this, "en", "GB")
        tts_french = TTS(this, "fr", "FR")
        tts_spanish = TTS(this, "es", "ES")
        tts_german = TTS(this, "de", "DE")
        tts_language = tts_english

        playImageButton.setOnClickListener{
            tts_language!!.speak(textToSay.text.toString())
        }

        saveButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Save Text")

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)

            builder.setPositiveButton("Save") { dialog, which ->
                var title = input.text.toString()
                var newText: TextObj = TextObj()
                newText.title = title
                newText.text = textToSay.text.toString()

                list.add(newText)

                Toast.makeText(this,"current data " + title, Toast.LENGTH_LONG).show()
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

                dialog.dismiss()
            })
            builder.show()
            true
        }

        fetchButton.setOnClickListener{
            val intent = Intent(this, FetchActivity::class.java)
            intent.putExtra("textToBeSaid", textToBeSaid)
            startActivity(intent)
        }

        /*
        button_english.setOnClickListener{
            tts_english!!.speak(textToSay.text.toString())
        }
        button_french.setOnClickListener{
            tts_french!!.speak(textToSay.text.toString())
        }
        button_spanish.setOnClickListener{
            tts_spanish!!.speak(textToSay.text.toString())
        }
        button_german.setOnClickListener{
            tts_german!!.speak(textToSay.text.toString())
        }
        */
    }

    private fun listToCharSeq(list: List<TextObj>): Array<CharSequence?> {
        val myArray = arrayOfNulls<CharSequence>(list.size)
        for (i in 0 until list.size) {
            myArray[i] = list[i].title
        }
        return myArray
    }

    public override fun onDestroy() {
        tts_english!!.destroy()
        tts_french!!.destroy()
        tts_spanish!!.destroy()
        tts_german!!.destroy()
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
}