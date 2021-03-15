package com.example.musicka

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.*
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver

class MediaPlayBackService :MediaBrowserServiceCompat() {


    lateinit var mediaSession: MediaSessionCompat

    var stateBuilder:PlaybackStateCompat.Builder?=null


    companion object{
        val TAG="musicservice"
    }

    override fun onCreate() {
        super.onCreate()

        // Create a MediaSessionCompat
        mediaSession = MediaSessionCompat(baseContext, TAG).apply {

            // Enable callbacks from MediaButtons and TransportControls
            if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.Q){
                setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
            }


            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            setPlaybackState(stateBuilder!!.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            setCallback(callback)

            // Set the session's token so that client activities can communicate with it.
            (sessionToken).also { setSessionToken(it) }

            afChangeListener=object :AudioManager.OnAudioFocusChangeListener{
                override fun onAudioFocusChange(focusChange: Int) {

                }

            }
        }
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return MediaBrowserServiceCompat.BrowserRoot("emptyid",null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult( null)
    }
    val channelId="musicServiceChannelId"

    fun buildNotificationAndStart(){
        // Given a media session and its context (usually the component containing the session)
        // Create a NotificationCompat.Builder

        // Get the session's metadata
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata?.description

        val builder = NotificationCompat.Builder(this, channelId).apply {
            // Add the metadata for the currently playing track
            setContentTitle(description?.title)
            setContentText(description?.subtitle)
            setSubText(description?.description)
            setLargeIcon(description?.iconBitmap)

            // Enable launching the player by clicking the notification
            setContentIntent(controller.sessionActivity)

            // Stop the service when the notification is swiped away
            setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@MediaPlayBackService,
                    PlaybackStateCompat.ACTION_STOP
                )
            )

            // Make the transport controls visible on the lockscreen
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Add an app icon and set its accent color
            // Be careful about the color
            setSmallIcon(R.drawable.ic_launcher_background)
            color = ContextCompat.getColor(this@MediaPlayBackService, R.color.design_default_color_primary_variant)

            // Add a pause button
            addAction(
                NotificationCompat.Action(
                    R.drawable.ic_launcher_background,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this@MediaPlayBackService,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )

            // Take advantage of MediaStyle features
            setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0)

                // Add a cancel button
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this@MediaPlayBackService,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
            )
        }

        var id=1

        // Display the notification and place the service in the foreground
        startForeground(++id, builder.build())

    }
    val music="https://www.bensound.com/bensound-music/bensound-betterdays.mp3"

    /*template*/
    private val intentFilter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY)

    // Defined elsewhere...
    private lateinit var afChangeListener: AudioManager.OnAudioFocusChangeListener
    //todo:receiver code
   // private val myNoisyAudioStreamReceiver = BecomingNoisyReceiver()

    //private lateinit var myPlayerNotification: MediaStyleNotification
    var player: MediaPlayer? = null

    private lateinit var audioFocusRequest: AudioFocusRequest

    private val callback = object: MediaSessionCompat.Callback() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPlay() {

            val context=this@MediaPlayBackService
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Request audio focus for playback, this registers the afChangeListener

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setOnAudioFocusChangeListener(afChangeListener)
                setAudioAttributes(AudioAttributes.Builder().run {
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                build()
            }
            val result = am.requestAudioFocus(audioFocusRequest)
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Start the service
                startService(Intent(context, MediaBrowserService::class.java))
                // Set the session active  (and update metadata and state)
                mediaSession.isActive = true
                // start the player (custom call)

                //TODO 正在播放
                startPlayer()
                // Register BECOME_NOISY BroadcastReceiver
                //registerReceiver(myNoisyAudioStreamReceiver, intentFilter)
                // Put the service in the foreground, post notification
                buildNotificationAndStart()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        override fun onStop() {
            val context=this@MediaPlayBackService
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Abandon audio focus
            am.abandonAudioFocusRequest(audioFocusRequest)
            //unregisterReceiver(myNoisyAudioStreamReceiver)
            // Stop the service

            stopSelf()
            // Set the session inactive  (and update metadata and state)
            mediaSession.isActive = false
            // stop the player (custom call)
            // Take the service out of the foreground
            player?.stop()
            stopForeground(true)
        }

        override fun onPause() {
            val context=this@MediaPlayBackService
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Update metadata and state
            // pause the player (custom call)
            player?.pause()
            // unregister BECOME_NOISY BroadcastReceiver
            //unregisterReceiver(myNoisyAudioStreamReceiver)
            // Take the service out of the foreground, retain the notification
            stopForeground(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player ?: return
        player=null
    }

    private fun startPlayer() {
        val url =music // your URL here
        if(player==null) {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
            }
        }
        player!!.prepare();player!!.start()
    }
}