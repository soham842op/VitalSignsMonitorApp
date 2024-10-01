package com.example.contextmonitoringapp

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.SQLException
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val VIDEO_CAPTURE_REQUEST_CODE = 101
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private var videoUri: Uri? = null
    private val accelerometerValuesX = mutableListOf<Float>()
    private val accelerometerValuesY = mutableListOf<Float>()
    private val accelerometerValuesZ = mutableListOf<Float>()
    private lateinit var vibrationService: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val symptomLoggingButton = findViewById<Button>(R.id.uploadSignsButton)
        val heartRateButton = findViewById<Button>(R.id.measureHeartRateButton)
        val respiratoryRateButton = findViewById<Button>(R.id.measureRespiratoryRateButton)
        val heartRateTextView = findViewById<TextView>(R.id.heartRateTextView)
        val respiratoryRateTextView = findViewById<TextView>(R.id.respiratoryRateTextView)

        vibrationService = getSystemService(VIBRATOR_SERVICE) as Vibrator
        accelerometerValuesX.clear()
        accelerometerValuesY.clear()
        accelerometerValuesZ.clear()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        // Create an Intent to navigate to the SymptomLoggingPage activity and also passing the data of heart rate and muscle rate
        symptomLoggingButton.setOnClickListener {
            val intent = Intent(applicationContext, SymptomLoggingPage::class.java)
            try {
                val values = ContentValues().apply {
                    put("heartRate", heartRateTextView.text.toString())
                    put("respiratoryRate", respiratoryRateTextView.text.toString())
                }
                intent.putExtra("Initial", values)
            } catch (e: SQLException) {
                Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
            }
            startActivity(intent)
        }

        respiratoryRateButton.setOnClickListener {
            Thread(CalcRespRateThread()).apply {
                Toast.makeText(
                    applicationContext,
                    "Please lie down and place the phone on your chest.",
                    Toast.LENGTH_SHORT
                ).show()
                start()
            }
        }


        heartRateButton.setOnClickListener {
            requestCameraPermission()
            val mediaFile = File(
                "${Environment.getExternalStorageDirectory()}/Android/data/com.example.contextmonitoringapp/files/fingerTip.mp4"
            )
            videoUri = FileProvider.getUriForFile(
                applicationContext,
                "com.example.contextmonitoringapp.provider",
                mediaFile
            )
            Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {

                putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivityForResult(this, VIDEO_CAPTURE_REQUEST_CODE)
            }
            println(videoUri?.path)
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VIDEO_CAPTURE_REQUEST_CODE) {
            when (resultCode) {
                RESULT_OK -> {
                    val videoUri = data?.data
                    println("Video URI: $videoUri")
                    val heartRateTextView = findViewById<TextView>(R.id.heartRateTextView)
                    heartRateTextView.text = "Calculating..."
                    GlobalScope.launch {
                        val calculatedHeartRate = heartRateCalculator(videoUri!!, contentResolver)
                        println("Calculated Heart Rate: $calculatedHeartRate")
                        runOnUiThread {
                            heartRateTextView.text = "$calculatedHeartRate bpm"
                        }
                    }
                }
                RESULT_CANCELED -> {
                    Toast.makeText(applicationContext, "Recording cancelled", Toast.LENGTH_SHORT).show()
                    println("Recording cancelled by user")
                }
                else -> {
                    Toast.makeText(applicationContext, "Recording failed", Toast.LENGTH_SHORT).show()
                    println("Recording failed due to unknown error")
                }
            }
        }
    }

    private inner class CalcRespRateThread : Runnable {
        override fun run() {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + 45 * 1000

            Thread.sleep(3000) // Wait before starting measurement

            vibrationService?.vibrate(500) // Notify user to start measurement

            Thread.sleep(2000) // Allow vibration to complete

            val accelerometerListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    event?.let {
                        if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                            accelerometerValuesX.add(it.values[0])
                            accelerometerValuesY.add(it.values[1])
                            accelerometerValuesZ.add(it.values[2])

                            println("Accelerometer Data - X: ${it.values[0]}, Y: ${it.values[1]}, Z: ${it.values[2]}")
                        }
                    }

                    if (System.currentTimeMillis() > endTime) {
                        sensorManager.unregisterListener(this)

                        val respiratoryRate = respiratoryRateCalculator(
                            accelerometerValuesX,
                            accelerometerValuesY,
                            accelerometerValuesZ
                        )

                        vibrationService?.vibrate(500) // Notify user that measurement is complete
                        println("Calculated Respiratory Rate: $respiratoryRate")
                        runOnUiThread {
                            findViewById<TextView>(R.id.respiratoryRateTextView).text = "$respiratoryRate breaths per minute"
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    //Heart rate calculator helper code
    @RequiresApi(Build.VERSION_CODES.P)
    suspend fun heartRateCalculator(uri: Uri, contentResolver: ContentResolver): Int {
        return withContext(Dispatchers.IO) {
            val result: Int

            // Use a file descriptor to read the video instead of converting the Uri to a path
            val retriever = MediaMetadataRetriever()
            val frameList = ArrayList<Bitmap>()
            try {
                val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                parcelFileDescriptor?.use {
                    retriever.setDataSource(it.fileDescriptor)
                }

                val duration = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT
                )?.toIntOrNull()

                if (duration != null) {
                    val frameDuration = min(duration, 425)
                    var i = 10
                    while (i < frameDuration) {
                        val bitmap = retriever.getFrameAtIndex(i)
                        bitmap?.let { frameList.add(it) }
                        i += 15
                    }
                }
            } catch (e: Exception) {
                Log.d("MediaPath", "Error processing video: ${e.stackTraceToString()}")
            } finally {
                retriever.release()
                var redBucket: Long
                var pixelCount: Long = 0
                val a = mutableListOf<Long>()
                for (i in frameList) {
                    redBucket = 0
                    for (y in 350 until 450) {
                        for (x in 350 until 450) {
                            val c: Int = i.getPixel(x, y)
                            pixelCount++
                            redBucket += Color.red(c) + Color.blue(c) + Color.green(c)
                        }
                    }
                    a.add(redBucket)
                }

                val b = mutableListOf<Long>()
                for (i in 0 until a.lastIndex - 5) {
                    val temp = (a[i] + a[i + 1] + a[i + 2] + a[i + 3] + a[i + 4]) / 4
                    b.add(temp)
                }

                var x = if (b.isNotEmpty()) b[0] else 0
                var count = 0
                for (i in 1 until b.lastIndex) {
                    val p = b[i]
                    if ((p - x) > 3500) {
                        count++
                    }
                    x = b[i]
                }

                val rate = (count.toFloat() * 60).toInt()
                result = rate / 4
            }

            return@withContext result
        }
    }


    // Respiratory rate calculation helper function
    fun respiratoryRateCalculator(
        accelValuesX: MutableList<Float>,
        accelValuesY: MutableList<Float>,
        accelValuesZ: MutableList<Float>,
    ): Int {
        var previousValue = 10f
        var k = 0
        for (i in 11 until accelValuesY.size) {
            val currentValue = kotlin.math.sqrt(
                accelValuesZ[i].toDouble().pow(2.0) + accelValuesX[i].toDouble().pow(2.0) + accelValuesY[i].toDouble().pow(2.0)
            ).toFloat()
            if (abs(previousValue - currentValue) > 0.15) {
                k++
            }
            previousValue = currentValue
        }
        val ret = k.toDouble() / 45.00
        return (ret * 30).toInt()
    }
}