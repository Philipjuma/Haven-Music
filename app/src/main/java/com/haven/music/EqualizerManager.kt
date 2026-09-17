package com.haven.music

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.util.Log

class EqualizerManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    
    private var currentAudioSessionId: Int = -1
    private var isEnabled: Boolean = false
    private var bandLevels: Map<Short, Short> = emptyMap()
    private var bassStrength: Short = 0
    private var virtualizerStrength: Short = 0
    private var reverbPreset: Short = 0

    fun init(audioSessionId: Int) {
        if (currentAudioSessionId == audioSessionId && equalizer != null) return

        release()
        currentAudioSessionId = audioSessionId
        try {
            equalizer = Equalizer(0, audioSessionId)
            bassBoost = BassBoost(0, audioSessionId)
            virtualizer = Virtualizer(0, audioSessionId)
            presetReverb = PresetReverb(0, audioSessionId)
            applySettings()
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Failed to initialize Audio Effects: ${e.message}")
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        presetReverb?.enabled = enabled
    }

    fun setBandLevel(band: Short, level: Short) {
        val updated = bandLevels.toMutableMap()
        updated[band] = level
        bandLevels = updated
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Failed to set band $band level: ${e.message}")
        }
    }

    fun setBassStrength(strength: Short) {
        bassStrength = strength
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength)
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Failed to set bass strength: ${e.message}")
        }
    }

    fun setVirtualizerStrength(strength: Short) {
        virtualizerStrength = strength
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Failed to set virtualizer strength: ${e.message}")
        }
    }

    fun setReverbPreset(preset: Short) {
        reverbPreset = preset
        try {
            presetReverb?.preset = preset
        } catch (e: Exception) {
            Log.e("EqualizerManager", "Failed to set reverb preset: ${e.message}")
        }
    }

    fun syncSettings(enabled: Boolean, bands: Map<Short, Short>, bass: Short, virt: Short, reverb: Short) {
        isEnabled = enabled
        bandLevels = bands
        bassStrength = bass
        virtualizerStrength = virt
        reverbPreset = reverb
        applySettings()
    }
    
    val minLevelMillibel: Int get() = getBandLevelRange().first.toInt()
    val maxLevelMillibel: Int get() = getBandLevelRange().second.toInt()

    fun getBandFrequencies(): List<Int> {
        val eq = equalizer ?: return emptyList()
        val numBands = eq.numberOfBands
        return (0 until numBands).map { eq.getCenterFreq(it.toShort()) / 1000 } // Hz
    }

    fun getBandLevelRange(): Pair<Short, Short> {
        val eq = equalizer ?: return Pair(-1500, 1500)
        val range = eq.bandLevelRange
        return Pair(range[0], range[1])
    }

    private fun applySettings() {
        equalizer?.enabled = isEnabled
        bassBoost?.enabled = isEnabled
        virtualizer?.enabled = isEnabled
        presetReverb?.enabled = isEnabled
        
        bandLevels.forEach { (band, level) ->
            try { equalizer?.setBandLevel(band, level) } catch (e: Exception) {}
        }
        
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(bassStrength)
            }
        } catch (e: Exception) {}

        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(virtualizerStrength)
            }
        } catch (e: Exception) {}

        try {
            presetReverb?.preset = reverbPreset
        } catch (e: Exception) {}
    }

    fun release() {
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
        presetReverb?.release()
        equalizer = null
        bassBoost = null
        virtualizer = null
        presetReverb = null
        currentAudioSessionId = -1
    }
}
