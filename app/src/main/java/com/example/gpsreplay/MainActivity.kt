package com.example.gpsreplay

import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.math.cos
import kotlin.math.sin


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val red: Int = Color.rgb(200, 0, 0)
        val green: Int = Color.rgb(0, 200, 0)
        val gpxButton: Button = findViewById<Button>(R.id.gpx_button)
        val seekBar: SeekBar = findViewById<SeekBar>(R.id.seekBar)
        val startTime: TextView = findViewById<TextView>(R.id.startTime)
        startTime.text = "0"
        val endTime: TextView = findViewById<TextView>(R.id.endTime)
        endTime.text = "0"
        val duration: TextView = findViewById<TextView>(R.id.duration)
        val dataPointTime: TextView = findViewById<TextView>(R.id.dataPointTime)
        val dataPointIndex: TextView = findViewById<TextView>(R.id.dataPointIndex)
        var index: Int = 0
        val latitude: TextView = findViewById<TextView>(R.id.latitude)
        val longitude: TextView = findViewById<TextView>(R.id.longitude)
        val altitude: TextView = findViewById<TextView>(R.id.altitude)
        val speed: TextView = findViewById<TextView>(R.id.speed)
        val playPauseButton: Button = findViewById<Button>(R.id.playPause)
        playPauseButton.setBackgroundColor(red)
        var numOfPoints: Int = 0
        var trackpoints: List<Trackpoint>? = null
        var systemTimeAtPlaystart: Long = System.currentTimeMillis()
        var deltaTime: Long = 0
        var play: Boolean = false
        val sbPlaySpeed:SeekBar = findViewById<SeekBar>(R.id.sbPlaySpeed)
        val tvPlaySpeed:TextView = findViewById<TextView>(R.id.tvPlaySpeed)
        var playSpeed:Int = 1
        val locationManager:LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        try{
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        }
        catch (e: Exception) {
            e.printStackTrace()
        }


        locationManager.addTestProvider(
            LocationManager.GPS_PROVIDER
            , false, false,
            false, false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE
        )


        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true)
        val mockLocation:Location = Location(LocationManager.GPS_PROVIDER)
        mockLocation.setElapsedRealtimeNanos(System.nanoTime())
        mockLocation.setAccuracy(5.0F)


        fun updateDatafields() {
            dataPointTime.text = Date(trackpoints!![index].epoch).toString()
            dataPointIndex.text = index.toString()
            latitude.text = trackpoints!![index].lat.toString()
            longitude.text = trackpoints!![index].lon.toString()
            altitude.text = trackpoints!![index].altitude.toString()
            speed.text = trackpoints!![index].speed.toString()
        }

        fun pause() {
            playPauseButton.text = "Paused"
            playPauseButton.setBackgroundColor(red)
            play = false
        }

        fun play() {
            playPauseButton.text = "Playing"
            playPauseButton.setBackgroundColor(green)
            play = true
        }

        fun mockGPSdata(trackpoint:Trackpoint){
            mockLocation.setLatitude(trackpoint.lat)
            mockLocation.setLongitude(trackpoint.lon)
            mockLocation.setAltitude(trackpoint.altitude)
            mockLocation.setSpeed(trackpoint.speed)
            mockLocation.setBearing(trackpoint.bearing)
            mockLocation.setTime(trackpoint.epoch+deltaTime)
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation)
        }




        //This is for reading the external file
        val getContent = ActivityResultContracts.GetContent()
        var callBack = ActivityResultCallback<Uri> {
            pause()
            val inputStream: InputStream? = this.contentResolver.openInputStream(it)
            //val inputReader: inputStream.bufferedReader().use
            //val inputAsString = inputStream?.bufferedReader().use { it?.readText() }
            //Log.d("MainActivity",inputAsString.toString())
            try {
                val parser = XmlPullParserHandler()
                val pairReturn: Pair<List<Trackpoint>, Int>
                pairReturn = parser.parse(inputStream)
                trackpoints = pairReturn.first
                val code: Int = pairReturn.second
                numOfPoints = trackpoints!!.size
                //bar?.max = trackpoints!!.size
                //min_txt?.text = 0.toString()
                when (code) {
                    0 -> {          //0 means the file was read successfully
                        val toast = Toast.makeText(
                            this@MainActivity,
                            "Read $numOfPoints points",
                            Toast.LENGTH_LONG
                        )
                        toast.setGravity(Gravity.CENTER,0,0)
                        toast.show()
                        val startDate: Date = Date(trackpoints!![0].epoch)
                        val endDate: Date = Date(trackpoints!![numOfPoints - 1].epoch)
                        startTime.text = startDate.toString()
                        endTime.text = endDate.toString()
                        dataPointTime.text = startDate.toString()
                        dataPointIndex.text = 0.toString()
                        val millis: Long = endDate!!.time - startDate!!.time
                        val hours: Int = (millis / (1000 * 60 * 60)).toInt()
                        val mins: Int = (millis / (1000 * 60) % 60).toInt()
                        val secs: Int =
                            ((millis - (hours * 3600 + mins * 60) * 1000) / 1000).toInt()
                        duration.text =
                            hours.toString() + " Hrs " + mins.toString() + " Mins " + secs.toString() + " secs"
                        index = 0
                        updateDatafields()
                    }
                    1 -> {  //1 means there was some error in the file
                        val toast = Toast.makeText(this@MainActivity, "Invalid File", Toast.LENGTH_LONG)
                        toast.setGravity(Gravity.CENTER,0,0)
                        toast.show()
                        numOfPoints = 0
                        startTime.text = 0.toString()
                        endTime.text = 0.toString()
                        dataPointTime.text = 0.toString()
                        dataPointIndex.text = 0.toString()
                        latitude.text = 0.toString()
                        longitude.text = 0.toString()
                        altitude.text = 0.toString()
                        speed.text = 0.toString()
                        duration.text = 0.toString()
                    }
                }
                seekBar.setProgress(0)
                index = 0
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        //Listener for the GPX file opener
        val getContentActivity = registerForActivityResult(getContent, callBack)
        gpxButton.setOnClickListener { getContentActivity.launch("*/*") }

        //Listener for seekbar change
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2){    //If the seekbar change was caused by screen input (instead of by code)
                    pause()     //put the system on pause
                    if (numOfPoints > 0) {
                        index = (p1 * (numOfPoints - 1) / 50).toInt()
                        updateDatafields()
                    }
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //TODO("Not yet implemented")
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //TODO("Not yet implemented")
            }
        })

        sbPlaySpeed.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                playSpeed = p1+1
                tvPlaySpeed.text = playSpeed.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {
                //TODO("Not yet implemented")
            }

            override fun onStopTrackingTouch(p0: SeekBar?) {
                //TODO("Not yet implemented")
            }
        })



        playPauseButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (play) {
                    pause()
                } else {
                    if (numOfPoints > 0) {
                        deltaTime =
                            System.currentTimeMillis() - Date(trackpoints!![index].epoch).time
                        play()
                    }
                }
            }
        })


        Thread(Runnable {
            while (true) {
                if (play && (index == numOfPoints - 1)) {
                    runOnUiThread() {
                        pause()
                    }
                }
                if (play && (numOfPoints > 0) && (index < numOfPoints - 1)) {
                    while (play && (Date(trackpoints!![index + 1].epoch).time + deltaTime > System.currentTimeMillis())) {
                    }
                    if (play) {//We need to check play again because it might have changed during the above idle loop
                        index += 1
                        runOnUiThread() {
                            updateDatafields()
                            mockGPSdata(trackpoints!![index])
                            if ((index*50.0/numOfPoints).toInt() > ((index-1)*50.0/numOfPoints).toInt()) {
                                seekBar.setProgress((index * 50.0 / numOfPoints).toInt())
                            }
                        }
                    }
                }
            }
        }).start()


//            if (Settings.Secure.getString(this.getContentResolver(),
//                    Settings.Secure.ALLOW_MOCK_LOCATION).equals("0")) {
//                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
//            }

    }

    override fun onDestroy() {
        val locationManager:LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (LocationManager.GPS_PROVIDER != null) {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
        }
        Log.d("TEST","On Destroy Started")
        super.onDestroy()

    }

    override fun onStop(){
        Log.d("TEST","On Stop Started")
        super.onStop()
    }



}