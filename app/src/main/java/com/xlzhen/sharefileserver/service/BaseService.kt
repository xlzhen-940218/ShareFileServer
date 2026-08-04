package com.xlzhen.sharefileserver.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

abstract class BaseService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        Log.i(javaClass.name, "..........onBind..........")
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(javaClass.name, "..........onCreate..........")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(javaClass.name, "..........onDestroy..........")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.i(javaClass.name, "..........onLowMemory..........")
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.i(javaClass.name, "..........onRebind..........")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(javaClass.name, "..........onStartCommand..........")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)
        Log.i(javaClass.name, "..........onTaskRemoved..........")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.i(javaClass.name, "..........onTrimMemory..........>$level")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i(javaClass.name, "..........onUnbind..........")
        return super.onUnbind(intent)
    }
}
