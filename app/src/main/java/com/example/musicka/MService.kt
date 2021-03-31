package com.example.musicka

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import java.lang.Exception
import kotlin.coroutines.resume


const val CHANNEL_ID = "jsn Music Channel Id" // 通知通道 ID
class MService : Service() {

    val musicController by lazy {
        MusicController(musicRepository)
    }

    val musicRepository by lazy {
        MusicRepository()
    }
    /* 通知管理 */
    private var notificationManager: NotificationManager? = null

    val job = SupervisorJob()

    val scope = CoroutineScope(Dispatchers.Main.immediate + job)

    val mediaPlayer by lazy { MediaPlayer() }

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
        mediaPlayer.release()
        scope.cancel()
        musicController.songChannel.close()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            if (false) {
                audioManager.requestAudioFocus(audioFocusRequest)
            }
        }
    }

    class MusicRepository() {
        val data
            get() = mutableListOf("https://www.bensound.com/bensound-music/bensound-sunny.mp3",
                    "https://www.bensound.com/bensound-music/bensound-sunny.mp3")
    }

    @ExperimentalCoroutinesApi
    inner class MusicController(val musicRepository: MusicRepository) : Binder() {

        val INIT=0 //created nothing playing    init ->  preparing -> playing ->pause ->playing ->completed
        val PLAYING = 1
        val COMPLETED = 2
        val PAUSED = 3
        val PREPARING=5

        val playbackState = MutableLiveData<Int>().apply { value = INIT }

        //todo restore last play
        val pendingSong = MutableLiveData<String>().apply { value = musicRepository.data[0] }

        val currentProcessingSong = MutableLiveData<String>()

        val player
            get() = mediaPlayer

        val musicData = MutableLiveData<List<String>>().apply {
            value = musicRepository.data
        }

        val scope
            get() = this@MService.scope

        val songChannel = ConflatedBroadcastChannel<String>()



        val songFlow = songChannel.asFlow()
                .flatMapLatest {
                    flow {
                        emit(mediaPlayer.prepareSong(it))
                    }
                }


        //重置播放器，播放一首歌曲
        fun playFormStart(song: String) {
            playbackState.value=PREPARING
            currentProcessingSong.value = song
            songChannel.offer(song)
        }

        init {
            scope.launch {
                songFlow.collect {
                    //todo: play it
                    mediaPlayer.start()
                    playbackState.value = PLAYING
                }
            }

            mediaPlayer.setOnCompletionListener {
                //重新播放

                playbackState.value=COMPLETED
                currentProcessingSong.value=null
                songChannel.valueOrNull ?: return@setOnCompletionListener
                (songChannel.valueOrNull!!).also {
                    playFormStart(it) //直接单曲循环
                }
            }

            mediaPlayer.setOnErrorListener { mp: MediaPlayer, what: Int, extra: Int ->
                val message = when (what) {
                    MEDIA_ERROR_IO -> "MEDIA_ERROR_IO"
                    MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED"
                    MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED"
                    MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT"
                    else -> "unknown"
                }
                Toast.makeText(this@MService, message, Toast.LENGTH_SHORT).show()
                false
            }


            fun pause() {
                if(mediaPlayer.isPlaying){
                   mediaPlayer.pause()
                   playbackState.value=PAUSED
                }
            }

            fun reset(){
                (songChannel.valueOrNull!!).also {
                    playFormStart(it) //直接单曲循环
                }
            }
        }

    }

    suspend fun MediaPlayer.prepareSong(song: String): String = suspendCancellableCoroutine { cont ->
        reset()
        setDataSource(song)
        prepareAsync()
        setOnPreparedListener {
            cont.resume(song)
        }
    }

}




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