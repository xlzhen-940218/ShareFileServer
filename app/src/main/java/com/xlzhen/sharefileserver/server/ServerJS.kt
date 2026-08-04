package com.xlzhen.sharefileserver.server

import android.content.res.AssetManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.evgenii.jsevaluator.JsEvaluator
import com.evgenii.jsevaluator.callback.JsCallback
import com.evgenii.jsevaluator.utils.MimeTypeConvert
import com.xlzhen.sharefileserver.Application
import com.xlzhen.sharefileserver.entity.ServerPackage
import fi.iki.elonen.NanoHTTPD
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.util.HashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ServerJS private constructor() {
    var jsEvaluator: JsEvaluator? = null
    var jsResult: String? = null

    fun initServerJS(serverPackage: ServerPackage, bVar: JsEvaluator) {
        try {
            val assets = Application.getContext().assets
            val open = DataInputStream(assets.open("${serverPackage.serverPath}/${serverPackage.main}"))
            val bArr = ByteArray(open.available())
            open.readFully(bArr)
            serverPackage.serverJs = String(bArr)
            open.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        loadModule(serverPackage, 0)
        bVar.callFunction(serverPackage.serverJs, object : JsCallback {
            override fun onResult(var1: String) {
                println(var1)
            }

            override fun onError(var1: String) {}
        }, "main", HashMap<String, String>())
    }

    @Synchronized
    fun executeJSServerAPI(
        serverPath: String?,
        serverJs: String?,
        substring: String,
        uploadFile: Boolean,
        params: MutableMap<String, String>
    ): NanoHTTPD.Response {
        val matcher = Pattern.compile("function.*?$substring.*?\\((.*?)\\)").matcher(serverJs ?: "")
        if (!matcher.find()) {
            return NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.NOT_FOUND,
                "application/json; charset=utf-8",
                null
            )
        }
        val split = matcher.group(1)?.split(",") ?: emptyList()
        if (split.size > params.size) {
            for (str4 in split) {
                if (!params.containsKey(str4.trim()) && str4.trim().isNotEmpty()) {
                    params[str4.trim()] = ""
                }
            }
        }
        val countDownLatch = CountDownLatch(1)
        jsEvaluator?.callFunction(serverJs, object : JsCallback {
            override fun onResult(str: String) {
                jsResult = str
                countDownLatch.countDown()
            }

            override fun onError(str: String) {
                jsResult = str
                countDownLatch.countDown()
            }
        }, substring, params)

        try {
            countDownLatch.await(if (uploadFile) 3600L else 10L, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        val str5 = jsResult
        if (str5 == null) {
            return NanoHTTPD.newChunkedResponse(
                NanoHTTPD.Response.Status.BAD_REQUEST,
                "application/json; charset=utf-8",
                null
            )
        }
        if (str5.contains("requestStorageURL")) {
            try {
                val jsonObject = JSONObject(str5)
                if (jsonObject.getInt("code") == 200) {
                    val string = jsonObject.getString("requestStorageURL")
                    val string2 = jsonObject.getString("filename")
                    try {
                        if (!string.contains("storage/") && !string.contains("data/app")) {
                            return NanoHTTPD.newChunkedResponse(
                                NanoHTTPD.Response.Status.OK,
                                if (string.endsWith(".css")) "text/css" else "*/*",
                                Application.getContext().assets.open("$serverPath/$string")
                            )
                        }
                        val a = MimeTypeConvert.getSuffix(string.substring(string.lastIndexOf(".") + 1))
                        Log.i("Play MimeType", a)
                        return NanoHTTPD.newChunkedResponse(
                            NanoHTTPD.Response.Status.OK,
                            a,
                            FileInputStream(string)
                        )
                    } catch (e7: FileNotFoundException) {
                        e7.printStackTrace()
                        return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.NOT_FOUND, "*/*", null)
                    } catch (e8: IOException) {
                        e8.printStackTrace()
                        return NanoHTTPD.newFixedLengthResponse(str5)
                    }
                }
            } catch (e10: JSONException) {
                e10.printStackTrace()
            }
        }
        if (str5.contains("requestToast")) {
            try {
                val jsonObject2 = JSONObject(str5)
                if (jsonObject2.getInt("code") == 200) {
                    Handler(Application.getContext().mainLooper).post {
                        try {
                            Toast.makeText(
                                Application.getContext(),
                                jsonObject2.getString("message"),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e11: JSONException) {
                            e11.printStackTrace()
                        }
                    }
                }
            } catch (e52: JSONException) {
                e52.printStackTrace()
            }
        }
        return NanoHTTPD.newFixedLengthResponse(str5)
    }

    fun init(serverPackage: ServerPackage, bVar: JsEvaluator) {
        this.jsEvaluator = bVar
        initServerJS(serverPackage, bVar)
    }

    fun loadModule(serverPackage: ServerPackage, i: Int) {
        val serverJs = serverPackage.serverJs ?: return
        val matcher = Pattern.compile("require\\((.*?)\\)").matcher(serverJs)
        var i2 = 0
        while (matcher.find()) {
            i2++
            if (i2 > i) {
                val group = matcher.group(1) ?: continue
                val compile = Pattern.compile("require\\($group\\)")
                val replace = group.replace("\"", "").replace("'", "")
                var nextI = i
                try {
                    val assets = Application.getContext().assets
                    val open = DataInputStream(assets.open("${serverPackage.serverPath}/$replace"))
                    val bArr = ByteArray(open.available())
                    open.readFully(bArr)
                    serverPackage.serverJs = compile.matcher(serverPackage.serverJs ?: "").replaceFirst(String(bArr))
                    open.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                    nextI++
                }
                loadModule(serverPackage, nextI)
                return
            }
        }
    }

    companion object {
        @Volatile
        var instance: ServerJS? = null
            get() {
                if (field == null) {
                    synchronized(ServerJS::class.java) {
                        if (field == null) {
                            field = ServerJS()
                        }
                    }
                }
                return field
            }
            private set
    }
}
