package com.haven.music

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class Playlist(val id: String, val name: String, val songIds: List<Long>)

class LibraryPersistenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("haven_library", Context.MODE_PRIVATE)

    fun saveFavorites(favorites: Set<Long>) {
        prefs.edit().putString("favorites", favorites.joinToString(",")).apply()
    }

    fun getFavorites(): Set<Long> {
        val raw = prefs.getString("favorites", "") ?: ""
        if (raw.isBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun savePlaylists(playlists: List<Playlist>) {
        val jsonArray = JSONArray()
        playlists.forEach { playlist ->
            val json = JSONObject()
            json.put("id", playlist.id)
            json.put("name", playlist.name)
            val idsArray = JSONArray()
            playlist.songIds.forEach { idsArray.put(it) }
            json.put("songIds", idsArray)
            jsonArray.put(json)
        }
        prefs.edit().putString("playlists", jsonArray.toString()).apply()
    }

    fun getPlaylists(): List<Playlist> {
        val raw = prefs.getString("playlists", "[]") ?: "[]"
        val list = mutableListOf<Playlist>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val id = json.getString("id")
                val name = json.getString("name")
                val idsArray = json.getJSONArray("songIds")
                val songIds = mutableListOf<Long>()
                for (j in 0 until idsArray.length()) {
                    songIds.add(idsArray.getLong(j))
                }
                list.add(Playlist(id, name, songIds))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveCategoryOrder(order: List<LibrarySection>) {
        prefs.edit().putString("category_order", order.joinToString(",") { it.name }).apply()
    }

    fun getCategoryOrder(): List<LibrarySection>? {
        val raw = prefs.getString("category_order", "") ?: ""
        if (raw.isBlank()) return null
        return raw.split(",").mapNotNull { name ->
            try { LibrarySection.valueOf(name) } catch (e: Exception) { null }
        }
    }

    fun saveFolders(folders: Set<String>) {
        prefs.edit().putStringSet("music_folders", folders).apply()
    }

    fun getFolders(): Set<String> {
        return prefs.getStringSet("music_folders", emptySet()) ?: emptySet()
    }

    fun saveLibraryCache(songs: List<Song>) {
        val jsonArray = JSONArray()
        songs.forEach { song ->
            val json = JSONObject()
            json.put("id", song.id)
            json.put("title", song.title)
            json.put("artist", song.artist)
            json.put("album", song.album)
            json.put("duration", song.duration)
            json.put("artUri", song.albumArtUri?.toString())
            json.put("contentUri", song.contentUri.toString())
            jsonArray.put(json)
        }
        prefs.edit().putString("library_cache", jsonArray.toString()).apply()
    }

    fun getLibraryCache(): List<Song> {
        val raw = prefs.getString("library_cache", "[]") ?: "[]"
        val list = mutableListOf<Song>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val json = jsonArray.getJSONObject(i)
                val artUriString = json.optString("artUri", "")
                list.add(
                    Song(
                        id = json.getLong("id"),
                        title = json.getString("title"),
                        artist = json.getString("artist"),
                        album = json.getString("album"),
                        duration = json.getLong("duration"),
                        albumArtUri = if (artUriString.isNotEmpty()) android.net.Uri.parse(artUriString) else null,
                        contentUri = android.net.Uri.parse(json.getString("contentUri"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveSettings(rememberPosition: Boolean, engine: String, audioSafeEnabled: Boolean, skipSilence: Boolean, resumeBT: Boolean, resumeHeadset: Boolean) {
        prefs.edit()
            .putBoolean("remember_position", rememberPosition)
            .putString("audio_engine", engine)
            .putBoolean("audio_safe_enabled", audioSafeEnabled)
            .putBoolean("skip_silence", skipSilence)
            .putBoolean("resume_on_bt", resumeBT)
            .putBoolean("resume_on_headset", resumeHeadset)
            .apply()
    }

    fun getSkipSilenceEnabled(): Boolean = prefs.getBoolean("skip_silence", false)
    fun getResumeOnBT(): Boolean = prefs.getBoolean("resume_on_bt", false)
    fun getResumeOnHeadset(): Boolean = prefs.getBoolean("resume_on_headset", false)

    fun getAudioSafeEnabled(): Boolean = prefs.getBoolean("audio_safe_enabled", false)


    fun saveEqualizerSettings(
        enabled: Boolean, 
        preset: String, 
        bands: Map<Int, Int>, 
        preGain: Int, 
        bassStrength: Int,
        virtualizerStrength: Int = 0,
        reverbPreset: Int = 0,
        ambience: Int = 0
    ) {
        val jsonBands = JSONObject()
        bands.forEach { (index, level) -> jsonBands.put(index.toString(), level) }
        prefs.edit()
            .putBoolean("eq_enabled", enabled)
            .putString("eq_preset", preset)
            .putString("eq_bands", jsonBands.toString())
            .putInt("eq_pre_gain", preGain)
            .putInt("eq_bass_strength", bassStrength)
            .putInt("eq_virtualizer_strength", virtualizerStrength)
            .putInt("eq_reverb_preset", reverbPreset)
            .putInt("eq_ambience", ambience)
            .apply()
    }

    fun getEqualizerEnabled(): Boolean = prefs.getBoolean("eq_enabled", false)
    fun getEqualizerPreset(): String = prefs.getString("eq_preset", "Flat") ?: "Flat"
    fun getEqualizerPreGain(): Int = prefs.getInt("eq_pre_gain", 0)
    fun getEqualizerBassStrength(): Int = prefs.getInt("eq_bass_strength", 0)
    fun getEqualizerVirtualizerStrength(): Int = prefs.getInt("eq_virtualizer_strength", 0)
    fun getEqualizerReverbPreset(): Int = prefs.getInt("eq_reverb_preset", 0)
    fun getEqualizerAmbience(): Int = prefs.getInt("eq_ambience", 0)
    fun getEqualizerBands(): Map<Int, Int> {
        val raw = prefs.getString("eq_bands", "{}") ?: "{}"
        val map = mutableMapOf<Int, Int>()
        try {
            val json = JSONObject(raw)
            json.keys().forEach { key ->
                val index = key.toIntOrNull()
                if (index != null) map[index] = json.getInt(key)
            }
        } catch (e: Exception) { e.printStackTrace() }
        return map
    }

    fun getRememberPosition(): Boolean = prefs.getBoolean("remember_position", true)
    fun getAudioEngine(): String = prefs.getString("audio_engine", "Media3") ?: "Media3"

    fun saveColorCache(colors: Map<Long, Int>) {
        val json = JSONObject()
        colors.forEach { (id, color) -> json.put(id.toString(), color) }
        prefs.edit().putString("color_cache", json.toString()).apply()
    }

    fun getColorCache(): Map<Long, Int> {
        val raw = prefs.getString("color_cache", "{}") ?: "{}"
        val map = mutableMapOf<Long, Int>()
        try {
            val json = JSONObject(raw)
            json.keys().forEach { key ->
                val id = key.toLongOrNull()
                if (id != null) map[id] = json.getInt(key)
            }
        } catch (e: Exception) { e.printStackTrace() }
        return map
    }

    fun isFirstLaunch(): Boolean = !prefs.getBoolean("welcome_v1_complete", false)
    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean("welcome_v1_complete", true).apply()
    }

    fun hasSeenHint(hintId: String): Boolean = prefs.getBoolean("hint_$hintId", false)
    fun setHintSeen(hintId: String) {
        prefs.edit().putBoolean("hint_$hintId", true).apply()
    }
}
