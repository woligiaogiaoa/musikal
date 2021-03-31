package com.example.musicka

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

class BindingActivity : AppCompatActivity() {
    private lateinit var mPlayBackService: MyPlayBackService
    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    @ExperimentalCoroutinesApi
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyPlayBackService.ServiceMusicController
            mPlayBackService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.binding)

    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        Intent(this, MyPlayBackService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

}