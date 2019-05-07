package com.example.readittome

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.Handler
import android.provider.OpenableColumns
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.view.KeyEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.Toast
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val intentFilter = IntentFilter()
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private var doubleBackToExitPressedOnce: Boolean? = false
    private var audio: AudioManager? = null
    private var tts: TTS? = null
    private var languageList: ArrayList<TTS> = ArrayList()
    private var list: ArrayList<TextObj> = ArrayList()
    private var langCode: String = ""
    private var currentSentence: Int = 0
    private var currentTitle: String = ""
    private var playing: Boolean = false
    private var speed: Float = 1.0F
    private var size = 0
    private var timeSinceLastSensorEvent: Long = 0L
    private val shakeThreshold: Float = 5F  // Adjust this value to fine tune the sensitivity of shakes, lower = more sensitive, higher = less sensitive
    private val READ_REQUEST_CODE: Int = 42
    private val WRITE_REQUEST_CODE: Int = 43
    private val mProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) {}
        override fun onError(utteranceId: String) {}
        override fun onDone(utteranceId: String) {
            //TODO("Play button getting stuck after completion")
            /*
            if(utteranceId.toInt() == (size - 1)){
                pauseImageButton.visibility = INVISIBLE
                playImageButton.visibility = VISIBLE
                playing = false
            }
            */
        }
        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            super.onStop(utteranceId, interrupted)
            if(interrupted){currentSentence = utteranceId!!.toInt()}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val tts_english = TTS(this, "en", "GB", "English")
        languageList.add(tts_english)
        addOtherLanguages()
        tts = tts_english
        langCode = "en"
        languageButton.text = tts!!.lang
        tts!!.setListener(mProgressListener)
        playImageButton.visibility = VISIBLE
        pauseImageButton.visibility = INVISIBLE

