package com.haven.music

import android.util.Log
import androidx.annotation.RequiresApi
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PJSoundManager(private val audioProcessor: PJHavenAudioProcessor) {
    
    private var currentEngine = AudioEngine.Media3
    private var currentOutputType = OutputType.OTHER

    enum class OutputType {
        SPEAKER, HEADPHONES, BLUETOOTH, OTHER
    }

    fun init(audioSessionId: Int) {
        // No longer using platform effects
        Log.d("PJSoundManager", "Init with session $audioSessionId")
        applyEngine(currentEngine)
    }

    fun setEngine(engine: AudioEngine) {
        currentEngine = engine
        applyEngine(engine)
    }

    fun setOutputType(outputType: OutputType) {
        if (currentOutputType == outputType) return
        currentOutputType = outputType
        applyEngine(currentEngine)
    }

    fun setPunchIntensity(strength: Int) {
        audioProcessor.setPunchIntensity(strength.toFloat())
    }

    fun setImmerseIntensity(strength: Int) {
        audioProcessor.setImmerseIntensity(strength.toFloat())
    }

    fun setAuraIntensity(strength: Int) {
        audioProcessor.setAuraIntensity(strength.toFloat())
    }

    fun setSpaceIntensity(strength: Int) {
        audioProcessor.setSpaceIntensity(strength.toFloat())
    }

    fun setEQBand(band: Int, gainDb: Float) {
        audioProcessor.setEQBand(band, gainDb)
    }

    private fun applyEngine(engine: AudioEngine) {
        val isEnabled = engine == AudioEngine.PJ_Haven_2_0
        Log.d("PJHavenDSP", "applyEngine: $engine, enabled=$isEnabled")
        audioProcessor.setEnabled(isEnabled)
        
        if (isEnabled) {
            val profile = when (currentOutputType) {
                OutputType.SPEAKER -> PJHavenAudioProcessor.Profile.SPEAKER
                else -> PJHavenAudioProcessor.Profile.HEADPHONES
            }
            Log.d("PJHavenDSP", "applyProfile: $profile")
            audioProcessor.setProfile(profile)
        }
    }

    fun release() {
        // Cleanup if needed
    }
}
