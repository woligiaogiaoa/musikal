package com.example.musicka

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.MediaPlayer.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import java.lang.Exception
import java.lang.ref.WeakReference
import kotlin.coroutines.resume


const val CHANNEL_ID = "jsn Music Channel Id" // 通知通道 ID

@ExperimentalCoroutinesApi
@FlowPreview
object AppMusicUtil{

    var controller: WeakReference<MyPlayBackService.ServiceMusicController>? =null

    set(value) {
        field=value
        value?.also {
            it.get()!!.playbackState.observeForever {
                playbackState.value=it
            }
            it.get()!!.pendingSong.observeForever {
                pendingSong.value=it
            }
            it.get()!!.songFocus.observeForever {
                focusSong.value=it
            }
        }
    }

    val musicBinder: MyPlayBackService.ServiceMusicController?
        get() = controller?.get()

    val playbackState=MutableLiveData<Int>()

    val pendingSong=MutableLiveData<String>()

    val focusSong=MutableLiveData<String>() //channel song
}

@ExperimentalCoroutinesApi
@FlowPreview
class MyPlayBackService : Service() {


    val musicController by lazy {
        ServiceMusicController().also {
            AppMusicUtil.controller= WeakReference(it)
        }
    }

    val musicRepository by lazy {
        MusicRepository()
    }
    /* 通知管理 */
    private var notificationManager: NotificationManager? = null

    val job = SupervisorJob()

    val scope = CoroutineScope(Dispatchers.Main.immediate + job)

    var mediaPlayer: MediaPlayer? =MediaPlayer()

