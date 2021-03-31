package com.example.musicka

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.airbnb.epoxy.EpoxyRecyclerView
import com.example.musicka.databinding.ActivityMisBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi

class BindingActivity : AppCompatActivity() {

    var mPlayBackService: MyPlayBackService?=null

    private var mBound: Boolean = false

    var controller: MyPlayBackService.ServiceMusicController?=null


    var songs:List<String>?= emptyList()

    /** Defines callbacks for service binding, passed to bindService()  */
    @ExperimentalCoroutinesApi
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyPlayBackService.ServiceMusicController
            mPlayBackService = binder.getService()
            controller=mPlayBackService!!.musicController
            if(songs.isNullOrEmpty())
            songs= controller!!.musicData.value!!
            updateUi(songs!!)
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mPlayBackService=null
            controller=null
            mBound = false
        }
    }

    val rv: EpoxyRecyclerView
        get() = findViewById<EpoxyRecyclerView>(R.id.rv)

    private fun updateUi(songs: List<String>) {
        rv.withModels {
            songs.forEach { s ->
                simpleText {
                    id(s)
                    text(s)
                    click { _ ->
                        //try
                        if(true){
                           /* Intent(this, MyPlayBackService::class.java).also { intent ->
                                startService(intent)
                            }*/
                            Intent(this@BindingActivity,MyPlayBackService::class.java).also {
                                it.putExtra("id",s)
                                startService(it)
                            }
                        }
                    }
                }
            }

        }
    }

    fun ensureBind()=mBound


    lateinit var binding :ActivityMisBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis)
        binding=DataBindingUtil.setContentView(this,R.layout.activity_mis)
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
        mPlayBackService=null
        controller=null
        mBound = false
    }

}