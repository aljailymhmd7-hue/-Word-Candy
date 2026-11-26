package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    CLICK,
    POP,
    CORRECT_GUESS,
    LEVEL_COMPLETE,
    WRONG_GUESS,
    TIME_UP
}

interface SoundEffectsController {
    var enabled: Boolean
    fun play(effect: SoundEffect)
    fun playClick()
    fun playPop()
    fun playDing()
    fun playWinFanfare()
    fun playWrongGuess()
    fun playTimeUp()
    fun release()
}

val LocalSoundEffects = staticCompositionLocalOf<SoundEffectsController?> { null }

class SoundEffects : SoundEffectsController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val pcmCache = ConcurrentHashMap<SoundEffect, ShortArray>()
    private val concurrencyLimit = Semaphore(permits = 4)
    private val lastPopTimeMs = AtomicLong(0L)

    @Volatile
    override var enabled: Boolean = true

    override fun play(effect: SoundEffect) {
        if (!enabled) return
        if (effect == SoundEffect.POP) {
            val now = System.currentTimeMillis()
            val prev = lastPopTimeMs.get()
            if (now - prev < 35L || !lastPopTimeMs.compareAndSet(prev, now)) {
                return
            }
        }
        playCachedSequence(effect)
    }

    override fun playPop() = play(SoundEffect.POP)

    override fun playClick() = play(SoundEffect.CLICK)

    override fun playDing() = play(SoundEffect.CORRECT_GUESS)

    override fun playWinFanfare() = play(SoundEffect.LEVEL_COMPLETE)

    override fun playWrongGuess() = play(SoundEffect.WRONG_GUESS)

    override fun playTimeUp() = play(SoundEffect.TIME_UP)

    override fun release() {
        scope.cancel()
        pcmCache.clear()
    }

    private data class ToneNote(val freqHz: Double, val durationMs: Int, val volume: Double)

    private fun notesFor(effect: SoundEffect): List<ToneNote> = when (effect) {
        SoundEffect.CLICK -> listOf(
            ToneNote(523.25, 35, 0.18)
        )
        SoundEffect.POP -> listOf(
            ToneNote(587.33, 45, 0.25),
            ToneNote(783.99, 55, 0.20)
        )
        SoundEffect.CORRECT_GUESS -> listOf(
            ToneNote(659.25, 70, 0.28),
            ToneNote(880.00, 90, 0.30),
            ToneNote(1046.50, 140, 0.25)
        )
        SoundEffect.LEVEL_COMPLETE -> listOf(
            ToneNote(523.25, 95, 0.28),
            ToneNote(659.25, 95, 0.28),
            ToneNote(783.99, 95, 0.28),
            ToneNote(1046.50, 220, 0.34)
        )
        SoundEffect.WRONG_GUESS -> listOf(
            ToneNote(246.94, 55, 0.16),
            ToneNote(185.00, 85, 0.18)
        )
        SoundEffect.TIME_UP -> listOf(
            ToneNote(440.00, 110, 0.24),
            ToneNote(349.23, 120, 0.24),
            ToneNote(261.63, 210, 0.26)
        )
    }

    private fun buildPcmBuffer(notes: List<ToneNote>): ShortArray {
        val totalSamples = notes.sumOf { (SAMPLE_RATE * it.durationMs) / 1000 }
        if (totalSamples <= 0) return ShortArray(0)
        val buffer = ShortArray(totalSamples)
        var offset = 0
        for (note in notes) {
            val count = (SAMPLE_RATE * note.durationMs) / 1000
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val envelope = exp(-4.5 * i / count.toDouble())
                val sample = sin(2.0 * PI * note.freqHz * t) * envelope * note.volume
                buffer[offset + i] = (sample * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            offset += count
        }
        return buffer
    }

    private fun playCachedSequence(effect: SoundEffect) {
        scope.launch {
            if (!concurrencyLimit.tryAcquire()) return@launch
            var track: AudioTrack? = null
            try {
                val notes = notesFor(effect)
                val buffer = pcmCache.getOrPut(effect) { buildPcmBuffer(notes) }
                if (buffer.isEmpty()) return@launch

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                val totalMs = notes.sumOf { it.durationMs }.toLong() + 80L
                delay(totalMs)
            } catch (_: Throwable) {
                // Safe fallback in headless/unit-test environments without audio output
            } finally {
                runCatching { track?.release() }
                concurrencyLimit.release()
            }
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22050
    }
}
