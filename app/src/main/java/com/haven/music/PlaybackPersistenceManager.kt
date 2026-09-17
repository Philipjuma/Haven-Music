package com.haven.music

import android.content.Context
import android.content.SharedPreferences

class PlaybackPersistenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("haven_playback", Context.MODE_PRIVATE)

    fun saveState(songId: Long, position: Long, queueIds: List<Long>, shuffle: Boolean, repeat: Int, room: Int, librarySection: String, audioEngine: String) {
        prefs.edit()
            .putLong("last_song_id", songId)
            .putLong("last_position", position)
            .putString("last_queue_ids", queueIds.joinToString(","))
            .putBoolean("shuffle_mode", shuffle)
            .putInt("repeat_mode", repeat)
            .putInt("last_room", room)
            .putString("last_library_section", librarySection)
            .putString("audio_engine", audioEngine)
            .apply()
    }

    fun getSavedLibrarySection(): String = prefs.getString("last_library_section", "Songs") ?: "Songs"
    fun getSavedAudioEngine(): String = prefs.getString("audio_engine", "Media3") ?: "Media3"

    fun getSavedSongId(): Long = prefs.getLong("last_song_id", -1L)
    fun getSavedPosition(): Long = prefs.getLong("last_position", 0L)
    fun getSavedQueueIds(): List<Long> {
        val raw = prefs.getString("last_queue_ids", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }
    fun getSavedShuffleMode(): Boolean = prefs.getBoolean("shuffle_mode", false)
    fun getSavedRepeatMode(): Int = prefs.getInt("repeat_mode", 0) // Default 0 (OFF)
    fun getSavedRoom(): Int = prefs.getInt("last_room", 1) // Default 1 (Now Playing hub)

    fun clearState() {
        prefs.edit().clear().apply()
    }
}
