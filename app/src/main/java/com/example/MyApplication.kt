package com.example

import android.app.Application

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashLogger.init(this)
    }
}
