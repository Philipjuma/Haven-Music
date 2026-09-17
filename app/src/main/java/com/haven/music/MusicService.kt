package com.haven.music

import android.app.PendingIntent
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.*

@UnstableApi
class MusicService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val pjHavenAudioProcessor = PJHavenAudioProcessor()
    private val pjSoundManager = PJSoundManager(pjHavenAudioProcessor)
    private val equalizerManager = EqualizerManager()
    private lateinit var audioManager: AudioManager
    
    private var resumeOnBTEnabled = false
    private var resumeOnHeadsetEnabled = false
    private var lastConnectedDevices = mutableSetOf<Int>()
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateOutputType()
            
            if (addedDevices != null) {
                val hasBT = addedDevices.any { it.isSink && (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) }
                val hasHeadset = addedDevices.any { it.isSink && (it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || (Build.VERSION.SDK_INT >= 26 && it.type == AudioDeviceInfo.TYPE_USB_HEADSET)) }

                if ((resumeOnBTEnabled && hasBT) || (resumeOnHeadsetEnabled && hasHeadset)) {
                    mediaSession?.player?.play()
                }
            }
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateOutputType()
        }
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)

        val persistence = LibraryPersistenceManager(this)
        resumeOnBTEnabled = persistence.getResumeOnBT()
        resumeOnHeadsetEnabled = persistence.getResumeOnHeadset()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setAudioProcessorChain(DefaultAudioProcessorChain(pjHavenAudioProcessor))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setSkipSilenceEnabled(persistence.getSkipSilenceEnabled())
            .build()

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = player.audioSessionId
                    if (sessionId != C.AUDIO_SESSION_ID_UNSET) {
                        pjSoundManager.init(sessionId)
                        equalizerManager.init(sessionId)
                        updateOutputType()
                    }
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("MusicService", "Playback Error: ${error.message}", error)
                // Try to recover if it's a transient error
                if (error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                    player.prepare()
                }
            }
        })

        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    when (customCommand.customAction) {
                        "SET_AUDIO_ENGINE" -> {
                            val engineName = args.getString("engine")
                            try {
                                val engine = AudioEngine.valueOf(engineName ?: "Media3")
                                pjSoundManager.setEngine(engine)
                                
                                // FORCE RECONFIGURATION: Toggling the engine changes PJHavenAudioProcessor's output format
                                // Media3 renderers need to be reset to pick up this change mid-stream.
                                if (player.playbackState != Player.STATE_IDLE) {
                                    val currentPos = player.currentPosition
                                    val isPlaying = player.playWhenReady
                                    player.prepare()
                                    player.seekTo(currentPos)
                                    player.playWhenReady = isPlaying
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_EQUALIZER_MODE" -> {
                            val mode = args.getString("mode")
                            if (mode == "System") {
                                // Disable internal EQ when system EQ is requested
                                pjSoundManager.setEngine(AudioEngine.Media3)
                                equalizerManager.setEnabled(false)
                            } else {
                                // Re-enable internal engine if it was active
                                // Note: MainViewModel will follow up with SET_AUDIO_ENGINE if needed
                            }
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "TOGGLE_EQ" -> {
                            val enabled = args.getBoolean("enabled")
                            equalizerManager.setEnabled(enabled)
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "UPDATE_EQ_BAND" -> {
                            val index = args.getInt("index")
                            val level = args.getInt("level")
                            equalizerManager.setBandLevel(index.toShort(), level.toShort())
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_EQ_PRE_GAIN" -> {
                            val gain = args.getInt("gain")
                            // We don't have a pre-gain effect yet, but keep state
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_BASS_STRENGTH" -> {
                            val strength = args.getInt("strength")
                            equalizerManager.setBassStrength(strength.toShort())
                            pjSoundManager.setPunchIntensity(strength)
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_VIRTUALIZER_STRENGTH" -> {
                            val strength = args.getInt("strength")
                            equalizerManager.setVirtualizerStrength(strength.toShort())
                            pjSoundManager.setImmerseIntensity(strength)
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_REVERB_PRESET" -> {
                            val preset = args.getInt("preset")
                            equalizerManager.setReverbPreset(preset.toShort())
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_AMBIENCE" -> {
                            val value = args.getInt("value")
                            pjSoundManager.setAuraIntensity(value)
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_SKIP_SILENCE_ENABLED" -> {
                            val enabled = args.getBoolean("enabled")
                            (mediaSession?.player as? ExoPlayer)?.skipSilenceEnabled = enabled
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_RESUME_ON_BT" -> {
                            resumeOnBTEnabled = args.getBoolean("enabled")
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SET_RESUME_ON_HEADSET" -> {
                            resumeOnHeadsetEnabled = args.getBoolean("enabled")
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                        "SYNC_EQ" -> {
                            val enabled = args.getBoolean("enabled")
                            val strength = args.getInt("strength")
                            val virt = args.getInt("virtualizer", 0)
                            val reverb = args.getInt("reverb", 0)
                            val aura = args.getInt("ambience", 0)
                            val bandsBundle = args.getBundle("bands")
                            val bandsMap = mutableMapOf<Short, Short>()
                            bandsBundle?.keySet()?.forEach { key ->
                                val index = key.toShortOrNull()
                                if (index != null) {
                                    bandsMap[index] = bandsBundle.getShort(key)
                                }
                            }
                            equalizerManager.syncSettings(enabled, bandsMap, strength.toShort(), virt.toShort(), reverb.toShort())
                            pjSoundManager.setPunchIntensity(strength)
                            pjSoundManager.setImmerseIntensity(virt)
                            pjSoundManager.setAuraIntensity(aura)
                            return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    return com.google.common.util.concurrent.Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
            })
            .build()
    }

    private fun updateOutputType() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var outputType = PJSoundManager.OutputType.OTHER
        
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    outputType = PJSoundManager.OutputType.SPEAKER
                }
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, 
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_HEADSET -> {
                    outputType = PJSoundManager.OutputType.HEADPHONES
                    break // Prioritize headphones
                }
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    outputType = PJSoundManager.OutputType.BLUETOOTH
                    break // Prioritize BT
                }
                else -> {
                    // Other devices fall under OTHER
                }
            }
        }
        pjSoundManager.setOutputType(outputType)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        serviceScope.cancel()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        pjHavenAudioProcessor.release()
        pjSoundManager.release()
        equalizerManager.release()
        super.onDestroy()
    }
}
