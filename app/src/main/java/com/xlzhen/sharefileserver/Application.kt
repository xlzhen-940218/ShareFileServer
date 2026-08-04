package com.xlzhen.sharefileserver

import android.content.Context

class Application : android.app.Application() {
    companion object {
        var application: Application? = null
        var registerShareFile: String? = null
        var shareFiles: Array<String>? = null
        var clipDataNote: String = "" // 复制文本Note

        fun getContext(): Context {
            return application!!.applicationContext
        }

        fun registerShareFile(register: Boolean, str: String?) {
            registerShareFile = str
        }
    }

    override fun onCreate() {
        super.onCreate()
        application = this
    }
}
