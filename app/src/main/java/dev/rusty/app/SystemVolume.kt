package dev.rusty.app

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

/**
 * The device's media stream, used as the receiver's one and only volume.
 *
 * Rusty hands the Connect volume to Android: the native mixer reports what Spotify asked for (so
 * the apps' slider stays truthful) but does not attenuate the samples — this stream does. The
 * hardware volume keys, the remote-control page and Spotify's own slider therefore all describe
 * the same level, and it survives casts because Android remembers the stream volume.
 *
 * The index/percent mapping is pure so the rounding is unit-testable without an AudioManager.
 */
object SystemVolume {
    /** [AudioManager.STREAM_MUSIC] — the stream the native AudioTrack writes to. */
    const val STREAM = AudioManager.STREAM_MUSIC

    /** Current media volume as 0..100, or null when the platform reports a fixed stream. */
    fun percent(context: Context): Int? = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return null
        if (audio.isVolumeFixed) return null
        toPercent(audio.getStreamVolume(STREAM), audio.getStreamMaxVolume(STREAM))
    }.getOrNull()

    /** Sets the media volume to the nearest step for [percent]. False when unavailable or fixed. */
    fun setPercent(context: Context, percent: Int): Boolean = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        val max = audio.getStreamMaxVolume(STREAM)
        if (audio.isVolumeFixed || max <= 0) return false
        audio.setStreamVolume(STREAM, toIndex(percent, max), 0)
        true
    }.getOrDefault(false)

    /**
     * Extra software attenuation for the bottom of the stream, as an amplitude ratio for
     * `AudioTrack.setVolume`. Android's lowest audible step is still room-filling on an Echo
     * Show, so the first ~20% of the range is scaled down; the rest is untouched. Pure so the
     * curve can be unit-tested without an AudioManager.
     */
    fun outputGain(index: Int, max: Int): Float {
        if (max <= 1) return 1f
        val fraction = index.toFloat() / max
        return when {
            fraction <= 0.07f -> 0.15f   // the stream's minimum audible step
            fraction <= 0.14f -> 0.4f
            fraction <= 0.21f -> 0.7f
            else -> 1f
        }
    }

    /** Software attenuation matching the current media-stream step. */
    fun outputGain(context: Context): Float = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return 1f
        if (audio.isVolumeFixed) return 1f
        outputGain(audio.getStreamVolume(STREAM), audio.getStreamMaxVolume(STREAM))
    }.getOrDefault(1f)

    /** Stream index -> 0..100, or null for a bogus range. */
    fun toPercent(index: Int, max: Int): Int? {
        if (max <= 0) return null
        return (index.toDouble() / max * 100.0).roundToInt().coerceIn(0, 100)
    }

    /**
     * 0..100 -> the nearest stream index in 0..[max]. A non-zero percent never lands on 0:
     * Spotify's 1% should be the quietest step, not mute (the taper in [outputGain] makes it
     * genuinely quiet), while 0% still is mute.
     */
    fun toIndex(percent: Int, max: Int): Int {
        if (max <= 0) return 0
        val clamped = percent.coerceIn(0, 100)
        if (clamped == 0) return 0
        return (clamped / 100.0 * max).roundToInt().coerceIn(1, max)
    }
}
