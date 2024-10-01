package com.example.contextmonitoringapp
import android.content.ContentValues
import android.content.Intent
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.RatingBar.OnRatingBarChangeListener
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date


class SymptomLoggingPage : AppCompatActivity() {
    var val1: ContentValues? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_symptom_logging_page)

        val back1 = findViewById<View>(R.id.backButton) as ImageButton
        val uploadSymptomsSubmit = findViewById<View>(R.id.uploadSymptomsButton) as Button
        val ratingBar = findViewById<View>(R.id.symptomRatingBar) as RatingBar


        val b = intent.extras
        if (b != null) val1 = b.getParcelable("Initial")

        val dbHelper: HealthDataDbHelper = HealthDataDbHelper(applicationContext)
        val db: SQLiteDatabase = dbHelper.getWritableDatabase()



        back1.setOnClickListener {
            val intent1 =
                Intent(applicationContext, MainActivity::class.java)
            startActivity(intent1)
        }

        uploadSymptomsSubmit.setOnClickListener {
            try {
                finalchk()
                val newRowId = db.insert("tblPat", null, val1)
            } catch (e: SQLException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_LONG).show()
            }
            Toast.makeText(
                applicationContext,
                "Data Inserted into Database. Redirecting in 5 Second",
                Toast.LENGTH_LONG
            ).show()
            try {
                Thread.sleep(5000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            val intent1 =
                Intent(applicationContext, MainActivity::class.java)
            startActivity(intent1)
        }

        val spinner = findViewById<View>(R.id.symptomSpinner) as Spinner
        spinner.setSelection(0)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.Symptoms, android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View,
                position: Int,
                id: Long
            ) {
                if (position > 0) {
                    ratingBar.onRatingBarChangeListener =
                        OnRatingBarChangeListener { ratingBar, rating, fromUser ->
                            val inp = spinner.selectedItem.toString()
                            var `var` = ""
                            if (inp.compareTo("Nausea") == 0) {
                                `var` = "symptomsNausea"
                            } else if (inp.compareTo("Headache") == 0) {
                                `var` = "symptomsHeadache"
                            } else if (inp.compareTo("Diarrhea") == 0) {
                                `var` = "symptomsDiarrhea"
                            } else if (inp.compareTo("Soar Throat") == 0) {
                                `var` = "symptomsSoarThroat"
                            } else if (inp.compareTo("Fever") == 0) {
                                `var` = "symptomsFever"
                            } else if (inp.compareTo("Muscle Ache") == 0) {
                                `var` = "symptomsMuscleAche"
                            } else if (inp.compareTo("Loss of Smell or Taste") == 0) {
                                `var` = "symptomsLOS"
                            } else if (inp.compareTo("Cough") == 0) {
                                `var` = "symptomsCough"
                            } else if (inp.compareTo("Shortness of Breath") == 0) {
                                `var` = "symptomsSOB"
                            } else if (inp.compareTo("Feeling Tired") == 0) {
                                `var` = "FeelingFT"
                            }
                            val1!!.put(`var`, rating)
                        }
                    ratingBar.rating = 0f
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    private fun finalchk() {
        val False = false
        if (val1!!.containsKey("timestamp") == False) {
            val timeStamp = SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(Date())
            val1!!.put("timestamp", timeStamp)
        }
        if (val1!!.containsKey("symptomsNausea") == False) {
            val1!!.put("symptomsNausea", 0)
        }
        if (val1!!.containsKey("symptomsHeadache") == False) {
            val1!!.put("symptomsHeadache", 0)
        }
        if (val1!!.containsKey("symptomsDiarrhea") == False) {
            val1!!.put("symptomsDiarrhea", 0)
        }
        if (val1!!.containsKey("symptomsSoarThroat") == False) {
            val1!!.put("symptomsSoarThroat", 0)
        }
        if (val1!!.containsKey("symptomsFever") == False) {
            val1!!.put("symptomsFever", 0)
        }
        if (val1!!.containsKey("symptomsMuscleAche") == False) {
            val1!!.put("symptomsMuscleAche", 0)
        }
        if (val1!!.containsKey("symptomsLOS") == False) {
            val1!!.put("symptomsLOS", 0)
        }
        if (val1!!.containsKey("symptomsCough") == False) {
            val1!!.put("symptomsCough", 0)
        }
        if (val1!!.containsKey("symptomsSOB") == False) {
            val1!!.put("symptomsSOB", 0)
        }
        if (val1!!.containsKey("FeelingFT") == False) {
            val1!!.put("FeelingFT", 0)
        }
    }
}