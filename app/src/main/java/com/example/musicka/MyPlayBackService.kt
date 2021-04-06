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
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import java.lang.ref.WeakReference
import kotlin.coroutines.resume


class MusicRepository() {
    val data
        get() =
            mutableListOf("http://isure.stream.qqmusic.qq.com/C400000eMCTT1akCEg.m4a?guid=2958323637&vkey=E1DE81B861F2F25D50F4D5E38BF13E9E4C54A64D3BA6932B78833BADB389413603091FE41C24EACFF7F5D044BEBFF5D6C9EDF6D6BCB5D067&uin=3203891186&fromtag=66",
            "http://isure.stream.qqmusic.qq.com/C4000048kEIo4SEMnw.m4a?guid=2958323637&vkey=EEC48A32D4931052B5E11B3EF15C00A9BE78B44E78954B3C1C141DD5C82ABE7D03BBDACEDE7CE17EA908123CECA32A8FEF856206A2331741&uin=3203891186&fromtag=66")
}


const val CHANNEL_ID = "jsn Music Channel Id" // 通知通道 ID

@ExperimentalCoroutinesApi
@FlowPreview
object AppMusicUtil{

    var controller: WeakReference<MyPlayBackService.ServiceMusicController>? =null

    set(value) {
        field=value
        value?.also {
            it.get()!!.pendingSong.observeForever {
                pendingSong.value=it
            }
            it.get()!!.playbackState.observeForever {
                playbackState.value=it
            }
            it.get()!!.songFocus.observeForever {
                focusSong.value=it
            }
            it.get()!!.songProgress.observeForever {
                songProgress.value=it
            }
        }
    }

    val playbackState=MutableLiveData<Int>()
    val songProgress=MutableLiveData<Float>()

    val pendingSong=MutableLiveData<String?>()

    val focusSong=MutableLiveData<String>() //channel song
}

@Suppress("DEPRECATION")
@ExperimentalCoroutinesApi
@FlowPreview
class MyPlayBackService : Service() {


    val musicController= ServiceMusicController().also {
            AppMusicUtil.controller= WeakReference(it)
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
        initChannel()
        initAudioFocus()
        musicController.lauchSongFlow()
    }

