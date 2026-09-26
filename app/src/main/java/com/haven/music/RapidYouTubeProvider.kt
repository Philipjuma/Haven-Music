package com.haven.music

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class RapidYouTubeProvider : OnlineMusicProvider {
    override val name: String = "Haven"

    private val pipedInstances = listOf(
        "https://pipedapi.kavin.rocks",
        "https://api.piped.video",
        "https://pipedapi.drgns.space",
        "https://pipedapi.mha.fi"
    )

    private val invidiousInstances = listOf(
        "https://inv.tux.pizza",
        "https://invidious.nerdvpn.de",
        "https://vid.puffyan.us"
    )

    override suspend fun searchTracks(query: String): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        for (baseUrl in pipedInstances) {
            try {
                val searchUrl = URL("$baseUrl/search?q=$encodedQuery&filter=music_songs")
                val connection = (searchUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val items = json.optJSONArray("items") ?: JSONArray()
                    
                    val tracks = mutableListOf<OnlineTrack>()
                    for (i in 0 until minOf(items.length(), 6)) {
                        val item = items.getJSONObject(i)
                        val itemUrl = item.optString("url", "")
                        val videoId = if (itemUrl.contains("/watch?v=")) itemUrl.substringAfter("/watch?v=") else itemUrl
                        if (videoId.isBlank()) continue

                        val title = item.optString("title", "Unknown Title")
                        val artist = item.optString("uploaderName", "Haven Engine")
                        val durationSec = item.optLong("duration", 0L)
                        val artUrl = item.optString("thumbnail", "")

                        tracks.add(
                            OnlineTrack(
                                id = videoId,
                                title = title,
                                artist = artist,
                                duration = durationSec * 1000L,
                                artUrl = if (artUrl.isNotBlank()) artUrl else null,
                                streamUrl = "PENDING:$videoId",
                                provider = name
                            )
                        )
                    }
                    if (tracks.isNotEmpty()) return@withContext tracks
                }
            } catch (_: Exception) {}
        }
        return@withContext emptyList()
    }

    suspend fun resolveStreamUrl(target: String): String? = withContext(Dispatchers.IO) {
        val rawTarget = if (target.startsWith("PENDING:")) target.substringAfter("PENDING:") else target
        
        val resolvedVideoId = if (!rawTarget.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            val searchResults = searchTracks(rawTarget)
            searchResults.firstOrNull()?.id ?: rawTarget
        } else {
            rawTarget
        }

        // Layer 1: Native YouTube InnerTube API (WEB_SAFARI iOS client payload - bypasses bot detection)
        try {
            val innerTubeUrl = URL("https://www.youtube.com/youtubei/v1/player")
            val connection = (innerTubeUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1")
            }

            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_SAFARI")
                        put("clientVersion", "1.0.0")
                        put("osName", "iOS")
                        put("osVersion", "17.5")
                    })
                })
                put("videoId", resolvedVideoId)
            }

            connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val streamingData = json.optJSONObject("streamingData")
                val adaptiveFormats = streamingData?.optJSONArray("adaptiveFormats") ?: JSONArray()

                var bestUrl: String? = null
                var maxBitrate = -1

                for (i in 0 until adaptiveFormats.length()) {
                    val fmt = adaptiveFormats.getJSONObject(i)
                    val mime = fmt.optString("mimeType", "")
                    val url = fmt.optString("url", "")
                    val bitrate = fmt.optInt("bitrate", 0)
                    if (mime.contains("audio") && url.isNotBlank() && bitrate > maxBitrate) {
                        maxBitrate = bitrate
                        bestUrl = url
                    }
                }

                if (bestUrl != null) return@withContext bestUrl
            }
        } catch (_: Exception) {}

        // Layer 2: Native YouTube InnerTube API (ANDROID_MUSIC fallback payload)
        try {
            val innerTubeUrl = URL("https://www.youtube.com/youtubei/v1/player")
            val connection = (innerTubeUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "com.google.android.apps.youtube.music/6.29.52 (Linux; U; Android 14)")
            }

            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "ANDROID_MUSIC")
                        put("clientVersion", "6.29.52")
                    })
                })
                put("videoId", resolvedVideoId)
            }

            connection.outputStream.bufferedWriter().use { it.write(payload.toString()) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val streamingData = json.optJSONObject("streamingData")
                val adaptiveFormats = streamingData?.optJSONArray("adaptiveFormats") ?: JSONArray()

                var bestUrl: String? = null
                var maxBitrate = -1

                for (i in 0 until adaptiveFormats.length()) {
                    val fmt = adaptiveFormats.getJSONObject(i)
                    val mime = fmt.optString("mimeType", "")
                    val url = fmt.optString("url", "")
                    val bitrate = fmt.optInt("bitrate", 0)
                    if (mime.contains("audio") && url.isNotBlank() && bitrate > maxBitrate) {
                        maxBitrate = bitrate
                        bestUrl = url
                    }
                }

                if (bestUrl != null) return@withContext bestUrl
            }
        } catch (_: Exception) {}

        // Layer 2: Cobalt API (https://api.cobalt.tools)
        try {
            val cobaltUrl = URL("https://api.cobalt.tools")
            val connection = (cobaltUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            val cobaltPayload = JSONObject().apply {
                put("url", "https://www.youtube.com/watch?v=$resolvedVideoId")
                put("downloadMode", "audio")
                put("audioFormat", "mp3")
            }

            connection.outputStream.bufferedWriter().use { it.write(cobaltPayload.toString()) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val streamUrl = json.optString("url", "")
                if (streamUrl.isNotBlank()) return@withContext streamUrl
            }
        } catch (_: Exception) {}

        // Layer 3: Invidious Failover Instances
        for (baseUrl in invidiousInstances) {
            try {
                val streamUrl = URL("$baseUrl/api/v1/videos/$resolvedVideoId")
                val connection = (streamUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val formats = json.optJSONArray("adaptiveFormats") ?: JSONArray()

                    var bestUrl: String? = null
                    var maxBitrate = -1

                    for (i in 0 until formats.length()) {
                        val fmt = formats.getJSONObject(i)
                        val type = fmt.optString("type", "")
                        val url = fmt.optString("url", "")
                        val bitrate = fmt.optInt("bitrate", 0)
                        if (type.contains("audio") && url.isNotBlank() && bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestUrl = url
                        }
                    }

                    if (bestUrl != null) return@withContext bestUrl
                }
            } catch (_: Exception) {}
        }

        // Layer 4: Piped Failover Instances
        for (baseUrl in pipedInstances) {
            try {
                val streamUrl = URL("$baseUrl/streams/$resolvedVideoId")
                val connection = (streamUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 4000
                    readTimeout = 4000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                }

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val audioStreams = json.optJSONArray("audioStreams") ?: JSONArray()

                    var bestUrl: String? = null
                    var maxBitrate = -1

                    for (i in 0 until audioStreams.length()) {
                        val stream = audioStreams.getJSONObject(i)
                        val url = stream.optString("url", "")
                        val bitrate = stream.optInt("bitrate", 0)
                        if (url.isNotBlank() && bitrate > maxBitrate) {
                            maxBitrate = bitrate
                            bestUrl = url
                        }
                    }

                    if (bestUrl != null) return@withContext bestUrl
                }
            } catch (_: Exception) {}
        }

        // Layer 5: Direct Full-Length Unthrottled Audio Stream Engines (Audius & Jamendo)
        try {
            val audiusResults = AudiusProvider().searchTracks(rawTarget)
            val audiusStream = audiusResults.firstOrNull { it.streamUrl.isNotBlank() }?.streamUrl
            if (audiusStream != null) return@withContext audiusStream

            val jamendoResults = JamendoProvider().searchTracks(rawTarget)
            val jamendoStream = jamendoResults.firstOrNull { it.streamUrl.isNotBlank() }?.streamUrl
            if (jamendoStream != null) return@withContext jamendoStream
        } catch (_: Exception) {}

        return@withContext null
    }
}
