package com.example.musicka

import android.media.Rating
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.service.media.MediaBrowserService
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MediaPlayBackService :MediaBrowserService() {


    lateinit var mediaSession: MediaSession

    var stateBuilder:PlaybackState.Builder?=null

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return MediaBrowserService.BrowserRoot("emptyid",null)
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowser.MediaItem>>) {
        p1.sendResult(null)
    }

    companion object{
        val TAG="musicservice"
    }

    override fun onCreate() {
        super.onCreate()

        // Create a MediaSessionCompat
        mediaSession = MediaSession(baseContext, TAG).apply {

            // Enable callbacks from MediaButtons and TransportControls
            if(Build.VERSION.SDK_INT<=Build.VERSION_CODES.Q){
                setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                        or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
                )
            }


            // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
            stateBuilder = PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY
                        or PlaybackState.ACTION_PLAY_PAUSE
                )
            setPlaybackState(stateBuilder!!.build())

            // MySessionCallback() has methods that handle callbacks from a media controller
            setCallback(MySessionCallback())

            // Set the session's token so that client activities can communicate with it.
            setSessionToken(sessionToken)
        }
    }

    class MySessionCallback:MediaSession.Callback(){
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

        override fun onSetRating(rating: Rating) {
            super.onSetRating(rating)
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
                    context,
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
                        PlaybackState.ACTION_PLAY_PAUSE
                    )
                )
            )

            // Take advantage of MediaStyle features
            setStyle(NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0)

                // Add a cancel button
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
            )
        }

        // Display the notification and place the service in the foreground
        startForeground(id, builder.build())

    }
}