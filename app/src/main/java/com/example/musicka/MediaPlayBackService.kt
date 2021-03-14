package com.example.musicka

import android.app.Notification
import android.media.Rating
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompatSideChannelService
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
            setCallback(MySessionCallback())

            // Set the session's token so that client activities can communicate with it.
            (sessionToken).also { setSessionToken(it) }
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
        result.sendResult( null
        )
    }

    /*override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return MediaBrowserService.BrowserRoot("emptyid",null)
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowser.MediaItem>>) {
        p1.sendResult(null)
    }*/

    class MySessionCallback:MediaSessionCompat.Callback(){
        override fun onPrepare() {
            super.onPrepare()
        }

        override fun onPlay() {
            super.onPlay()
        }

        override fun onPause() {
            super.onPause()
        }

        override fun onStop() {
            super.onStop()
        }

        override fun onSeekTo(pos: Long) {
            super.onSeekTo(pos)
        }


        override fun onSetPlaybackSpeed(speed: Float) {
            super.onSetPlaybackSpeed(speed)
        }
    }
    val channelId="musicServiceChannelId"

    fun buildNotification(){
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
}