    override fun onDestroy() {
        super.onDestroy()

        musicController.playbackState.value= COMPLETED


        musicController.songFocus.value?.also{
            musicController.pendingSong.value=it //recent play
        }


        musicController.songFocus.value=null
        musicController.songProgress.value=0f
        mediaPlayer?.release()
        scope.cancel()
        musicController.songChannel.close()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.getStringExtra("id")?.also {   //A NEW SONG CLICK FROM OTHER UI 比如歌单
            if(!it.isEmpty() ){
                musicController.playThisSongOrPauseItIfPlaying(it)
            }
        }
        
        intent?.getStringExtra(SEEK_POSITION_DATA_KEY)?.also {   //seekto
            if(!it.isEmpty() ){
                musicController.seekto(it)
            }
        }

        intent?.getStringExtra(NOTIFICATION_COMMAND_KEY)?.also {   //通知点击 play pause
            if(it.equals(NOTIFICATION_COMMANE_PLAY_PAUSE) ){
                if(musicController.songFocus.value!=null){
                    musicController.playThisSongOrPauseItIfPlaying(musicController.songFocus.value!!)
                    return@also
                }
                if(musicController.pendingSong.value!=null){
                    musicController.playThisSongOrPauseItIfPlaying(musicController.pendingSong.value!!)
                    return@also
                }
            }
        }

        return START_NOT_STICKY
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
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            } //musicController.pause()
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            } //musicController.pause()
                        }
                    }.build()
            if (true) {
                audioManager.requestAudioFocus(audioFocusRequest)
            }
        }
    }




    companion object{

        /*-------------------PLAY STATE -0----------------*/
        //not playing idle
        val INIT=0 //created ,nothing playing , init ->  preparing -> playing ->pause ->playing ->completed
        val PLAYING = 1
        val COMPLETED = 2 //idle
        val PAUSED = 3
        val PREPARING=5

        /*--------------COMMAND CODE FOR NOTIFICATION--------------*/
        //CODE_KEY
        val NOTIFICATION_COMMAND_KEY="COMMAND"
        val NOTIFICATION_COMMANE_PLAY_PAUSE="PLAY_PAUSE"

        val SEEK_POSITION_DATA_KEY="SEEK_POSITION_DATA_KEY"





        /*--------------------PLAY MODE----------------*/
        /*REPEAT*/
        val REAPEAT=6


        /*--------------PERSISTENT DATA KEY------------------ */
        /*RECENT PLAY*/
        val RECENT="RECENT_SONG"

    }



    @ExperimentalCoroutinesApi
    inner class
    ServiceMusicController : Binder() {

        val mode= REAPEAT

        val sp = PreferenceManager.getDefaultSharedPreferences(MyApp.app)


        //todo restore last play
        val pendingSong = MutableLiveData<String?>().apply { value=sp.getString(RECENT, null) }

        val playbackState = MutableLiveData<Int>().apply { value = INIT }

        fun saveRecent(s: String)=sp.edit { putString(RECENT, s) }

        val currentProcessingSong = MutableLiveData<String?>()




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

        fun playIfNotPlay(song: String){
            if(mediaPlayer!!.isPlaying ){
                if(song.equals(songChannel.valueOrNull!!)){
                    return
                }else{
                    playThisSongOrPauseItIfPlaying(song)
                }
            }
            else{ //not playing
                if(playbackState.value == PAUSED){
                    if(songInChannel!!.equals(song)){
                        playThisSongOrPauseItIfPlaying(song)
                    }else{
                        playThisSongOrPauseItIfPlaying(song)
                    }
                }else{
                    playThisSongOrPauseItIfPlaying(song)
                }
            }

        }

        val isActive
        get() = playbackState.value == PLAYING || playbackState.value== PAUSED

        fun seekto(percent:String){
            val percent=percent.toFloat()
            if(isActive) mediaPlayer!!.seekTo((percent*mediaPlayer!!.duration).toInt().also {
                Log.e(TAG, "seekto: $it", )
                mediaPlayer?.start()
                playbackState.value= PLAYING
            })else if(playbackState.value== PREPARING){
                return
            }else if(playbackState.value== INIT){
                if(pendingSong.value!=null)
                playThisSongOrPauseItIfPlaying(pendingSong.value!!)
            }
            else if (playbackState.value== COMPLETED){
                if(songFocus.value!=null)
                    playThisSongOrPauseItIfPlaying(songFocus.value!!)
            }else{
                return
            }
        }

        //change State
        fun playThisSongOrPauseItIfPlaying(song: String){

            Log.e(TAG, "handleSongClick: ")
            val  hasSong= !songChannel.valueOrNull.isNullOrEmpty()
            if(mediaPlayer!!.isPlaying && hasSong){  //playing
                if(song.equals(songChannel.value)){
                    //pause
                    playbackState.value=PAUSED
                    songFocus.value=song
                    pendingSong.value=song
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
                pendingSong.value=song
                return
            }else{
                // has song not playing
                if(playbackState.value==PAUSED && song.equals(songChannel.valueOrNull!!)){
                    songFocus.value=song
                    pendingSong.value=song
                    playbackState.value=PLAYING
                    updateNotification(song, PLAYING)
                    mediaPlayer!!.start()
                    return
                }
                else{
                    if(playbackState.value== PREPARING){
                        return
                    }else{
                        playFormStart(song)
                        songFocus.value=song
                        pendingSong.value=song
                    }

                }
            }
        }

        var isError=false

        var job:Job?=null

        val songProgress=MutableLiveData<Float>().apply { value=0f }

        fun playFormStart(song: String) {
            queryProgress()
            isError=false
            songFocus.value=song
            playbackState.value=PREPARING
            updateNotification(song, PREPARING)
            songFocus.value=song
            pendingSong.value= song.also { saveRecent(it) }
            currentProcessingSong.value = song
            songChannel.offer(song)
        }

        private fun queryProgress() {
            job = scope.launch {
                try {
                    while (true) {
                        ensureActive()
                        delay(80)
                        var progress=0f
                        if(playbackState.value== COMPLETED){
                            if(isError) progress=0f
                            else progress=1f
                        }
                        if(playbackState.value== PREPARING) progress=0f

                        if(playbackState.value==PLAYING || playbackState.value== PAUSED){
                            progress=mediaPlayer!!.currentPosition.toFloat() / mediaPlayer!!.duration
                        }
                        updateNotification(songFocus.value,playbackState.value!!,progress)
                        songProgress.value=progress
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "queryProgress: ${e.message}", )
                }

            }
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
                updateNotification(songFocus.value!!, playbackState.value!!)
                currentProcessingSong.value = null
                songChannel.valueOrNull ?: return
                (songChannel.valueOrNull!!).also {
                    pendingSong.value = it //未播放的时候使用
                   saveRecent(it)
                }
                job?.cancel()
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

        fun lauchSongFlow() {

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
                setOnErrorListener(error)
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
    private fun updateNotification(song: String?, state: Int,progress:Float?=0f) {

        val active= (state== PLAYING || state== PREPARING)

        Log.e("state", "updateNotification:${state}")
        val mipmap=ContextCompat.getDrawable(this@MyPlayBackService, if (state == PLAYING || state == PREPARING) R.mipmap.pause else R.mipmap.play)!!.toBitmap()

        // Get the layouts to use in the custom notification
        val notificationLayout = RemoteViews(packageName, R.layout.notification_small).apply {
        }
        val notificationLayoutExpanded = RemoteViews(packageName, R.layout.notification_large)

// Apply the layouts to the notification
        //val customNotification = NotificationCompat.Builder(context, CHANNEL_ID)
                //.setSmallIcon(R.drawable.notification_icon)

        fun getPlayIcon(): Int {
            return if (active) {
                R.drawable.pause
            } else {
                R.drawable.play
            }
        }
         fun getPendingIntentPlay(): PendingIntent {
            val intent = Intent(this, MyPlayBackService::class.java)
            intent.putExtra(NOTIFICATION_COMMAND_KEY, NOTIFICATION_COMMANE_PLAY_PAUSE)
            return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }


        val notification = NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(R.drawable.ic_launcher_background)
                    setLargeIcon(mipmap)
                    /*.setCustomContentView(notificationLayout)
                            .addAction(getPlayIcon(), "play", getPendingIntentPlay())
                    .setCustomBigContentView(notificationLayoutExpanded)*/
            setContentTitle(musicController.songInChannel)
            setContentText(musicController.songInChannel)
            setContentIntent(getPendingIntentActivity())
                    .build()
            addAction(getPlayIcon(), "play", getPendingIntentPlay())
            /*  setStyle(
                      androidx.media.app.NotificationCompat.MediaStyle()
                              .setMediaSession(mediaSession?.sessionToken)
                              .setShowActionsInCompactView(0, 1, 2)
              )*/
            setOngoing(true)

        }.build()


        if(active){
            startForeground(START_FOREGROUND_ID, notification)
        }else{
            //startForeground(START_FOREGROUND_ID, notification)
                notificationManager?.notify(START_FOREGROUND_ID, notification)
            stopForeground(false)
        }

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