//        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
//        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        shakeToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if(isChecked) {
                val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                sensorManager.unregisterListener(this, accelerometer)
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        playImageButton.setOnClickListener{
            if(!playing) {play()}
        }
        //TODO("Improve skip functionality")
        skipBackImageButton.setOnClickListener{
            if(playing){
                pause()
                currentSentence -= 1
                play()
            }
            else{currentSentence -= 1}
        }

        skipForwardImageButton.setOnClickListener{
            if(playing){
                pause()
                currentSentence += 1
                play()
            }
            else{currentSentence += 1}
        }

        pauseImageButton.setOnClickListener{
            if(playing) {pause()}
        }

        clearImageButton.setOnClickListener{
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Are you sure you want to clear the text?")
            builder.setPositiveButton("Yes") { dialog, which ->
                if(playing){pause()}
                textToSay.text.clear()
                Toast.makeText(this,"Text cleared", Toast.LENGTH_SHORT).show()
            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
            true
        }

        fastForwardImageButton.setOnClickListener{
            if(playing) {
                pause()
                if (speed >= 2.0F) {
                    Toast.makeText(this, "Already at maximum speed", Toast.LENGTH_SHORT).show()
                } else {
                    speed += 0.2F
                    tts!!.fastForward(speed)
                }
                play()
            }
            else{
                if (speed >= 2.0F) {
                    Toast.makeText(this, "Already at maximum speed", Toast.LENGTH_SHORT).show()
                } else {
                    speed += 0.2F
                    tts!!.fastForward(speed)
                }
            }
        }

        rewindImageButton.setOnClickListener{
            if(playing) {
                pause()
                if (speed <= 0.2F) {
                    Toast.makeText(this, "Already at minimum speed", Toast.LENGTH_SHORT).show()
                } else {
                    speed -= 0.2F
                    tts!!.slowDown(speed)
                }
                play()
            }
            else{
                if (speed <= 0.2F) {
                    Toast.makeText(this, "Already at minimum speed", Toast.LENGTH_SHORT).show()
                } else {
                    speed -= 0.2F
                    tts!!.slowDown(speed)
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
                val title = input.text.toString()
                val newText: TextObj = TextObj()
                var exists = false
                newText.title = title
                newText.text = textToSay.text.toString()
                newText.currentSentance = currentSentence
                newText.tts = tts
                for(i in 0 until list.size){
                    if(list.get(i).title.equals(title)){
                        exists = true
                        list.removeAt(i)
                        list.add(i, newText)
                    }
                }
                if(!exists){list.add(newText)}

                Toast.makeText(this,title + " saved", Toast.LENGTH_SHORT).show()
            }

            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            builder.show()
            true
        }

        loadButton.setOnClickListener{
            val textArray = listToCharSeq(list)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Text")
            builder.setItems(textArray, DialogInterface.OnClickListener { dialog, i  ->

                val selected = list[i]
                textToSay.text.clear()
                textToSay.text.insert(0,selected.text)
                currentTitle = selected.title
                currentSentence = selected.currentSentance
                tts = selected.tts
                languageButton.text = tts!!.lang

                dialog.dismiss()
            })
            builder.show()
            true
        }

        shakeToggleButton.setOnClickListener{

        }

        importButton.setOnClickListener{
            performFileSearch()
        }

        voiceButton.setOnClickListener{
            val setOfAvailableVoices = tts!!.getVoices()
            val availableVoices = setOfAvailableVoices!!.toTypedArray()
            val voiceSubSet: ArrayList<Voice> = ArrayList<Voice>()
            for (i in 0 until availableVoices.size) {
                if(availableVoices[i].locale.toString().contains(langCode)) {
                    voiceSubSet.add(availableVoices[i])
                }
            }
            Collections.sort(voiceSubSet, VoiceComparator());
            val voiceArray: Array<CharSequence?>  = voiceListToCharSeq(voiceSubSet)
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Voice")
            builder.setItems(voiceArray, DialogInterface.OnClickListener { dialog, i  ->
                val connectionManager: ConnectionManager = ConnectionManager(this)
                if(voiceArray[i]!!.toString().contains("Network") && !connectionManager.checkConnection())
                    {Toast.makeText(this, getString(R.string.networkRequired), Toast.LENGTH_SHORT).show()}
                else { tts!!.setVoice(voiceSubSet[i]) }
                dialog.dismiss()
            })
            builder.show()
            true
        }

        languageButton.setOnClickListener{
            val langArray: Array<CharSequence?>  = languageListToCharSeq(languageList)

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Select Language")
            builder.setItems(langArray, DialogInterface.OnClickListener { dialog, i  ->

                tts = languageList[i]
                langCode = setLangCode(tts!!)
                languageButton.text = tts!!.lang

                dialog.dismiss()
            })
            builder.show()
            true
        }
    }

    private fun setLangCode(tts: TTS): String {
        return when {
            tts.lang == "English" -> "en"
            tts.lang == "French" -> "fr"
            tts.lang == "Spanish" -> "es"
            tts.lang == "German" -> "de"
            tts.lang == "Russian" -> "ru"
            tts.lang == "Italian" -> "it"
            tts.lang == "Chinese" -> "zh"
            tts.lang == "Japanese" -> "ja"
            else -> "en"
        }
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
            when {
                voiceList[i].name.toLowerCase().contains("gb") -> { myArray[i] = "Great Britain " + gbCount; gbCount++; }
                voiceList[i].name.toLowerCase().contains("-us-") -> { myArray[i] = "United States " + usCount; usCount++; }
                voiceList[i].name.toLowerCase().contains("-au-") -> { myArray[i] = "Australia " + auCount; auCount++; }
                voiceList[i].name.toLowerCase().contains("-in-") -> { myArray[i] = "India " + inCount; inCount++; }
                voiceList[i].name.toLowerCase().contains("-ca-") -> { myArray[i] = "Canada " + caCount; caCount++; }
                voiceList[i].name.toLowerCase().contains("-fr-") -> { myArray[i] = "France " + frCount; frCount++; }
                voiceList[i].name.toLowerCase().contains("-es-") -> { myArray[i] = "Mexico " + esCount; esCount++; }
                voiceList[i].name.toLowerCase().contains("-de-") -> { myArray[i] = "Germany " + deCount; deCount++; }
                voiceList[i].name.toLowerCase().contains("-ru-") -> { myArray[i] = "Russia " + ruCount; ruCount++; }
                voiceList[i].name.toLowerCase().contains("-it-") -> { myArray[i] = "Italy " + itCount; itCount++; }
                voiceList[i].name.toLowerCase().contains("-cn-") -> { myArray[i] = "China " + cnCount; cnCount++; }
                voiceList[i].name.toLowerCase().contains("-tw-") -> { myArray[i] = "Taiwan " + twCount; twCount++; }
                voiceList[i].name.toLowerCase().contains("-jp-") -> { myArray[i] = "Japan " + jpCount; jpCount++; }
                else -> {
                    myArray[i] = voiceList[i].name.toString()
                    found = false
                }
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
        size = textList.size
        if(currentSentence < 0 || currentSentence >= size){currentSentence = 0}
        for (i in currentSentence until textList.size){
            tts!!.speak(textList.get(i), i.toString())
        }
    }

    private fun pause(){
        pauseImageButton.visibility = INVISIBLE
        playImageButton.visibility = VISIBLE
        playing = false
        tts!!.stop()
    }

    private fun playOrPause(){
        if(!playing) {play()}
        else{pause()}
    }

    public override fun onDestroy() {
        tts!!.destroy()
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
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if(currentTime - timeSinceLastSensorEvent > 1000){
                val accelerometerX = event.values[0].toDouble()
                val accelerometerY = event.values[1].toDouble()
                val accelerometerZ = event.values[2].toDouble()
                val acceleration = Math.sqrt(Math.pow(accelerometerX, 2.0) +
                                                    Math.pow(accelerometerY, 2.0) +
                                                    Math.pow(accelerometerZ, 2.0)) -
                                                    SensorManager.GRAVITY_EARTH
                if(acceleration > shakeThreshold){
                    timeSinceLastSensorEvent = currentTime
                    playOrPause()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    fun performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as a
            // file (as opposed to a list of contacts or timezones)
            addCategory(Intent.CATEGORY_OPENABLE)

            // Filter to show only images, using the image MIME data type.
            // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
            // To search for all documents available via installed storage providers,
            // it would be "*/*".
            type = "application/*"
        }

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            resultData?.data?.also { uri ->
                //Log.i(TAG, "Uri: $uri")
                textToSay.text.clear()
                textToSay.text.insert(0,readTextFromUri(uri))
            }
        }
    }

    fun dumpImageMetaData(uri: Uri) {

        // The query, since it only applies to a single document, will only return
        // one row. There's no need to filter, sort, or select fields, since we want
        // all fields for one document.
        val cursor: Cursor? = contentResolver.query( uri, null, null, null, null, null)

        cursor?.use {
            // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
            // "if there's anything to look at, look at it" conditionals.
            if (it.moveToFirst()) {

                // Note it's called "Display Name".  This is
                // provider-specific, and might not necessarily be the file name.
                val displayName: String =
                    it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                //Log.i(TAG, "Display Name: $displayName")

                val sizeIndex: Int = it.getColumnIndex(OpenableColumns.SIZE)
                // If the size is unknown, the value stored is null.  But since an
                // int can't be null in Java, the behavior is implementation-specific,
                // which is just a fancy term for "unpredictable".  So as
                // a rule, check if it's null before assigning to an int.  This will
                // happen often:  The storage API allows for remote files, whose
                // size might not be locally known.
                val size: String = if (!it.isNull(sizeIndex)) {
                    // Technically the column stores an int, but cursor.getString()
                    // will do the conversion automatically.
                    it.getString(sizeIndex)
                } else {
                    "Unknown"
                }
                //Log.i(TAG, "Size: $size")
            }
        }
    }

    private fun createFile(mimeType: String, fileName: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            // Filter to only show results that can be "opened", such as
            // a file (as opposed to a list of contacts or timezones).
            addCategory(Intent.CATEGORY_OPENABLE)

            // Create a file with the requested MIME type.
            type = mimeType
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        /*
         * Get the file's content URI from the incoming Intent, then
         * get the file's MIME type
         */
        val mimeType: String? = contentResolver.getType(uri)
        var returnText: String = ""

        if(mimeType!!.contains("pdf")){
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                returnText = pdfToText(inputStream)
            }
        }
        else{
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                }
            }
            returnText = stringBuilder.toString()
        }
        return returnText
    }

    @Throws(IOException::class)
    private fun readText(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun pdfToText(pdfPath: InputStream):String{
        var returnText: String = ""
        try {
            var parsedText: String = ""
            val reader: PdfReader = PdfReader(pdfPath)
            var n: Int = reader.getNumberOfPages()
            for (i in 0 until n) {
                parsedText   = parsedText+ PdfTextExtractor.getTextFromPage(reader, i+1).trim()+" " //Extracting the content from the different pages
            }
            returnText = parsedText
            reader.close()
        } catch (e: Exception) {
            System.out.println(e)
        }
        return returnText
    }

    private fun removeEndOfLineChars(text: String):String{
        val eol: Char = 0x0A.toChar()
        while(text.contains(eol)) {
            text.replace(eol, ' ')
        }
        return text
    }
}