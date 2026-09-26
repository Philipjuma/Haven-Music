package com.haven.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface OnlineMusicProvider {
    val name: String
    suspend fun searchTracks(query: String): List<OnlineTrack>
}

class AudiusProvider : OnlineMusicProvider {
    override val name: String = "Audius"

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val host = getBestHost() ?: return@withContext emptyList()
            val url = URL("https://$host/v1/tracks/search?query=$encodedQuery&app_name=HAVENMUSIC")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val data = json.getJSONArray("data")
            
            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val user = item.getJSONObject("user")
                
                tracks.add(
                    OnlineTrack(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        artist = user.getString("name"),
                        duration = item.getLong("duration") * 1000, // Audius gives seconds
                        artUrl = item.optJSONObject("artwork")?.optString("150x150"),
                        streamUrl = "https://$host/v1/tracks/${item.getString("id")}/stream?app_name=HAVENMUSIC",
                        provider = name,
                        isDownloadable = item.optBoolean("downloadable", false)
                    )
                )
            }
            return@withContext tracks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    private fun getBestHost(): String? {
        return try {
            val url = URL("https://api.audius.co")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val data = json.getJSONArray("data")
            if (data.length() > 0) data.getString(0) else null
        } catch (e: Exception) {
            "discoveryprovider.audius.co" // Fallback
        }
    }
}

class JamendoProvider : OnlineMusicProvider {
    override val name: String = "Jamendo"
    private val clientId = "5627d391" // Public client ID for Jamendo

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.jamendo.com/v3.0/tracks/?client_id=$clientId&format=json&limit=20&search=$encodedQuery&include=musicinfo&audioformat=mp32")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val results = json.getJSONArray("results")
            
            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                
                tracks.add(
                    OnlineTrack(
                        id = item.getString("id"),
                        title = item.getString("name"),
                        artist = item.getString("artist_name"),
                        duration = item.getLong("duration") * 1000,
                        artUrl = item.optString("album_image"),
                        streamUrl = item.getString("audio"),
                        provider = name,
                        isDownloadable = true // Jamendo creative commons usually allows this
                    )
                )
            }
            return@withContext tracks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}

class MusicBrainzProvider : OnlineMusicProvider {
    override val name: String = "MusicBrainz"

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://musicbrainz.org/ws/2/recording?query=$encodedQuery&limit=15&fmt=json")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "HavenMusic/1.0.0 ( contact@example.com )")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val recordings = json.getJSONArray("recordings")
            
            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until recordings.length()) {
                val item = recordings.getJSONObject(i)
                val artistCredit = item.getJSONArray("artist-credit")
                val artistName = if (artistCredit.length() > 0) artistCredit.getJSONObject(0).getString("name") else "Unknown"
                
                val releases = item.optJSONArray("releases")
                val releaseId = if (releases != null && releases.length() > 0) releases.getJSONObject(0).getString("id") else null
                
                tracks.add(
                    OnlineTrack(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        artist = artistName,
                        duration = item.optLong("length", 0L),
                        artUrl = if (releaseId != null) "https://coverartarchive.org/release/$releaseId/front-250" else null,
                        streamUrl = "", // MusicBrainz provides metadata only
                        provider = name,
                        isDownloadable = false
                    )
                )
            }
            return@withContext tracks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun fetchArtistDetails(artistName: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            // Strict artist matching using Lucene syntax
            val strictQuery = "artist:\"$artistName\""
            val encodedArtist = URLEncoder.encode(strictQuery, "UTF-8")
            val url = URL("https://musicbrainz.org/ws/2/artist?query=$encodedArtist&limit=1&fmt=json")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "HavenMusic/1.0.0 ( contact@example.com )")
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val artists = json.getJSONArray("artists")
            if (artists.length() > 0) {
                val artist = artists.getJSONObject(0)
                // Strict check: Ensure the name actually matches exactly (ignoring case)
                val foundName = artist.optString("name", "")
                if (foundName.equals(artistName, ignoreCase = true)) artist else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchRecordingDetails(title: String, artist: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            // Strict recording and artist matching
            val strictQuery = "recording:\"$title\" AND artist:\"$artist\""
            val encodedQuery = URLEncoder.encode(strictQuery, "UTF-8")
            val url = URL("https://musicbrainz.org/ws/2/recording?query=$encodedQuery&limit=1&fmt=json")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "HavenMusic/1.0.0 ( contact@example.com )")
            
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val recordings = json.getJSONArray("recordings")
            if (recordings.length() > 0) recordings.getJSONObject(0) else null
        } catch (e: Exception) {
            null
        }
    }
}

