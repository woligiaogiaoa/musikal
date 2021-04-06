package com.example.musicka

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import com.airbnb.epoxy.EpoxyRecyclerView
import com.example.musicka.MyPlayBackService.Companion.COMPLETED
import com.example.musicka.MyPlayBackService.Companion.INIT
import com.example.musicka.MyPlayBackService.Companion.NOTIFICATION_COMMAND_KEY
import com.example.musicka.MyPlayBackService.Companion.NOTIFICATION_COMMANE_PLAY_PAUSE
import com.example.musicka.MyPlayBackService.Companion.PAUSED
import com.example.musicka.MyPlayBackService.Companion.PLAYING
import com.example.musicka.MyPlayBackService.Companion.PREPARING
import com.example.musicka.MyPlayBackService.Companion.RECENT
import com.example.musicka.MyPlayBackService.Companion.SEEK_POSITION_DATA_KEY
import com.example.musicka.databinding.ActivityMisBinding
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class BindingActivity : AppCompatActivity() {

    val rv: EpoxyRecyclerView
        get() = findViewById<EpoxyRecyclerView>(R.id.rv)

    val data
        get() =MusicRepository().data

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
                            Intent(this@BindingActivity, MyPlayBackService::class.java).also {
                                it.putExtra("id", s)
                                startService(it)
                            }
                        }
                    }
                }
            }

        }
    }




    lateinit var binding :ActivityMisBinding

    val TAG="music activity"

    val sp get() = PreferenceManager.getDefaultSharedPreferences(MyApp.app)

    val recent get() = sp.getString(RECENT, "")

    fun goneControllerview(){binding.controllerView.isVisible=false;Log.e(TAG, "goneControllerview: ")}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_mis)
        updateUi(data)
        AppMusicUtil.playbackState.observe(this) {
            when (it) {
                //not playing idle
                INIT -> { //ready to play
                    AppMusicUtil.pendingSong.value?.also {
                        binding.controllerView.isVisible = true
                        binding.tvSong.text = it
                        Log.e(TAG, "visibleControllerview")
                    } ?: goneControllerview()
                    binding.playPause.setImageResource(R.mipmap.play)

                }//created ,nothing playing , init ->  preparing -> playing ->pause ->playing ->completed
                PLAYING -> {
                    AppMusicUtil.focusSong.value?.also {
                        binding.controllerView.isVisible = true
                        binding.tvSong.text = it
                    }
                    binding.playPause.setImageResource(R.mipmap.pause)
                }
                COMPLETED -> {
                    AppMusicUtil.pendingSong.value?.also {
                        binding.controllerView.isVisible = true
                        binding.tvSong.text = it
                    }
                    AppMusicUtil.focusSong.value?.also {
                        binding.controllerView.isVisible = true
                        binding.tvSong.text = it
                    }
                    binding.playPause.setImageResource(R.mipmap.play)
                } //idle
                PAUSED -> {
                    AppMusicUtil.focusSong.value?.also {
                        binding.controllerView.isVisible = true
                        binding.tvSong.text = it
                    }
                    binding.playPause.setImageResource(R.mipmap.play)
                }
                PREPARING -> {
                    AppMusicUtil.focusSong.value!!.also {
                        binding.tvSong.text = it
                        binding.controllerView.isVisible = true
                    }
                    binding.playPause.setImageResource(R.mipmap.pause)
                }
                else ->{

                }
            }
        }

        AppMusicUtil.songProgress.observe(this){ percent ->
            val progressBar=binding.pb
            progressBar.setProgress((percent * progressBar.getMax()).toInt())
        }

        binding.pb.setOnSeekBarChangeListener(object :SeekBar.OnSeekBarChangeListener{

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    val percent=seekBar.progress.toFloat() / seekBar.max
                    Intent(this@BindingActivity, MyPlayBackService::class.java).also {
                        it.putExtra(SEEK_POSITION_DATA_KEY, percent.toString())
                        startService(it)
                    }
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {

            }
        })


        Intent(this@BindingActivity, MyPlayBackService::class.java).also {
            //just start it
            startService(it)
        }
       /* AppMusicUtil.pendingSong.value?.also {
            AppMusicUtil.pendingSong.value!!.also {
                binding.tvSong.text=it
                binding.controllerView.isVisible=true
            }
            binding.playPause.setImageResource(R.mipmap.play)
        }*/

/*
        if(AppMusicUtil.pendingSong.value!=null && AppMusicUtil.pendingSong.value!!.equals(recent)){
            AppMusicUtil.pendingSong.value?.also {
                AppMusicUtil.pendingSong.value?.also {
                    binding.tvSong.text=it
                }
            }
        }
        else{



            if(recent.isNullOrEmpty()){
                goneControllerview()
            }else{
                binding.controllerView.isVisible=true
                binding.tvSong.text=recent
            }
        }*/


        binding.playPause.setOnClickListener {
          /*  AppMusicUtil.focusSong.value?.also { s  ->
                Intent(this@BindingActivity,MyPlayBackService::class.java).also {
                    it.putExtra("id",s)
                    startService(it)
                }
                return@setOnClickListener
            }

            val a: String? = if (AppMusicUtil.pendingSong.value.isNullOrEmpty()) null else AppMusicUtil.pendingSong.value
            a?.also { s  ->
                Intent(this@BindingActivity,MyPlayBackService::class.java).also {
                    it.putExtra("id",s)
                    startService(it)
                }
                return@setOnClickListener
            }*/
            Intent(this@BindingActivity, MyPlayBackService::class.java).also {
                it.putExtra(NOTIFICATION_COMMAND_KEY, NOTIFICATION_COMMANE_PLAY_PAUSE)
                startService(it)
            }
        }

    }

}