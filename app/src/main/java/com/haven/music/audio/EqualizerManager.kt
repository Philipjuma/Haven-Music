package com.haven.music.audio

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

class EqualizerManager(
    initialEnabled: Boolean,
    initialBassStrength: Int,
    initialVirtualizerStrength: Int,
    initialReverbPreset: Int,
    initialAmbience: Int,
    initialBands: Map<Int, Int>,
    initialFrequencies: List<Int>,
    val minLevelMillibel: Int = -1500,
    val maxLevelMillibel: Int = 1500,
    private val onEnabledChange: (Boolean) -> Unit,
    private val onBassStrengthChange: (Int) -> Unit,
    private val onVirtualizerStrengthChange: (Int) -> Unit,
    private val onReverbPresetChange: (Int) -> Unit,
    private val onAmbienceChange: (Int) -> Unit,
    private val onBandChange: (Int, Int) -> Unit,
    private val onResetFlat: () -> Unit
) {
    private val _isEnabled = mutableStateOf(initialEnabled)
    val isEnabled: State<Boolean> = _isEnabled

    private val _bassStrength = mutableStateOf(initialBassStrength)
    val bassStrength: State<Int> = _bassStrength

    private val _virtualizerStrength = mutableStateOf(initialVirtualizerStrength)
    val virtualizerStrength: State<Int> = _virtualizerStrength

    private val _reverbPreset = mutableStateOf(initialReverbPreset)
    val reverbPreset: State<Int> = _reverbPreset

    private val _ambience = mutableStateOf(initialAmbience)
    val ambience: State<Int> = _ambience

    private val _bandLevels = mutableStateMapOf<Int, Short>().apply {
        initialBands.forEach { (k, v) -> put(key = k, value = v.toShort()) }
    }
    val bandLevels: State<Map<Int, Short>> = mutableStateOf(_bandLevels)

    private val _bandFrequencies = mutableStateOf(initialFrequencies)
    val bandFrequencies: State<List<Int>> = _bandFrequencies

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        onEnabledChange(enabled)
    }

    fun setBassStrength(strength: Int) {
        _bassStrength.value = strength
        onBassStrengthChange(strength)
    }

    fun setVirtualizerStrength(strength: Int) {
        _virtualizerStrength.value = strength
        onVirtualizerStrengthChange(strength)
    }

    fun setReverbPreset(preset: Int) {
        _reverbPreset.value = preset
        onReverbPresetChange(preset)
    }

    fun setAmbience(value: Int) {
        _ambience.value = value
        onAmbienceChange(value)
    }

    fun setBandLevel(index: Int, level: Int) {
        _bandLevels[index] = level.toShort()
        onBandChange(index, level)
    }

    fun resetFlat() {
        _bandLevels.keys.forEach { _bandLevels[it] = 0 }
        _bassStrength.value = 0
        _virtualizerStrength.value = 0
        _reverbPreset.value = 0
        _ambience.value = 0
        onResetFlat()
    }
    
    fun updateState(enabled: Boolean, bands: Map<Int, Int>, bass: Int, virt: Int, reverb: Int, ambience: Int) {
        _isEnabled.value = enabled
        _bassStrength.value = bass
        _virtualizerStrength.value = virt
        _reverbPreset.value = reverb
        _ambience.value = ambience
        bands.forEach { (k, v) -> _bandLevels[k] = v.toShort() }
    }
}
