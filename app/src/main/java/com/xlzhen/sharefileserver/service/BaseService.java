package com.xlzhen.sharefileserver.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/* loaded from: classes8.dex */
public abstract class BaseService extends Service {
    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        Log.i(getClass().getName(), "..........onBind..........");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        Log.i(getClass().getName(), "..........onCreate..........");
    }

    @Override // android.app.Service
    public void onDestroy() {
        super.onDestroy();
        Log.i(getClass().getName(), "..........onDestroy..........");
    }

    @Override // android.app.Service, android.content.ComponentCallbacks
    public void onLowMemory() {
        super.onLowMemory();
        Log.i(getClass().getName(), "..........onLowMemory..........");
    }

    @Override // android.app.Service
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.i(getClass().getName(), "..........onRebind..........");
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.i(getClass().getName(), "..........onStartCommand..........");
        return super.onStartCommand(intent, i, i2);
    }

    @Override // android.app.Service
    public void onTaskRemoved(Intent intent) {
        super.onTaskRemoved(intent);
        Log.i(getClass().getName(), "..........onTaskRemoved..........");
    }

    @Override // android.app.Service, android.content.ComponentCallbacks2
    public void onTrimMemory(int i) {
        super.onTrimMemory(i);
        String name = getClass().getName();
        Log.i(name, "..........onTrimMemory..........>" + i);
    }

    @Override // android.app.Service
    public boolean onUnbind(Intent intent) {
        Log.i(getClass().getName(), "..........onUnbind..........");
        return super.onUnbind(intent);
    }
}
