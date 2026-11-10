package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

class GameSoundManager {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val random = Random()
    var isSoundEnabled: Boolean = true

    fun playGunshot(type: String = "rifle") {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val durationMs = when (type) {
                    "sniper" -> 320
                    "shotgun" -> 250
                    "smg" -> 90
                    else -> 140
                }
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val decay = (1.0 - (i.toDouble() / numSamples)).coerceIn(0.0, 1.0)
                    val noise = (random.nextDouble() * 2.0 - 1.0)
                    val baseFreq = when (type) {
                        "sniper" -> 90.0
                        "shotgun" -> 110.0
                        "smg" -> 220.0
                        else -> 150.0
                    }
                    val tone = sin(2 * PI * baseFreq * t * (1.0 - t * 2.0))
                    val sample = ((noise * 0.7 + tone * 0.3) * decay * 32767 * 0.75).toInt()
                    buffer[i] = sample.coerceIn(-32767, 32767).toShort()
                }

                playBuffer(buffer, sampleRate)
            } catch (_: Exception) {}
        }
    }

    fun playHitMarker(isHeadshot: Boolean = false) {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val durationMs = if (isHeadshot) 80 else 40
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                val freq = if (isHeadshot) 1760.0 else 980.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val decay = (1.0 - (i.toDouble() / numSamples))
                    val tone = sin(2 * PI * freq * t)
                    buffer[i] = (tone * decay * 32767 * 0.6).toInt().toShort()
                }

                playBuffer(buffer, sampleRate)
            } catch (_: Exception) {}
        }
    }

    fun playReload() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val durationMs = 150
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val tone = sin(2 * PI * 800.0 * t) + sin(2 * PI * 1400.0 * t)
                    val decay = (1.0 - (i.toDouble() / numSamples))
                    buffer[i] = (tone * decay * 0.5 * 32767 * 0.5).toInt().toShort()
                }

                playBuffer(buffer, sampleRate)
            } catch (_: Exception) {}
        }
    }

    fun playExplosion() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val durationMs = 500
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val decay = (1.0 - (i.toDouble() / numSamples))
                    val noise = (random.nextDouble() * 2.0 - 1.0)
                    val rumble = sin(2 * PI * 65.0 * (i.toDouble() / sampleRate))
                    val sample = ((noise * 0.65 + rumble * 0.35) * decay * 32767 * 0.9).toInt()
                    buffer[i] = sample.coerceIn(-32767, 32767).toShort()
                }

                playBuffer(buffer, sampleRate)
            } catch (_: Exception) {}
        }
    }

    fun playVehicleHorn() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val durationMs = 300
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val tone = sin(2 * PI * 440.0 * t) + sin(2 * PI * 554.0 * t)
                    buffer[i] = (tone * 0.5 * 32767 * 0.6).toInt().toShort()
                }

                playBuffer(buffer, sampleRate)
            } catch (_: Exception) {}
        }
    }

    fun playVictoryFanfare() {
        if (!isSoundEnabled) return
        scope.launch {
            try {
                val sampleRate = 22050
                val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
                val noteDurationMs = 140

                for (freq in notes) {
                    val numSamples = (sampleRate * (noteDurationMs / 1000.0)).toInt()
                    val buffer = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val decay = (1.0 - (i.toDouble() / numSamples) * 0.3)
                        val tone = sin(2 * PI * freq * t)
                        buffer[i] = (tone * decay * 32767 * 0.7).toInt().toShort()
                    }
                    playBuffer(buffer, sampleRate)
                    kotlinx.coroutines.delay(120)
                }
            } catch (_: Exception) {}
        }
    }

    private fun playBuffer(buffer: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        scope.launch {
            kotlinx.coroutines.delay(1000)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }
}
