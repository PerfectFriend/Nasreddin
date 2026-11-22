package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import com.example.R

/**
 * Contract for controlling the closed-loop Eastern background soundtrack.
 * Isolated behind an interface so [com.example.ui.ComicViewModel] stays decoupled from native
 * [MediaPlayer] / [AudioManager] details and remains straightforward to test in JVM/Robolectric.
 */
interface SoundtrackController {
    val currentTrackIndex: Int
    val isActivelyPlaying: Boolean

    fun updatePlaybackState(shouldBePlaying: Boolean)
    fun skipToNextTrack()
    fun release()
}

/**
 * Closed-loop 6-track playlist state machine shared by [AndroidSoundtrackPlayer] and unit tests.
 * Rotates sequentially (`0 -> 1 -> 2 -> 3 -> 4 -> 5 -> 0`) so only a single OGG track is kept
 * active in native memory at any time.
 */
internal class ClosedLoopPlaylistState(
    val trackResIds: List<Int>
) {
    var currentIndex: Int = 0
        private set

    var consecutiveErrors: Int = 0
        private set

    fun currentTrackResId(): Int? = trackResIds.getOrNull(currentIndex)

    fun advanceToNextTrack(): Int? {
        if (trackResIds.isEmpty()) return null
        currentIndex = (currentIndex + 1) % trackResIds.size
        return trackResIds[currentIndex]
    }

    fun markPlaybackStarted() {
        consecutiveErrors = 0
    }

    /**
     * Records a decode/playback failure and advances to the next track unless every track in the
     * playlist has failed consecutively (prevents infinite error loops).
     */
    fun onTrackErrorShouldRetry(): Boolean {
        if (trackResIds.isEmpty()) return false
        consecutiveErrors += 1
        if (consecutiveErrors >= trackResIds.size) {
            return false
        }
        advanceToNextTrack()
        return true
    }
}

/**
 * Lifecycle-safe Android [MediaPlayer] + [AudioManager] soundtrack engine playing all 6 compressed
 * OGG Vorbis "Tashkent Market" tracks in an infinite closed loop at a gentle background level.
 *
 * Key safety invariants:
 * - Loads strictly one compressed `.ogg` track at a time (`setNextMediaPlayer` not duplicated in RAM).
 * - Requests `AUDIOFOCUS_GAIN` before playback and abandons audio focus on pause/background/release.
 * - Ducks volume smoothly on `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` and pauses cleanly on focus loss.
 * - Every native audio call is guarded against device/codec/Robolectric anomalies so UI never crashes.
 */
