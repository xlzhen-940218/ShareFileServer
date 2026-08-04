package com.xlzhen.sharefileserver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.text.Html
import com.evgenii.jsevaluator.JsEvaluator
import com.xlzhen.sharefileserver.R
import com.xlzhen.sharefileserver.entity.ServerPackage
import com.xlzhen.sharefileserver.server.MiniJsServer
import com.xlzhen.sharefileserver.server.ServerJS
import com.xlzhen.sharefileserver.utils.NetWorkUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.DataInputStream
import java.io.IOException

class ServerRunService : BaseService() {
    var rootPort = 8080
    var userPort = 8090

    fun startManagementServer() {
        host = "http://${NetWorkUtils.getDeviceIp()}:$rootPort"
        try {
            val open = DataInputStream(assets.open("management/package.json"))
            val bArr = ByteArray(open.available())
            open.readFully(bArr)
            val jsonObject = JSONObject(String(bArr))
            val serverPackage = ServerPackage().apply {
                logo = jsonObject.getString("logo")
                name = jsonObject.getString("name")
                version = jsonObject.getString("version")
                author = jsonObject.getString("author")
                description = jsonObject.getString("description")
                main = jsonObject.getString("main")
                serverPath = "management"
            }
            open.close()

            ServerJS.instance!!.init(serverPackage, JsEvaluator(this))
            serverPackage.miniJsServer = MiniJsServer(rootPort, serverPackage)

            try {
                serverPackage.miniJsServer?.start()
            } catch (e2: IOException) {
                e2.printStackTrace()
            }
            serverPackage.port = host
            serverPackage.isPower = true
        } catch (e3: IOException) {
            e3.printStackTrace()
        } catch (e4: JSONException) {
            e4.printStackTrace()
        }
    }

    fun startUserServer() {
        val str = "http://${NetWorkUtils.getDeviceIp()}:$userPort"
        try {
            val open = DataInputStream(assets.open("web/package.json"))
            val bArr = ByteArray(open.available())
            open.readFully(bArr)
            val jsonObject = JSONObject(String(bArr))
            val serverPackage = ServerPackage().apply {
                logo = jsonObject.getString("logo")
                name = jsonObject.getString("name")
                version = jsonObject.getString("version")
                author = jsonObject.getString("author")
                description = jsonObject.getString("description")
                main = jsonObject.getString("main")
                serverPath = "web"
            }
            open.close()

            ServerJS.instance!!.init(serverPackage, JsEvaluator(this))
            serverPackage.miniJsServer = MiniJsServer(userPort, serverPackage)

            try {
                serverPackage.miniJsServer?.start()
            } catch (e2: IOException) {
                e2.printStackTrace()
            }
            serverPackage.port = str
            serverPackage.isPower = true
        } catch (e4: IOException) {
            e4.printStackTrace()
        } catch (e3: JSONException) {
            e3.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startManagementServer()
        startUserServer()
        notification()
        return super.onStartCommand(intent, Service.START_FLAG_REDELIVERY, startId)
    }

    fun notification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                ServerRunService::class.java.name,
                ServerRunService::class.java.simpleName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(applicationContext, ServerRunService::class.java.name)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(applicationContext)
        }

        val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(getString(R.string.notification_content), Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(getString(R.string.notification_content))
        }
        builder.setContentText(htmlText)
        builder.setContentTitle(getString(R.string.notification_title))
        builder.setSmallIcon(R.mipmap.ic_launcher_notification)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            builder.setLargeIcon(Icon.createWithResource(applicationContext, R.mipmap.ic_launcher))
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 1, intent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        val build = builder.build()
        build.flags = build.flags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, build, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, build)
        }
    }

    companion object {
        var f284c: ServerRunService? = null
        var host: String? = null
            private set

        @JvmStatic
        fun start(context: Context) {
            if (f284c == null) {
                f284c = ServerRunService()
                context.startService(Intent(context, ServerRunService::class.java))
            }
        }
    }
}
