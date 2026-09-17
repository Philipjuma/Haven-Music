package com.haven.music

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

@UnstableApi
class PJHavenAudioProcessor : BaseAudioProcessor() {

    private var nativeHandle: Long = 0
    private var isPjHavenEnabled = false
    private var crossfadeDuration: Int = 0
    private var currentProfile = Profile.HEADPHONES
    
    enum class Profile(val value: Int) {
        SPEAKER(0), HEADPHONES(1)
    }

    init {
        try {
            System.loadLibrary("pj_haven_dsp")
            nativeHandle = nativeCreate()
            Log.d("PJHavenDSP", "Native library loaded, handle: $nativeHandle")
        } catch (e: Exception) {
            Log.e("PJHavenDSP", "Failed to load native library: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (isPjHavenEnabled != enabled) {
            Log.d("PJHavenDSP", "Enabled changed to: $enabled")
            isPjHavenEnabled = enabled
            if (nativeHandle != 0L) nativeSetEnabled(nativeHandle, enabled)
            flush()
        }
    }

    fun setProfile(profile: Profile) {
        if (currentProfile != profile) {
            Log.d("PJHavenDSP", "Profile changed to: $profile")
            currentProfile = profile
            if (nativeHandle != 0L) nativeSetProfile(nativeHandle, profile.value)
        }
    }

    fun setPunchIntensity(strength: Float) {
        if (nativeHandle != 0L) nativeSetPunchIntensity(nativeHandle, strength)
    }

    fun setImmerseIntensity(strength: Float) {
        if (nativeHandle != 0L) nativeSetImmerseIntensity(nativeHandle, strength)
    }

    fun setAuraIntensity(strength: Float) {
        if (nativeHandle != 0L) nativeSetAuraIntensity(nativeHandle, strength)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        Log.d("PJHavenDSP", "onConfigure: rate=${inputAudioFormat.sampleRate}, channels=${inputAudioFormat.channelCount}, encoding=${inputAudioFormat.encoding}, enabled=$isPjHavenEnabled, xfade=$crossfadeDuration")
        
        if (!isPjHavenEnabled && crossfadeDuration == 0) {
            return inputAudioFormat // Truly untouched separation when nothing is active
        }

        // Force float output for precision when ANY processing (DSP or Fading) is active
        val outputFormat = AudioProcessor.AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_FLOAT
        )

        if (nativeHandle != 0L) {
            nativeSetSampleRate(nativeHandle, inputAudioFormat.sampleRate.toFloat())
            nativeSetEnabled(nativeHandle, isPjHavenEnabled)
            nativeSetProfile(nativeHandle, currentProfile.value)
        }
        
        return outputFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val inputFormat = inputAudioFormat
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        // Only process if NDK is ready AND either DSP is enabled or Crossfade is active (Crossfade removed, so just DSP)
        if (!isPjHavenEnabled || nativeHandle == 0L) {
            val outputBuffer = replaceOutputBuffer(remaining)
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        // Active processing: Conversion handled in JNI for maximum performance
        val is16Bit = inputFormat.encoding == C.ENCODING_PCM_16BIT
        val inputSampleSize = if (is16Bit) 2 else 4
        val numSamples = remaining / inputSampleSize
        val outputBytes = numSamples * 4 // Always FLOAT output
        
        val outputBuffer = replaceOutputBuffer(outputBytes)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // Pass both buffers to JNI for in-place conversion and processing
        nativeProcess(
            nativeHandle, 
            inputBuffer, 
            outputBuffer, 
            numSamples / inputFormat.channelCount,
            inputFormat.channelCount,
            is16Bit
        )
        
        outputBuffer.position(outputBytes)
        outputBuffer.flip()
    }

    override fun onReset() {
        // We don't release the native handle here because AudioProcessors can be reused
    }

    fun release() {
        if (nativeHandle != 0L) {
            nativeRelease(nativeHandle)
            nativeHandle = 0
        }
    }

    // Native methods
    private external fun nativeCreate(): Long
    private external fun nativeRelease(handle: Long)
    private external fun nativeSetSampleRate(handle: Long, sr: Float)
    private external fun nativeSetEnabled(handle: Long, enabled: Boolean)
    private external fun nativeSetProfile(handle: Long, profile: Int)
    private external fun nativeSetPunchIntensity(handle: Long, intensity: Float)
    private external fun nativeSetImmerseIntensity(handle: Long, intensity: Float)
    private external fun nativeSetAuraIntensity(handle: Long, intensity: Float)
    private external fun nativeProcess(
        handle: Long, 
        input: ByteBuffer, 
        output: ByteBuffer, 
        numFrames: Int, 
        channels: Int, 
        is16Bit: Boolean
    )
}
