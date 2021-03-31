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

    val data
        get() = mutableListOf("http://m801.music.126.net/20210331211617/a816f7d8088c3885e4da53c671152bfc/jdymusic/obj/w5zDlMODwrDDiGjCn8Ky/2234478436/ea74/268b/d0b0/35427f829b28d425340b3d6b8db8030a.mp3",
            "http://m701.music.126.net/20210331211701/add5c3fd6f323a2d2b0e8e7f751fa20a/jdymusic/obj/wo3DlMOGwrbDjj7DisKw/4959745806/482a/1a84/ca27/02c0f32c8c1b78a97988cebbee2ede1b.mp3")

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
        updateUi(data)
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
       /* Intent(this, MyPlayBackService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }*/
    }

    override fun onStop() {
        super.onStop()
        //unbindService(connection)
        mPlayBackService=null
        controller=null
        mBound = false
    }

}