class DeezerProvider : OnlineMusicProvider {
    override val name: String = "Deezer"

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://api.deezer.com/search?q=$encodedQuery&limit=15")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val data = json.getJSONArray("data")
            
            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val artist = item.getJSONObject("artist")
                val album = item.getJSONObject("album")
                
                tracks.add(
                    OnlineTrack(
                        id = item.getLong("id").toString(),
                        title = item.getString("title"),
                        artist = artist.getString("name"),
                        duration = item.getLong("duration") * 1000,
                        artUrl = album.getString("cover_medium"),
                        streamUrl = item.getString("preview"), // 30s preview
                        provider = name,
                        isDownloadable = false
                    )
                )
            }
            return@withContext tracks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}

class ITunesProvider : OnlineMusicProvider {
    override val name: String = "iTunes"

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encodedQuery&media=music&limit=15")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val results = json.getJSONArray("results")
            
            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                
                tracks.add(
                    OnlineTrack(
                        id = item.optLong("trackId", 0L).toString(),
                        title = item.optString("trackName", "Unknown"),
                        artist = item.optString("artistName", "Unknown Artist"),
                        duration = item.optLong("trackTimeMillis", 0L),
                        artUrl = item.optString("artworkUrl100", "").replace("100x100", "300x300"),
                        streamUrl = item.optString("previewUrl", ""),
                        provider = name,
                        isDownloadable = false
                    )
                )
            }
            return@withContext tracks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}

class BaquirProvider : OnlineMusicProvider {
    override val name: String = "Baquir"
    private val baseUrl = "https://musicapi.x007.workers.dev"

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            // Use 'seevn' (JioSaavn) as primary engine
            val url = URL("$baseUrl/search?q=$encodedQuery&searchEngine=seevn")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val data = json.getJSONArray("response")
            
            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.getString("id")
                
                tracks.add(
                    OnlineTrack(
                        id = id,
                        title = item.getString("title"),
                        artist = "Official Discovery", // API Search doesn't return artist directly in brief
                        duration = 0L, // Duration obtained via fetch/stream
                        artUrl = item.optString("img"),
                        streamUrl = "$baseUrl/fetch?id=$id", // Direct stream resolution
                        provider = name,
                        isDownloadable = true
                    )
                )
            }
            return@withContext tracks
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }
}

class YouTubeProvider : OnlineMusicProvider {
    override val name: String = "YouTube"
    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://piped-api.lunar.icu",
        "https://pipedapi.drgns.space",
        "https://api.piped.projectsegfau.lt"
    )

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        for (instance in pipedInstances) {
            try {
                val url = URL("$instance/search?q=$encodedQuery&filter=videos")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val items = json.getJSONArray("content")
                
                val tracks = mutableListOf<OnlineTrack>()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    if (item.optString("type") != "stream") continue

                    val videoUrl = item.getString("url")
                    val videoId = videoUrl.substringAfterLast("/")
                    
                    tracks.add(
                        OnlineTrack(
                            id = videoId,
                            title = item.getString("title"),
                            artist = item.getString("uploaderName"),
                            duration = item.optLong("duration", 0L) * 1000,
                            artUrl = item.optString("thumbnail"),
                            streamUrl = "https://www.youtube.com/watch?v=$videoId",
                            provider = name,
                            isDownloadable = false
                        )
                    )
                }
                if (tracks.isNotEmpty()) return@withContext tracks
            } catch (e: Exception) {
                e.printStackTrace()
                continue // Try next instance
            }
        }
        return@withContext emptyList()
    }
}

class SpotifyProvider : OnlineMusicProvider {
    override val name: String = "Spotify"
    // Using a reliable public Spotify proxy for metadata
    private val apiUrl = "https://spotify-search-proxy.pjhaven.workers.dev"

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = URL("$apiUrl/search?q=$encodedQuery&type=track&limit=15")
            
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val items = json.getJSONObject("tracks").getJSONArray("items")
            
            val tracks = mutableListOf<OnlineTrack>()
            for (i in 0 until data_len(items)) {
                val item = items.getJSONObject(i)
                val artists = item.getJSONArray("artists")
                val artistName = if (artists.length() > 0) artists.getJSONObject(0).getString("name") else "Unknown"
                val album = item.getJSONObject("album")
                val images = album.getJSONArray("images")
                val artUrl = if (images.length() > 0) images.getJSONObject(0).getString("url") else null
                
                tracks.add(
                    OnlineTrack(
                        id = item.getString("id"),
                        title = item.getString("name"),
                        artist = artistName,
                        duration = item.getLong("duration_ms"),
                        artUrl = artUrl,
                        streamUrl = "", // Spotify provides metadata only in this public proxy
                        provider = name,
                        isDownloadable = false
                    )
                )
            }
            return@withContext tracks
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    private fun data_len(arr: JSONArray): Int = arr.length()
}