class AndroidSoundtrackPlayer(
    context: Context,
    trackResIds: List<Int> = DEFAULT_TRACKS
) : SoundtrackController {

    private val appContext = context.applicationContext
    private val audioManager: AudioManager? =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val playlist = ClosedLoopPlaylistState(trackResIds)

    private var mediaPlayer: MediaPlayer? = null
    private var preparedTrackResId: Int? = null
    private var isPrepared: Boolean = false

    private var wantsPlayback: Boolean = false
    private var hasAudioFocus: Boolean = false
    private var isDucked: Boolean = false
    private var pausedByFocusLoss: Boolean = false

    override val currentTrackIndex: Int
        get() = playlist.currentIndex

    override val isActivelyPlaying: Boolean
        get() = try {
            mediaPlayer?.isPlaying == true
        } catch (_: Throwable) {
            false
        }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                isDucked = false
                applyVolume()
                if (wantsPlayback && pausedByFocusLoss) {
                    pausedByFocusLoss = false
                    startOrPrepareCurrentTrack()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                pausedByFocusLoss = true
                pauseInternal(abandonFocus = false)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                isDucked = true
                applyVolume()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                pausedByFocusLoss = false
                pauseInternal(abandonFocus = true)
            }
        }
    }

    private val audioFocusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(buildMusicAudioAttributes())
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
        } else {
            null
        }

    override fun updatePlaybackState(shouldBePlaying: Boolean) {
        wantsPlayback = shouldBePlaying
        if (shouldBePlaying) {
            pausedByFocusLoss = false
            if (requestFocusIfNeeded()) {
                startOrPrepareCurrentTrack()
            }
        } else {
            pausedByFocusLoss = false
            isDucked = false
            pauseInternal(abandonFocus = true)
        }
    }

    override fun skipToNextTrack() {
        playlist.advanceToNextTrack()
        releaseCurrentPlayer()
        if (wantsPlayback && requestFocusIfNeeded()) {
            startOrPrepareCurrentTrack()
        }
    }

    override fun release() {
        wantsPlayback = false
        pausedByFocusLoss = false
        isDucked = false
        abandonFocus()
        releaseCurrentPlayer()
    }

    private fun startOrPrepareCurrentTrack() {
        val targetResId = playlist.currentTrackResId() ?: return

        val existing = mediaPlayer
        if (existing != null && preparedTrackResId == targetResId && isPrepared) {
            try {
                applyVolume()
                if (!existing.isPlaying) {
                    existing.start()
                }
                playlist.markPlaybackStarted()
                return
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to resume existing MediaPlayer; recreating track", t)
                releaseCurrentPlayer()
            }
        } else if (existing != null) {
            releaseCurrentPlayer()
        }

        prepareAndPlayTrack(targetResId)
    }

    private fun prepareAndPlayTrack(trackResId: Int) {
        var afd: android.content.res.AssetFileDescriptor? = null
        val player = MediaPlayer()
        try {
            afd = appContext.resources.openRawResourceFd(trackResId)
            if (afd == null) {
                Log.w(TAG, "Missing raw resource descriptor for track=$trackResId")
                safeRelease(player)
                handleTrackFailure()
                return
            }

            player.setAudioAttributes(buildMusicAudioAttributes())
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

            player.setOnCompletionListener {
                // Advance cleanly around the closed 6-track loop (`1 -> 2 -> ... -> 6 -> 1`)
                playlist.advanceToNextTrack()
                releaseCurrentPlayer()
                if (wantsPlayback && hasAudioFocus) {
                    startOrPrepareCurrentTrack()
                }
            }

            player.setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra on track=$trackResId")
                releaseCurrentPlayer()
                handleTrackFailure()
                true
            }

            player.prepare()
            mediaPlayer = player
            preparedTrackResId = trackResId
            isPrepared = true
            applyVolume()

            if (wantsPlayback && hasAudioFocus) {
                player.start()
                playlist.markPlaybackStarted()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Silent fallback: unable to prepare/start soundtrack track=$trackResId", t)
            safeRelease(player)
            if (mediaPlayer === player) {
                mediaPlayer = null
                preparedTrackResId = null
                isPrepared = false
            }
            handleTrackFailure()
        } finally {
            try {
                afd?.close()
            } catch (_: Throwable) {
            }
        }
    }

    private fun handleTrackFailure() {
        if (!wantsPlayback || !hasAudioFocus) return
        if (playlist.onTrackErrorShouldRetry()) {
            val nextResId = playlist.currentTrackResId() ?: return
            prepareAndPlayTrack(nextResId)
        } else {
            abandonFocus()
        }
    }

    private fun pauseInternal(abandonFocus: Boolean) {
        try {
            val player = mediaPlayer
            if (player != null && isPrepared && player.isPlaying) {
                player.pause()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to pause MediaPlayer cleanly", t)
        }
        if (abandonFocus) {
            abandonFocus()
        }
    }

    private fun applyVolume() {
        val level = if (isDucked) DUCKED_VOLUME else DEFAULT_VOLUME
        try {
            if (isPrepared) {
                mediaPlayer?.setVolume(level, level)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to set MediaPlayer volume", t)
        }
    }

    private fun requestFocusIfNeeded(): Boolean {
        if (hasAudioFocus) return true
        val manager = audioManager ?: run {
            hasAudioFocus = true
            return true
        }
        val result = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                manager.requestAudioFocus(audioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "AudioFocus request threw unexpectedly; continuing silently", t)
            AudioManager.AUDIOFOCUS_REQUEST_FAILED
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonFocus() {
        if (!hasAudioFocus) return
        hasAudioFocus = false
        val manager = audioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                manager.abandonAudioFocusRequest(audioFocusRequest)
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to abandon AudioFocus", t)
        }
    }

    private fun releaseCurrentPlayer() {
        val player = mediaPlayer
        mediaPlayer = null
        preparedTrackResId = null
        isPrepared = false
        safeRelease(player)
    }

    private fun safeRelease(player: MediaPlayer?) {
        if (player == null) return
        try {
            if (player.isPlaying) {
                player.stop()
            }
        } catch (_: Throwable) {
        }
        try {
            player.reset()
        } catch (_: Throwable) {
        }
        try {
            player.release()
        } catch (_: Throwable) {
        }
    }

    private fun buildMusicAudioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    companion object {
        private const val TAG = "SoundtrackPlayer"
        const val DEFAULT_VOLUME = 0.22f
        const val DUCKED_VOLUME = 0.06f

        val DEFAULT_TRACKS: List<Int> = listOf(
            R.raw.soundtrack_tashkent_01,
            R.raw.soundtrack_tashkent_02,
            R.raw.soundtrack_tashkent_03,
            R.raw.soundtrack_tashkent_04,
            R.raw.soundtrack_tashkent_05,
            R.raw.soundtrack_tashkent_06
        )
    }
}
