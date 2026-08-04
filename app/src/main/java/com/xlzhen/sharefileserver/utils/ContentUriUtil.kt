package com.xlzhen.sharefileserver.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore

object ContentUriUtil {
    @JvmStatic
    fun getPath(context: Context, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
        if (!isKitKat || !DocumentsContract.isDocumentUri(context, uri)) {
            if ("content".equals(uri.scheme, ignoreCase = true)) {
                if (isGooglePhotosUri(uri)) {
                    return uri.lastPathSegment
                }
                return getDataColumn(context, uri, null, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                return uri.path
            }
        } else if (isExternalStorageDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            if ("primary".equals(split[0], ignoreCase = true)) {
                return "${Environment.getExternalStorageDirectory()}/${split[1]}"
            }
        } else if (isDownloadsDocument(uri)) {
            val id = DocumentsContract.getDocumentId(uri)
            try {
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    id.toLong()
                )
                return getDataColumn(context, contentUri, null, null)
            } catch (e: NumberFormatException) {
                // Ignore
            }
        } else if (isMediaDocument(uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val type = split[0]
            var contentUri: Uri? = null
            if ("image" == type) {
                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            } else if ("video" == type) {
                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else if ("audio" == type) {
                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            val selectionArgs = arrayOf(split[1])
            return getDataColumn(context, contentUri, "_id=?", selectionArgs)
        }
        return null
    }

    @JvmStatic
    fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        if (uri == null) return null
        var cursor: Cursor? = null
        val projection = arrayOf("_data")
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor == null || !cursor.moveToFirst()) {
                cursor?.close()
                return null
            }
            val index = cursor.getColumnIndexOrThrow("_data")
            val string = cursor.getString(index)
            cursor.close()
            return string
        } catch (ex: Exception) {
            ex.printStackTrace()
            if (uri.path != null && !uri.path!!.startsWith("/storage/emulated/0/") && !uri.path!!.startsWith("/sdcard/")) {
                cursor?.close()
                return null
            }
            val path = uri.path
            cursor?.close()
            return path
        }
    }

    @JvmStatic
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    @JvmStatic
    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    @JvmStatic
    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    @JvmStatic
    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}
