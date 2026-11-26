package com.example.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.concurrent.atomic.AtomicLong

enum class HapticCue {
    LETTER_TICK,
    WORD_FOUND,
    LEVEL_COMPLETE,
    WRONG_GUESS
}

interface HapticsController {
    var enabled: Boolean
    fun perform(cue: HapticCue)
    fun playLetterTick()
    fun playWordFound()
    fun playLevelComplete()
    fun playWrongGuess()
}

val LocalHaptics = staticCompositionLocalOf<HapticsController?> { null }

class AndroidHapticsController(context: Context) : HapticsController {

    private val appContext = context.applicationContext
    private val lastTickTimeMs = AtomicLong(0L)

    @Volatile
    override var enabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = appContext.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    override fun perform(cue: HapticCue) {
        if (!enabled) return
        if (cue == HapticCue.LETTER_TICK) {
            val now = System.currentTimeMillis()
            val prev = lastTickTimeMs.get()
            if (now - prev < 25L || !lastTickTimeMs.compareAndSet(prev, now)) {
                return
            }
        }
        vibrateCue(cue)
    }

    override fun playLetterTick() = perform(HapticCue.LETTER_TICK)

    override fun playWordFound() = perform(HapticCue.WORD_FOUND)

    override fun playLevelComplete() = perform(HapticCue.LEVEL_COMPLETE)

    override fun playWrongGuess() = perform(HapticCue.WRONG_GUESS)

    private fun vibrateCue(cue: HapticCue) {
        val vib = vibrator ?: return
        if (!runCatching { vib.hasVibrator() }.getOrDefault(false)) return

        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cue == HapticCue.LETTER_TICK -> {
                    vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val effect = when (cue) {
                        HapticCue.LETTER_TICK ->
                            VibrationEffect.createOneShot(12L, 95)
                        HapticCue.WORD_FOUND ->
                            VibrationEffect.createWaveform(
                                longArrayOf(0L, 26L, 45L, 44L),
                                intArrayOf(0, 175, 0, 240),
                                -1
                            )
                        HapticCue.LEVEL_COMPLETE ->
                            VibrationEffect.createWaveform(
                                longArrayOf(0L, 28L, 40L, 34L, 45L, 70L),
                                intArrayOf(0, 180, 0, 210, 0, 255),
                                -1
                            )
                        HapticCue.WRONG_GUESS ->
                            VibrationEffect.createWaveform(
                                longArrayOf(0L, 16L, 30L, 16L),
                                intArrayOf(0, 85, 0, 85),
                                -1
                            )
                    }
                    vib.vibrate(effect)
                }
                else -> {
                    @Suppress("DEPRECATION")
                    when (cue) {
                        HapticCue.LETTER_TICK -> vib.vibrate(12L)
                        HapticCue.WORD_FOUND -> vib.vibrate(longArrayOf(0L, 26L, 45L, 44L), -1)
                        HapticCue.LEVEL_COMPLETE -> vib.vibrate(longArrayOf(0L, 28L, 40L, 34L, 45L, 70L), -1)
                        HapticCue.WRONG_GUESS -> vib.vibrate(longArrayOf(0L, 16L, 30L, 16L), -1)
                    }
                }
            }
        }
    }
}
