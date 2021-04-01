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
import com.example.musicka.MyPlayBackService.Companion.COMPLETED
import com.example.musicka.MyPlayBackService.Companion.INIT
import com.example.musicka.MyPlayBackService.Companion.PAUSED
import com.example.musicka.MyPlayBackService.Companion.PLAYING
import com.example.musicka.MyPlayBackService.Companion.PREPARING
import com.example.musicka.databinding.ActivityMisBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi


class BindingActivity : AppCompatActivity() {

    var mPlayBackService: MyPlayBackService?=null

    private var mBound: Boolean = false


    val controller: MyPlayBackService.ServiceMusicController?
        get() = AppMusicUtil.controller?.get()


    var songs:List<String>?= emptyList()

    /** Defines callbacks for service binding, passed to bindService()  */
    @ExperimentalCoroutinesApi
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyPlayBackService.ServiceMusicController
            mPlayBackService = binder.getService()
            if(songs.isNullOrEmpty())
            songs= controller!!.musicData.value!!
            updateUi(songs!!)
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mPlayBackService=null
            mBound = false
        }
    }

    val rv: EpoxyRecyclerView
        get() = findViewById<EpoxyRecyclerView>(R.id.rv)

    val data
        get() = mutableListOf("http://isure.stream.qqmusic.qq.com/C400003zDTau0boSQm.m4a?guid=2958323637&vkey=72B5A322351DCFB5B1FF4C3013479DF80E8EC61E196DB7C407BA21BCAA529E820A940220AECBBA553F67118D1805A16579C01AE30C2291D0&uin=3203891186&fromtag=66",
            "http://isure.stream.qqmusic.qq.com/C400000eMCTT1akCEg.m4a?guid=2958323637&vkey=9535B6CB21D053C2DCDC999804248BAD8BF1FC0716D7630476869D56128E56C5F0F42076FB9B186A18DDF3013490DDDD5248B35B645771EE&uin=3203891186&fromtag=66")

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
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mis)
        updateUi(data)
        AppMusicUtil.playbackState.observe(this) {
            when (it) {
                //not playing idle
                INIT -> {
                    AppMusicUtil.pendingSong?.value?.also {
                        binding.tvSong.text=it
                    }
                    binding.playPause.setImageResource(R.mipmap.play)
                }//created ,nothing playing , init ->  preparing -> playing ->pause ->playing ->completed
                PLAYING -> {
                    AppMusicUtil.focusSong.value?.also {
                        binding.tvSong.text=it
                    }
                    binding.playPause.setImageResource(R.mipmap.pause)
                }
                COMPLETED -> {
                    AppMusicUtil.pendingSong?.value?.also {
                        binding.tvSong.text=it
                    }
                    binding.playPause.setImageResource(R.mipmap.play)
                } //idle
                PAUSED -> {
                    AppMusicUtil.focusSong.value?.also {
                        binding.tvSong.text=it
                    }
                    binding.playPause.setImageResource(R.mipmap.play)
                }
                PREPARING -> {
                    AppMusicUtil.pendingSong?.value?.also {
                        binding.tvSong.text=it
                    }
                    binding.playPause.setImageResource(R.mipmap.pause)
                }
                else ->{

                }
            }
        }
        if(AppMusicUtil.pendingSong.value!=null){
            AppMusicUtil.pendingSong.value?.also {
                AppMusicUtil.pendingSong?.value?.also {
                    binding.tvSong.text=it
                }
            }
        }
        else{
            binding.tvSong.text="http://isure.stream.qqmusic.qq.com/C400003zDTau0boSQm.m4a?guid=2958323637&vkey=72B5A322351DCFB5B1FF4C3013479DF80E8EC61E196DB7C407BA21BCAA529E820A940220AECBBA553F67118D1805A16579C01AE30C2291D0&uin=3203891186&fromtag=66"
        }


        binding.playPause.setOnClickListener {
            AppMusicUtil.focusSong.value?.also { s  ->
                Intent(this@BindingActivity,MyPlayBackService::class.java).also {
                    it.putExtra("id",s)
                    startService(it)
                }
                return@setOnClickListener
            }
            AppMusicUtil.pendingSong.value?.also { s  ->
                Intent(this@BindingActivity,MyPlayBackService::class.java).also {
                    it.putExtra("id",s)
                    startService(it)
                }
                return@setOnClickListener
            }

            Intent(this@BindingActivity,MyPlayBackService::class.java).also {
                it.putExtra("id","http://isure.stream.qqmusic.qq.com/C400003zDTau0boSQm.m4a?guid=2958323637&vkey=72B5A322351DCFB5B1FF4C3013479DF80E8EC61E196DB7C407BA21BCAA529E820A940220AECBBA553F67118D1805A16579C01AE30C2291D0&uin=3203891186&fromtag=66")
                startService(it)
            }
        }

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
        mBound = false
    }

}