package com.audion.player.engine

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.audion.player.model.AudioTrack
import com.audion.player.model.MusicFolder

class AudioPlayerEngine(
    context: Context,
    private val onStateChanged: (track: AudioTrack?, isPlaying: Boolean, folderName: String?) -> Unit
) {

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()
    private var currentTracks: List<AudioTrack> = emptyList()
    private var currentFolderName: String? = null
    private val seekStepMs: Long = 10_000L

    init {
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                notifyCurrentState()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                notifyCurrentState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                notifyCurrentState()
            }
        })
    }

    fun playFolder(folder: MusicFolder, startIndex: Int = 0) {
        if (folder.tracks.isEmpty()) return

        currentFolderName = folder.folderName
        currentTracks = folder.tracks

        val mediaItems = currentTracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }

        val safeIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
        player.setMediaItems(mediaItems, safeIndex, 0L)
        player.prepare()
        player.play()
        notifyCurrentState()
    }

    fun togglePlayPause() {
        if (currentTracks.isEmpty()) return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
        notifyCurrentState()
    }

    fun nextTrack() {
        if (currentTracks.isEmpty()) return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        }
    }

    fun previousTrack() {
        if (currentTracks.isEmpty()) return
        if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else {
            player.seekTo(0L)
            player.play()
        }
    }

    fun forward() {
        if (currentTracks.isEmpty()) return
        val duration = player.duration
        val targetPosition = if (duration > 0L) {
            (player.currentPosition + seekStepMs).coerceAtMost(duration)
        } else {
            player.currentPosition + seekStepMs
        }
        player.seekTo(targetPosition)
    }

    fun rewind() {
        if (currentTracks.isEmpty()) return
        val targetPosition = (player.currentPosition - seekStepMs).coerceAtLeast(0L)
        player.seekTo(targetPosition)
    }

    fun getCurrentTrack(): AudioTrack? {
        val index = player.currentMediaItemIndex
        return currentTracks.getOrNull(index)
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun release() {
        player.release()
    }

    private fun notifyCurrentState() {
        onStateChanged(getCurrentTrack(), player.isPlaying, currentFolderName)
    }
}