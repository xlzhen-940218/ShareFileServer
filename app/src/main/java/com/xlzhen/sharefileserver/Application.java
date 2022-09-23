package com.xlzhen.sharefileserver;

import android.content.Context;

/* loaded from: classes3.dex */
public class Application extends android.app.Application {
    public static Application application = null;
    public static String registerShareFile = null;
    public static String[] shareFiles = null;
    public static String clipDataNote = "";//复制文本Note

    public static void setClipDataNote(String str) {
        clipDataNote = str;
    }

    public static String getRegisterShareFile() {
        return registerShareFile;
    }

    public static String[] getShareFiles() {
        return shareFiles;
    }

    public static String getClipDataNote() {
        return clipDataNote;
    }

    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        application = this;
    }

    public static Context getContext() {
        return application.getApplicationContext();
    }

    public static void registerShareFile(boolean register, String str) {
        registerShareFile = str;
    }

    public static void setShareFiles(String[] strArr) {
        shareFiles = strArr;
    }
}
