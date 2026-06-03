package com.example.util

import java.io.File
import java.io.FileOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

object AudioSynthesizer {
    
    /**
     * Synthesizes a mono 16-bit PCM .wav file containing an ambient, retro-synth chord progression.
     * Switch chords every 5 seconds.
     */
    fun generateDemoAudio(file: File, durationSeconds: Int): Boolean {
        if (file.exists() && file.length() > 1000) return true

        val sampleRate = 11025
        val numSamples = sampleRate * durationSeconds
        val dataSize = numSamples * 2 // 16-bit Mono requires 2 bytes per sample

        return try {
            FileOutputStream(file).use { fos ->
                val dos = DataOutputStream(fos)
                
                // RIFF container and WAVE format header (44 bytes total)
                dos.writeBytes("RIFF")
                dos.writeInt(Integer.reverseBytes(36 + dataSize))
                dos.writeBytes("WAVE")
                
                // Format Subchunk
                dos.writeBytes("fmt ")
                dos.writeInt(Integer.reverseBytes(16)) // 16 for PCM
                writeShortLE(dos, 1) // AudioFormat (1 for PCM)
                writeShortLE(dos, 1) // Mono (1 channel)
                dos.writeInt(Integer.reverseBytes(sampleRate))
                dos.writeInt(Integer.reverseBytes(sampleRate * 2)) // Byte rate (SampleRate * 1 channel * 2 bytes)
                writeShortLE(dos, 2) // Block align
                writeShortLE(dos, 16) // Bits per sample
                
                // Data Subchunk
                dos.writeBytes("data")
                dos.writeInt(Integer.reverseBytes(dataSize))
                
                // Standard little-endian short synthesizer byte buffer
                val buffer = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
                val chords = listOf(
                    listOf(196.00, 261.63, 329.63, 392.00), // C Major / G (G3, C4, E4, G4)
                    listOf(220.00, 261.63, 329.63, 392.00), // A Minor (A3, C4, E4, G4)
                    listOf(174.61, 220.00, 261.63, 349.23), // F Major (F3, A3, C4, F4)
                    listOf(196.00, 246.94, 293.66, 392.00), // G Major (G3, B3, D4, G4)
                    listOf(220.00, 261.63, 311.13, 392.00), // A Half-Diminished
                    listOf(174.61, 220.00, 261.63, 311.13), // F Minor (F3, A3, C4, Eb4)
                    listOf(261.63, 329.63, 392.00, 440.00)  // C Major 6
                )

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    
                    val chordIndex = (t.toInt() / 5) % chords.size
                    val activeChord = chords[chordIndex]
                    
                    var sampleVal = 0.0
                    for (freq in activeChord) {
                        sampleVal += sin(2.0 * Math.PI * freq * t)
                    }
                    sampleVal /= activeChord.size
                    
                    // Tremolo vibrato effect for organic synth warm retro tone
                    val tremoloVolume = 0.65 + 0.15 * sin(2.0 * Math.PI * 5.0 * t)
                    
                    // Attack-decay envelope per chord change
                    val chordTime = t % 5.0
                    val attack = if (chordTime < 0.2) chordTime / 0.2 else 1.0
                    val decay = if (chordTime > 4.5) (5.0 - chordTime) / 0.5 else 1.0
                    
                    val volume = tremoloVolume * attack * decay
                    val shortVal = (sampleVal * 22000 * volume).toInt().coerceIn(-32767, 32767)
                    
                    buffer.putShort(0, shortVal.toShort())
                    dos.write(buffer.array())
                }
                dos.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeShortLE(dos: java.io.DataOutputStream, value: Int) {
        dos.writeByte(value and 0xFF)
        dos.writeByte((value shr 8) and 0xFF)
    }
}
