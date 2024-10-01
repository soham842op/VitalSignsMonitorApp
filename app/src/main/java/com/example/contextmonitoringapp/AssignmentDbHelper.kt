package com.example.contextmonitoringapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

class HealthDataDbHelper(context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL(CREATE_TABLE_SQL)
        } catch (e: SQLiteException) {
            // Silent catch, consider logging the error in a production environment
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(DROP_TABLE_SQL)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }

    companion object {
        const val DB_VERSION = 1
        const val DB_NAME = "health_db.db"

        private const val CREATE_TABLE_SQL = """
            CREATE TABLE patient_data (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT,
                heart_rate NUMERIC,
                respiratory_rate NUMERIC,
                nausea NUMERIC,
                headache NUMERIC,
                diarrhea NUMERIC,
                sore_throat NUMERIC,
                fever NUMERIC,
                muscle_ache NUMERIC,
                loss_of_smell NUMERIC,
                cough NUMERIC,
                shortness_of_breath NUMERIC,
                feeling_tired NUMERIC
            )
        """

        private const val DROP_TABLE_SQL = "DROP TABLE IF EXISTS patient_data"
    }
}