    override fun onBind(intent: Intent?): IBinder? {
        return musicController
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager // 通知管理
        //updateNotification(false)
        initChannel()
        initAudioFocus()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicController.playbackState.value= COMPLETED
        musicController.songFocus.value=null
        mediaPlayer?.release()
        scope.cancel()
        musicController.songChannel.close()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("id")?.also {
            if(!it.isEmpty() ){
                musicController.playPauseOrPlayANew(it)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun initChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "jsn Music Notification"
            val descriptionText = "jsn Music 音乐通知"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = descriptionText
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /* 音频管理 */
    private lateinit var audioManager: AudioManager

    /* AudioAttributes */
    private lateinit var audioAttributes: AudioAttributes

    /* AudioFocusRequest */
    private lateinit var audioFocusRequest: AudioFocusRequest

    fun initAudioFocus() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener { focusChange ->
                        when (focusChange) {
                            // AudioManager.AUDIOFOCUS_GAIN -> musicBinder.play()
                            // AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> musicBinder.play()
                            // AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> musicBinder.play()
                            AudioManager.AUDIOFOCUS_LOSS -> {
                                // audioManager.abandonAudioFocusRequest(audioFocusRequest)
                                //musicController.pause()
                            }
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ->{} //musicController.pause()
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->{} //musicController.pause()
                        }
                    }.build()
            if (true) {
                audioManager.requestAudioFocus(audioFocusRequest)
            }
        }
    }

    class MusicRepository() {
        val data
            get() = mutableListOf("http://isure.stream.qqmusic.qq.com/C400003zDTau0boSQm.m4a?guid=2958323637&vkey=72B5A322351DCFB5B1FF4C3013479DF80E8EC61E196DB7C407BA21BCAA529E820A940220AECBBA553F67118D1805A16579C01AE30C2291D0&uin=3203891186&fromtag=66",
                "http://m701.music.126.net/20210331211701/add5c3fd6f323a2d2b0e8e7f751fa20a/jdymusic/obj/wo3DlMOGwrbDjj7DisKw/4959745806/482a/1a84/ca27/02c0f32c8c1b78a97988cebbee2ede1b.mp3")
    }


    companion object{
        //not playing idle
        val INIT=0 //created ,nothing playing , init ->  preparing -> playing ->pause ->playing ->completed
        val PLAYING = 1
        val COMPLETED = 2 //idle
        val PAUSED = 3
        val PREPARING=5


        /*REPEAT*/
        val REAPEAT=6


        /*RECENT PLAY*/
        val RECENT="RECENT_SONG"

    }



    @ExperimentalCoroutinesApi
    inner class ServiceMusicController : Binder() {

        val mode= REAPEAT

        val musicRepository
        get() = this@MyPlayBackService.musicRepository

        val sp by lazy {
            PreferenceManager.getDefaultSharedPreferences(this@MyPlayBackService)
        }


        val playbackState = MutableLiveData<Int>().apply { value = INIT }

        //todo restore last play
        val pendingSong = MutableLiveData<String?>().apply { sp.getString(RECENT,"") }

        fun saveRecent(s:String)=sp.edit { putString(RECENT,s) }

        val currentProcessingSong = MutableLiveData<String?>()


        val musicData = MutableLiveData<List<String>>().apply {
            value = musicRepository.data
        }

        fun getService()=this@MyPlayBackService

        val scope
            get() = this@MyPlayBackService.scope

        val songChannel = ConflatedBroadcastChannel<String>()



        val songFlow = songChannel.asFlow()
                .flatMapLatest {
                    flow {
                        emit(mediaPlayer!!.prepareSong(it))
                    }
                }

        val songFocus=MutableLiveData<String>()

        val TAG="music controller"

        val songInChannel
        get() = songChannel.valueOrNull

        fun playIfNotPlay(song:String){
            if(mediaPlayer!!.isPlaying ){
                if(song.equals(songChannel.valueOrNull!!)){
                    return
                }else{
                    playPauseOrPlayANew(song)
                }
            }
            else{ //not playing
                if(playbackState.value == PAUSED){
                    if(songInChannel!!.equals(song)){
                        playPauseOrPlayANew(song)
                    }else{
                        playPauseOrPlayANew(song)
                    }
                }else{
                    playPauseOrPlayANew(song)
                }
            }

        }

        //change State
        fun playPauseOrPlayANew(song:String){

            Log.e(TAG, "handleSongClick: ", )
            val  hasSong= !songChannel.valueOrNull.isNullOrEmpty()
            if(mediaPlayer!!.isPlaying && hasSong){  //playing
                if(song.equals(songChannel.value)){
                    //pause
                    playbackState.value=PAUSED
                    songFocus.value=song
                    mediaPlayer!!.pause()
                    updateNotification(song, PAUSED)
                    return
                }else{
                    playFormStart(song) //preparing
                    return
                }
            }
            else if(!hasSong){
                playFormStart(song)
                songFocus.value=song
                return
            }else{
                // has song
                if(playbackState.value==PAUSED && song.equals(songChannel.valueOrNull!!)){
                    songFocus.value=song
                    playbackState.value=PLAYING
                    updateNotification(song, PLAYING)
                    mediaPlayer!!.start()
                    return
                }
                else{
                    playFormStart(song)
                    songFocus.value=song
                }
            }
        }

        var isError=false


        fun playFormStart(song: String) {
            isError=false
            songFocus.value=song
            playbackState.value=PREPARING
            updateNotification(song, PREPARING)
            songFocus.value=song
            pendingSong.value= song.also { saveRecent(it) }
            currentProcessingSong.value = song
            songChannel.offer(song)
        }

        fun pause() {
            if(mediaPlayer!!.isPlaying){
                mediaPlayer!!.pause()
                playbackState.value=PAUSED
            }
        }

        fun reset(){
            (songChannel.valueOrNull!!).also {
                playFormStart(it) //直接单曲循环
            }
        }

        val completeListener=object :OnCompletionListener{
            override fun onCompletion(mp: MediaPlayer?) {
                playbackState.value = COMPLETED //error accur or completed
                currentProcessingSong.value = null
                songChannel.valueOrNull ?: return
                (songChannel.valueOrNull!!).also {
                    pendingSong.value = it //未播放的时候使用
                   saveRecent(it)
                }
                if(!isError){
                    if(mode== REAPEAT){
                        playFormStart(songChannel.valueOrNull!!)
                    }
                }
            }
        }

        val error = OnErrorListener { mp, what, extra ->
            val message = when (what) {
                MEDIA_ERROR_IO -> "MEDIA_ERROR_IO"
                MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED"
                MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED"
                MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT"
                else -> "unknown"
            }
            isError=true
            Toast.makeText(this@MyPlayBackService, message, Toast.LENGTH_SHORT).show()
            false
        }

        init {

            scope.launch {
                songFlow.collect {
                    //todo: play it
                    pendingSong.value=it
                    saveRecent(it)
                    mediaPlayer!!.start()
                    playbackState.value = PLAYING
                    updateNotification(it, PLAYING)
                }
            }

        }

        suspend fun MediaPlayer.prepareSong(song: String): String = suspendCancellableCoroutine { cont ->
            mediaPlayer?.reset()
            mediaPlayer?.release()
            mediaPlayer=null
            mediaPlayer=MediaPlayer().apply {
                setDataSource(song)
                prepareAsync()
                setOnPreparedListener {
                    cont.resume(song)
                }
                setOnCompletionListener(completeListener)
                setOnErrorListener (error)
            }
        }


    }


    private fun getPendingIntentActivity(): PendingIntent {
        val intentMain = Intent(this, BindingActivity::class.java)
        return PendingIntent.getActivity(this, 1, intentMain, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /**
     * 显示通知
     */
    private fun updateNotification(song: String?,state:Int) {

        Log.e("state", "updateNotification:${state}", )
        val mipmap=ContextCompat.getDrawable(this@MyPlayBackService,if(state== PLAYING || state== PREPARING) R.mipmap.pause else R.mipmap.play )!!.toBitmap()

        val fromLyric=false
        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
            setLargeIcon(mipmap)
            setContentTitle(song)
            setContentText(song)
            setContentIntent(getPendingIntentActivity())
            /*addAction(R.drawable.ic_baseline_skip_previous_24, "Previous", getPendingIntentPrevious())
            addAction(getPlayIcon(), "play", getPendingIntentPlay())
            addAction(R.drawable.ic_baseline_skip_next_24, "next", getPendingIntentNext())*/
          /*  setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession?.sessionToken)
                            .setShowActionsInCompactView(0, 1, 2)
            )*/
            setOngoing(true)
         /*   if (getCurrentLineLyricEntry()?.text != null && fromLyric && musicController.statusBarLyric) {
                setTicker(getCurrentLineLyricEntry()?.text) // 魅族状态栏歌词的实现方法
            }*/
            // .setAutoCancel(true)
        }.build()
        startForeground(START_FOREGROUND_ID, notification)
    }


}

const val START_FOREGROUND_ID = 10 // 开启前台服务的 ID



    sealed class Result<out T> {

        data class Success<T>(val data: T) : Result<T>() {

        }

        data class Error(val error: Exception) : Result<Nothing>() {

        }

        object Loading : Result<Nothing>()


        override fun toString(): String {
            return when (this) {
                is Success -> "success:${data}"
                is Error -> "error:${error}"
                Loading -> "loading"
            }
        }

    }

    fun <T> Result<T>.successOrNull(): T? {
        return when (this) {
            is Result.Success -> this.data
            is Result.Error -> null
            Result.Loading -> null
        }
    }