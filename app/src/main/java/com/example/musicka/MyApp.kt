package com.example.musicka

import android.app.Application

class MyApp:Application() {

    companion object{
        lateinit var app:Application
    }

    override fun onCreate() {
        super.onCreate()
        app=this
    }
}