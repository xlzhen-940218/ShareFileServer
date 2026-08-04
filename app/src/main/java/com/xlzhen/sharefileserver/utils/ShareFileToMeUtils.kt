package com.xlzhen.sharefileserver.utils

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import com.xlzhen.sharefileserver.Application
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.ArrayList

object ShareFileToMeUtils {
    @JvmStatic
    fun getShareFiles(activity: Activity): List<String> {
        val arrayList = ArrayList<String>()
        val intent = activity.intent
        if (Intent.ACTION_SEND == intent.action) {
            if (NanoHTTPD.MIME_PLAINTEXT == intent.type) {
                val clipData = intent.clipData
                if (clipData != null && clipData.itemCount > 0) {
                    Application.clipDataNote = clipData.getItemAt(0).text.toString()
                }
                return ArrayList()
            }
            val uri = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri
            if (uri != null) {
                val parsed = parseFileUri(activity, uri)
                if (parsed != null) arrayList.add(parsed)
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == intent.action) {
            val list = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
            if (list != null) {
                for (uri in list) {
                    val parsed = parseFileUri(activity, uri as Uri)
                    if (parsed != null) arrayList.add(parsed)
                }
            }
        }
        return arrayList
    }

    @JvmStatic
    fun parseFileUri(activity: Activity, uri: Uri?): String? {
        var str = ""
        if (uri == null) {
            return str
        }
        Log.v("Before Transformation", uri.path ?: "")
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        val path = uri.path
        if (path != null && path.startsWith("/root/data/app/") && path.endsWith(".apk")) {
            return path.replace("/root/data/app/", "data/app/")
        }
        var str2: String? = null
        if (!"content".equals(uri.scheme, ignoreCase = true) || !("com.google.android.apps.photos.contentprovider".equals(uri.host, ignoreCase = true))) {
            try {
                @Suppress("DEPRECATION")
                val managedQuery = activity.managedQuery(uri, arrayOf("_data"), null, null, null)
                if (managedQuery != null) {
                    val columnIndexOrThrow = managedQuery.getColumnIndexOrThrow("_data")
                    if (managedQuery.moveToFirst()) {
                        str2 = managedQuery.getString(columnIndexOrThrow)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (str2 == null) {
                str2 = ContentUriUtil.getPath(activity, uri)
            }
            if (str2 != null) {
                str = str2
            }
            Log.v("After Transformation", str)
            return str
        }
        try {
            val inputStream = activity.contentResolver.openInputStream(uri)
            val decodeStream = BitmapFactory.decodeStream(inputStream)
            val byteArrayOutputStream = ByteArrayOutputStream()
            decodeStream?.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val externalFilesDir = Application.getContext().getExternalFilesDir("Google Photo")
            if (externalFilesDir != null && !externalFilesDir.exists()) {
                externalFilesDir.mkdirs()
            }
            val absolutePath = externalFilesDir?.absolutePath ?: return null
            val file = File(absolutePath, "${System.currentTimeMillis()}.jpg")
            try {
                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(byteArrayOutputStream.toByteArray())
                fileOutputStream.flush()
                fileOutputStream.close()
            } catch (e2: IOException) {
                e2.printStackTrace()
            }
            return file.absolutePath
        } catch (e3: FileNotFoundException) {
            e3.printStackTrace()
            return null
        }
    }
}
