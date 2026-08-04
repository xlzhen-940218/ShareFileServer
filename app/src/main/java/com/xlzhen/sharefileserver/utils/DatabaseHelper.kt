package com.xlzhen.sharefileserver.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ShareFileServer.db"
        const val TABLE_SETTINGS = "settings"
        const val COLUMN_KEY = "setting_key"
        const val COLUMN_VALUE = "setting_value"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSettingsTable = ("CREATE TABLE " + TABLE_SETTINGS + "("
                + COLUMN_KEY + " TEXT PRIMARY KEY,"
                + COLUMN_VALUE + " TEXT" + ")")
        db.execSQL(createSettingsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        onCreate(db)
    }

    fun setSetting(key: String, value: String) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COLUMN_KEY, key)
        values.put(COLUMN_VALUE, value)
        
        val cursor = db.query(TABLE_SETTINGS, arrayOf(COLUMN_KEY), "$COLUMN_KEY=?", arrayOf(key), null, null, null)
        if (cursor.moveToFirst()) {
            db.update(TABLE_SETTINGS, values, "$COLUMN_KEY=?", arrayOf(key))
        } else {
            db.insert(TABLE_SETTINGS, null, values)
        }
        cursor.close()
        db.close()
    }

    fun getSetting(key: String): String? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_SETTINGS, arrayOf(COLUMN_VALUE), "$COLUMN_KEY=?", arrayOf(key), null, null, null)
        var value: String? = null
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(COLUMN_VALUE)
            if (index != -1) {
                value = cursor.getString(index)
            }
        }
        cursor.close()
        db.close()
        return value
    }
}
