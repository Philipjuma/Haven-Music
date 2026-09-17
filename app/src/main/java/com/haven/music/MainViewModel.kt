package com.haven.music

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.collection.LruCache
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import kotlin.random.Random

enum class LibrarySection {
    Songs, Albums, Artists, Playlists, Favorites
}

enum class AudioEngine {
    Media3, PJ_Haven_2_0, PJ_Fern
}

enum class EqualizerMode {
    App, System
}

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val duration: Long,
    val artUrl: String?,
    val streamUrl: String,
    val provider: String,
    val isDownloadable: Boolean = false
)

data class DeepInsight(
    val artistInfo: JSONObject? = null,
    val recordingInfo: JSONObject? = null,
    val year: String? = null,
    val genre: String? = null,
    val producers: List<String> = emptyList(),
    val label: String? = null
)

class MainViewModel(
    private val repository: MusicRepository
) : ViewModel() {

    private val historyManager = HistoryManager(repository.context)
    private val playbackPersistence = PlaybackPersistenceManager(repository.context)
    private val libraryPersistence = LibraryPersistenceManager(repository.context)
    
    private val _songs = MutableStateFlow<List<Song>>(libraryPersistence.getLibraryCache())
    val songs = _songs.asStateFlow()

    private val _isSongsLoaded = MutableStateFlow(_songs.value.isNotEmpty())
    val isSongsLoaded = _isSongsLoaded.asStateFlow()
    
    private val _librarySection = mutableStateOf(
        try { LibrarySection.valueOf(playbackPersistence.getSavedLibrarySection()) } catch (e: Exception) { LibrarySection.Songs }
    )
    val librarySection: State<LibrarySection> = _librarySection

    private val _orderedLibrarySections = MutableStateFlow(
        libraryPersistence.getCategoryOrder()?.filter { it != LibrarySection.valueOf("Discover") } ?: listOf(
            LibrarySection.Songs,
            LibrarySection.Albums,
            LibrarySection.Artists,
            LibrarySection.Playlists,
            LibrarySection.Favorites
        )
    )
    val orderedLibrarySections = _orderedLibrarySections.asStateFlow()

    private val _favorites = MutableStateFlow(libraryPersistence.getFavorites())
    val favorites = _favorites.asStateFlow()

    private val _playlists = MutableStateFlow(libraryPersistence.getPlaylists())
    val playlists = _playlists.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Online Discovery State
    private val _onlineSearchQuery = MutableStateFlow("")
    val onlineSearchQuery = _onlineSearchQuery.asStateFlow()

    private val _onlineSearchResults = MutableStateFlow<List<OnlineTrack>>(emptyList())
    val onlineSearchResults = _onlineSearchResults.asStateFlow()

    private val _isOnlineSearching = MutableStateFlow(false)
    val isOnlineSearching = _isOnlineSearching.asStateFlow()

    private val _onlineSearchError = MutableStateFlow<String?>(null)
    val onlineSearchError = _onlineSearchError.asStateFlow()

    private val _showWelcomeScreen = mutableStateOf(libraryPersistence.isFirstLaunch())
    val showWelcomeScreen: State<Boolean> = _showWelcomeScreen

    // Contextual Hints State
    private val _visibleHints = mutableStateMapOf<String, Boolean>().apply {
        listOf(
            "library_long_press", "library_reorder",
            "player_3d", "player_swipe",
            "discover_mixes", "discover_voice",
            "eq_stacking"
        ).forEach { id ->
            val seen = libraryPersistence.hasSeenHint(id)
            put(id, !seen)
            if (!seen) startHintTimer(id)
        }
    }
    val visibleHints: Map<String, Boolean> = _visibleHints

    fun dismissHint(hintId: String) {
        _visibleHints[hintId] = false
        libraryPersistence.setHintSeen(hintId)
    }

    private fun startHintTimer(hintId: String) {
        viewModelScope.launch {
            delay(30000) // 30 seconds
            if (_visibleHints[hintId] == true) {
                dismissHint(hintId)
            }
        }
    }

    // Bitmap Cache for Ultra-Fast Scrolling
    private val _bitmapCache = LruCache<Long, Bitmap>(100) // Cache 100 bitmaps
    val bitmapCache: LruCache<Long, Bitmap> = _bitmapCache

    private var onlineSearchJob: Job? = null

    private val onlineProviders = listOf(
        SpotifyProvider(),
        AudiusProvider(), 
        JamendoProvider(), 
        MusicBrainzProvider(),
        DeezerProvider(),
        ITunesProvider(),
        BaquirProvider(),
        YouTubeProvider()
    )
    private val musicBrainz = MusicBrainzProvider()

    // Deep Insight State
    private val _deepInsights = MutableStateFlow<DeepInsight?>(null)
    val deepInsights = _deepInsights.asStateFlow()

    private val _currentYouTubeVideoId = MutableStateFlow<String?>(null)
    val currentYouTubeVideoId = _currentYouTubeVideoId.asStateFlow()

    fun searchOnline(query: String) {
        onlineSearchJob?.cancel()
        _onlineSearchQuery.value = query
        // Also update local search query to include library results in Discover
        _searchQuery.value = query
        
        if (query.isBlank()) {
            _onlineSearchResults.value = emptyList()
            _isOnlineSearching.value = false
            return
        }

        onlineSearchJob = viewModelScope.launch {
            _isOnlineSearching.value = true
            _onlineSearchError.value = null
            
            try {
                val results = mutableListOf<OnlineTrack>()
                onlineProviders.forEach { provider ->
                    try {
                        val providerResults = provider.searchTracks(query)
                        results.addAll(providerResults)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (results.isEmpty() && query.isNotBlank()) {
                    _onlineSearchError.value = "Couldn't reach online music right now."
                }
                
                // Sort by playability (MusicBrainz metadata only tracks at the bottom)
                _onlineSearchResults.value = results.sortedWith(compareByDescending<OnlineTrack> { it.streamUrl.isNotEmpty() }.thenBy { it.title })
            } catch (e: Exception) {
                _onlineSearchError.value = "Search failed. Check your connection."
            } finally {
                _isOnlineSearching.value = false
            }
        }
    }

    fun fetchDeepInsights(song: Song) {
        viewModelScope.launch {
            // Cancel previous job if still running
            _deepInsights.value = null
            
            val artistInfo = musicBrainz.fetchArtistDetails(song.artist)
            var recordingInfo = musicBrainz.fetchRecordingDetails(song.title, song.artist)
            
            // If direct title/artist match fails, try a broader search
            if (recordingInfo == null) {
                val results = musicBrainz.searchTracks("${song.title} ${song.artist}")
                if (results.isNotEmpty()) {
                    // Try to find the closest match in results
                    val bestMatch = results.first()
                    recordingInfo = musicBrainz.fetchRecordingDetails(bestMatch.title, bestMatch.artist)
                }
            }
            
            // Rich Metadata Parsing & Analysis
            var year: String? = null
            var genre: String? = null
            var label: String? = null
            val producers = mutableListOf<String>()

            recordingInfo?.let { rec ->
                val releases = rec.optJSONArray("releases")
                if (releases != null && releases.length() > 0) {
                    val firstRelease = releases.getJSONObject(0)
                    year = firstRelease.optString("date", "").take(4)
                    if (year.isEmpty()) year = null
                    
                    // Extract Label with depth analysis
                    val labelInfo = firstRelease.optJSONArray("label-info")
                    if (labelInfo != null && labelInfo.length() > 0) {
                        label = labelInfo.getJSONObject(0).optJSONObject("label")?.optString("name")
                    }
                }
                
                // Enhanced Genre Detection (Tags + Annotations)
                val tags = rec.optJSONArray("tags")
                if (tags != null && tags.length() > 0) {
                    genre = tags.getJSONObject(0).optString("name").replaceFirstChar { it.uppercase() }
                } else {
                    // Fallback: Check artist tags if song tags are empty
                    artistInfo?.optJSONArray("tags")?.let { artistTags ->
                        if (artistTags.length() > 0) {
                            genre = artistTags.getJSONObject(0).optString("name").replaceFirstChar { it.uppercase() }
                        }
                    }
                }

                // Analyze Relations for Production Credits
                val relations = rec.optJSONArray("relations")
                if (relations != null) {
                    for (i in 0 until relations.length()) {
                        val rel = relations.getJSONObject(i)
                        val type = rel.optString("type")
                        if (type == "producer" || type == "engineer" || type == "mixer") {
                            rel.optJSONObject("artist")?.optString("name")?.let { 
                                if (!producers.contains(it)) producers.add(it)
                            }
                        }
                    }
                }
            }

            _deepInsights.value = DeepInsight(
                artistInfo = artistInfo,
                recordingInfo = recordingInfo,
                year = year,
                genre = genre,
                producers = producers,
                label = label
            )
        }
    }

    private val _currentLyrics = MutableStateFlow<String?>(null)
    val currentLyrics = _currentLyrics.asStateFlow()

    fun fetchLyrics(song: Song) {
        _currentLyrics.value = null
        viewModelScope.launch {
            // Future: Use song title/artist for online fetch
            _currentLyrics.value = null
        }
    }

    fun playOnlineTrack(context: Context, track: OnlineTrack) {
        if (track.provider == "YouTube") {
            togglePlayPause() // Pause local if playing
            _currentYouTubeVideoId.value = track.id
            return
        }
        
        if (track.streamUrl.isEmpty()) {
            Toast.makeText(context, "Official metadata only. No stream available.", Toast.LENGTH_SHORT).show()
            return
        }
        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(track.streamUrl)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setArtworkUri(if (track.artUrl != null) android.net.Uri.parse(track.artUrl) else null)
                    .setExtras(Bundle().apply { 
                        putString("provider", track.provider)
                        putBoolean("is_online", true)
                    })
                    .build()
            )
            .build()
            
        controller?.setMediaItem(mediaItem)
        controller?.prepare()
        controller?.play()
        
        // Map OnlineTrack to dummy Song for UI compatibility (minimal mutation)
        val dummySong = Song(
            id = Random.nextLong(), // Temporary ID for session
            title = track.title,
            artist = track.artist,
            album = track.provider,
            duration = track.duration,
            albumArtUri = if (track.artUrl != null) android.net.Uri.parse(track.artUrl) else null,
            contentUri = android.net.Uri.parse(track.streamUrl)
        )
        _currentSong.value = dummySong
        updateThemeColors(context, dummySong)
    }

    private val _musicFolders = MutableStateFlow(libraryPersistence.getFolders())
    val musicFolders = _musicFolders.asStateFlow()

    // Cache for extracted album art colors to ensure smooth scrolling
    private val _albumArtColors = mutableStateMapOf<Long, Color>().apply {
        libraryPersistence.getColorCache().forEach { (id, argb) -> put(id, Color(argb)) }
    }
    val albumArtColors: Map<Long, Color> = _albumArtColors

    // Main Library: Pure and untouched by search
    val filteredSongs = combine(_songs, _favorites, _librarySection.asFlow()) { songs, favs, section ->
        when (section) {
            LibrarySection.Favorites -> songs.filter { it.id in favs }
            else -> songs
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Dedicated Search Results: Independent from main library view
    val searchResults = combine(_songs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            songs.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val albums = filteredSongs.map { songs ->
        songs.groupBy { it.album }.map { (name, tracks) ->
            Album(name, tracks.first().artist, tracks.first().albumArtUri, tracks)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val artists = filteredSongs.map { songs ->
        songs.groupBy { it.artist }.map { (name, tracks) ->
            Artist(name, tracks.first().albumArtUri, tracks)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    private val _dominantColor = mutableStateOf(Color(0xFF121210))
    val dominantColor: State<Color> = _dominantColor

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _playbackPosition = mutableStateOf(0L)
    val playbackPosition: State<Long> = _playbackPosition

    private val _playbackDuration = mutableStateOf(0L)
    val playbackDuration: State<Long> = _playbackDuration

    private val _shuffleModeEnabled = mutableStateOf(false)
    val shuffleModeEnabled: State<Boolean> = _shuffleModeEnabled

    private val _repeatMode = mutableStateOf(Player.REPEAT_MODE_OFF)
    val repeatMode: State<Int> = _repeatMode

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue = _currentQueue.asStateFlow()

    private var controller: MediaController? = null
    val mediaController: MediaController? get() = controller

    private lateinit var sharedImageLoader: ImageLoader

    // Settings & Management State
    private val _isScanning = mutableStateOf(false)
    val isScanning: State<Boolean> = _isScanning

    private val _sleepTimerRemaining = mutableStateOf<Int?>(null) // In minutes
    val sleepTimerRemaining: State<Int?> = _sleepTimerRemaining
    private var sleepTimerJob: Job? = null

    private val _skipSilenceEnabled = mutableStateOf(libraryPersistence.getSkipSilenceEnabled())
    val skipSilenceEnabled: State<Boolean> = _skipSilenceEnabled

    private val _resumeOnBT = mutableStateOf(libraryPersistence.getResumeOnBT())
    val resumeOnBT: State<Boolean> = _resumeOnBT

    private val _resumeOnHeadset = mutableStateOf(libraryPersistence.getResumeOnHeadset())
    val resumeOnHeadset: State<Boolean> = _resumeOnHeadset

    private val _rememberPlaybackPosition = mutableStateOf(libraryPersistence.getRememberPosition())
    val rememberPlaybackPosition: State<Boolean> = _rememberPlaybackPosition

    private val _audioSafeEnabled = mutableStateOf(libraryPersistence.getAudioSafeEnabled())
    val audioSafeEnabled: State<Boolean> = _audioSafeEnabled

    private val _showAudioSafeWarning = mutableStateOf(false)
    val showAudioSafeWarning: State<Boolean> = _showAudioSafeWarning

    private var hasWarnedForCurrentHighVolume = false

    private val _selectedAudioEngine = mutableStateOf(
        try { AudioEngine.valueOf(libraryPersistence.getAudioEngine()) } catch (e: Exception) { AudioEngine.Media3 }
    )
    val selectedAudioEngine: State<AudioEngine> = _selectedAudioEngine

    // Equalizer State
    private val _equalizerEnabled = mutableStateOf(libraryPersistence.getEqualizerEnabled())
    val equalizerEnabled: State<Boolean> = _equalizerEnabled

    private val _equalizerPreset = mutableStateOf(libraryPersistence.getEqualizerPreset())
    val equalizerPreset: State<String> = _equalizerPreset

    private val _equalizerBands = mutableStateMapOf<Int, Int>().apply {
        libraryPersistence.getEqualizerBands().forEach { (index, level) -> put(index, level) }
    }
    val equalizerBands: Map<Int, Int> = _equalizerBands

    private val _bassStrength = mutableStateOf(libraryPersistence.getEqualizerBassStrength())
    val bassStrength: State<Int> = _bassStrength

    private val _virtualizerStrength = mutableStateOf(libraryPersistence.getEqualizerVirtualizerStrength())
    val virtualizerStrength: State<Int> = _virtualizerStrength

    private val _reverbPreset = mutableStateOf(libraryPersistence.getEqualizerReverbPreset())
    val reverbPreset: State<Int> = _reverbPreset

    private val _ambience = mutableStateOf(libraryPersistence.getEqualizerAmbience())
    val ambience: State<Int> = _ambience

    private val _equalizerPreGain = mutableStateOf(libraryPersistence.getEqualizerPreGain())
    val equalizerPreGain: State<Int> = _equalizerPreGain

    private val _equalizerFrequencies = mutableStateOf<List<Int>>(emptyList())
    val equalizerFrequencies: State<List<Int>> = _equalizerFrequencies

    private val _equalizerRange = mutableStateOf<Pair<Int, Int>>(Pair(-1500, 1500))
    val equalizerRange: State<Pair<Int, Int>> = _equalizerRange

    private val _equalizerMode = mutableStateOf(
        try { EqualizerMode.valueOf(repository.context.getSharedPreferences("haven_settings", Context.MODE_PRIVATE).getString("equalizer_mode", "App")!!) } catch (e: Exception) { EqualizerMode.App }
    )
    val equalizerMode: State<EqualizerMode> = _equalizerMode

    val eqManager = com.haven.music.audio.EqualizerManager(
        initialEnabled = _equalizerEnabled.value,
        initialBassStrength = _bassStrength.value,
        initialVirtualizerStrength = _virtualizerStrength.value,
        initialReverbPreset = _reverbPreset.value,
        initialAmbience = _ambience.value,
        initialBands = _equalizerBands.toMap(),
        initialFrequencies = listOf(60, 150, 250, 500, 1000, 2000, 4000, 8000, 16000), // Default 9-band
        onEnabledChange = { setEqualizerEnabled(it) },
        onBassStrengthChange = { setBassStrength(it) },
        onVirtualizerStrengthChange = { setVirtualizerStrength(it) },
        onReverbPresetChange = { setReverbPreset(it) },
        onAmbienceChange = { setAmbience(it) },
        onBandChange = { index, level -> setEqualizerBand(index, level) },
        onResetFlat = { setEqualizerPreset("Flat") }
    )

    val libraryStats = songs.map { all ->
        val albumsCount = all.groupBy { it.album }.size
        val artistsCount = all.groupBy { it.artist }.size
        "${all.size} songs • $albumsCount albums • $artistsCount artists"
    }.stateIn(viewModelScope, SharingStarted.Lazily, "Loading library...")

    private val recEngine = RecommendationEngine(historyManager)
    private val mixGenerator = MixGenerator(recEngine, historyManager)

    val musicMixes = combine(songs, favorites) { all, favs ->
        mixGenerator.generateMixes(all, favs)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyPlayed = songs.map { all ->
        all.filter { historyManager.getLastPlayed(it.id) > 0 }
            .sortedByDescending { historyManager.getLastPlayed(it.id) }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun initController(context: Context) {
        sharedImageLoader = ImageLoader.Builder(context)
            .allowHardware(true)
            .crossfade(true)
            .build()
            
        viewModelScope.launch {
            val sessionToken = SessionToken(context, ComponentName(context, MusicService::class.java))
            controller = MediaController.Builder(context, sessionToken).buildAsync().await()
            
            // IMMEDIATE SYNC: Don't wait for listeners to fire
            syncWithController(context)

            // INSTANT RESTORATION: Use cache if available to avoid cold-start delay
            val currentCache = _songs.value
            if (controller?.currentMediaItem == null) {
                if (currentCache.isNotEmpty()) {
                    // Try to restore from persistence first
                    restoreSession(context)
                    
                    // If still null, do auto-select fallback from cache immediately
                    if (controller?.currentMediaItem == null) {
                        val firstSong = currentCache.first()
                        _currentSong.value = firstSong
                        updateThemeColors(context, firstSong)
                        
                        val mediaItems = currentCache.map { createMediaItem(it) }
                        controller?.setMediaItems(mediaItems, 0, 0L)
                        controller?.prepare()
                        
                        _playbackDuration.value = firstSong.duration
                        _playbackPosition.value = 0L
                        _isPlaying.value = false
                    }
                } else {
                    // Wait for background scan only if cache was truly empty
                    isSongsLoaded.filter { it }.first().let {
                        if (controller?.currentMediaItem == null && _songs.value.isNotEmpty()) {
                            val firstSong = _songs.value.first()
                            _currentSong.value = firstSong
                            updateThemeColors(context, firstSong)
                            val mediaItems = _songs.value.map { createMediaItem(it) }
                            controller?.setMediaItems(mediaItems, 0, 0L)
                            controller?.prepare()
                        }
                    }
                }
            } else {
                syncWithController(context)
            }
            
            // Ensure player is ready
            if (controller?.playbackState == Player.STATE_IDLE) {
                controller?.prepare()
            }
            
            // Post-restoration sync
            setAudioEngine(_selectedAudioEngine.value)
            syncEqualizerState()
            setSkipSilenceEnabled(_skipSilenceEnabled.value)
            setResumeOnBT(_resumeOnBT.value)
            setResumeOnHeadset(_resumeOnHeadset.value)

            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (!isPlaying) persistCurrentState()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentSong.value?.let { song ->
                        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                            historyManager.recordFinish(song.id)
                        } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                            historyManager.recordSkip(song.id)
                        }
                    }

                    syncWithController(context)
                    persistCurrentState()
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _playbackDuration.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
                    }
                    updateQueue()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _shuffleModeEnabled.value = shuffleModeEnabled
                    persistCurrentState()
                    updateQueue()
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = repeatMode
                    persistCurrentState()
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    updateQueue()
                    persistCurrentState()
                }
            })
            
            // Ticker for position & periodic save
            viewModelScope.launch {
                var ticks = 0
                while (isActive) {
                    if (_isPlaying.value) {
                        val pos = controller?.currentPosition ?: 0L
                        _playbackPosition.value = pos
                        ticks++
                        if (ticks >= 5) { // Every 5s
                            persistCurrentState()
                            ticks = 0
                        }
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun syncWithController(context: Context) {
        val ctrl = controller ?: return
        
        _isPlaying.value = ctrl.isPlaying
        _shuffleModeEnabled.value = ctrl.shuffleModeEnabled
        _repeatMode.value = ctrl.repeatMode
        _playbackDuration.value = ctrl.duration.coerceAtLeast(0L)
        _playbackPosition.value = ctrl.currentPosition.coerceAtLeast(0L)

        val mediaItem = ctrl.currentMediaItem
        if (mediaItem != null) {
            val songId = mediaItem.mediaId.toLongOrNull()
            val song = _songs.value.find { it.id == songId }
            
            if (song != null) {
                _currentSong.value = song
                updateThemeColors(context, song)
            } else {
                // Fallback: Create a temporary Song object from MediaItem metadata if not in library yet
                val metadata = mediaItem.mediaMetadata
                val fallbackSong = Song(
                    id = songId ?: -1L,
                    title = metadata.title?.toString() ?: "Unknown Title",
                    artist = metadata.artist?.toString() ?: "Unknown Artist",
                    album = metadata.albumTitle?.toString() ?: "Unknown Album",
                    duration = ctrl.duration.coerceAtLeast(0L),
                    albumArtUri = metadata.artworkUri,
                    contentUri = mediaItem.localConfiguration?.uri ?: android.net.Uri.EMPTY
                )
                _currentSong.value = fallbackSong
                updateThemeColors(context, fallbackSong)
            }
            
            if (ctrl.isPlaying) {
                songId?.let { historyManager.recordPlay(it) }
            }
        } else {
            // No item playing, but maybe we have a saved state to restore?
            // Usually handled by restoreSession()
        }
        
        updateQueue()
    }

    private fun restoreSession(context: Context) {
        val savedId = playbackPersistence.getSavedSongId()
        val savedPos = playbackPersistence.getSavedPosition()
        val savedQueueIds = playbackPersistence.getSavedQueueIds()
        
        if (savedId == -1L || _songs.value.isEmpty() || controller?.currentMediaItem != null) return

        val mainSong = _songs.value.find { it.id == savedId } ?: return
        
        // Build items
        val mediaItems = mutableListOf<MediaItem>()
        mediaItems.add(createMediaItem(mainSong))
        
        savedQueueIds.forEach { qId ->
            _songs.value.find { it.id == qId }?.let { mediaItems.add(createMediaItem(it)) }
        }

        controller?.setMediaItems(mediaItems, 0, savedPos)
        controller?.shuffleModeEnabled = playbackPersistence.getSavedShuffleMode()
        controller?.repeatMode = playbackPersistence.getSavedRepeatMode()
        controller?.prepare()
        
        _currentSong.value = mainSong
        updateThemeColors(context, mainSong)
        _playbackPosition.value = savedPos
        _shuffleModeEnabled.value = controller?.shuffleModeEnabled ?: false
        _repeatMode.value = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
    }

    fun persistCurrentState(room: Int = 1) {
        val song = _currentSong.value ?: return
        val ctrl = controller ?: return
        val queueIds = mutableListOf<Long>()
        for (i in ctrl.currentMediaItemIndex + 1 until ctrl.mediaItemCount) {
            ctrl.getMediaItemAt(i).mediaId.toLongOrNull()?.let { queueIds.add(it) }
        }
        
        playbackPersistence.saveState(
            songId = song.id,
            position = ctrl.currentPosition,
            queueIds = queueIds,
            shuffle = ctrl.shuffleModeEnabled,
            repeat = ctrl.repeatMode,
            room = room,
            librarySection = _librarySection.value.name,
            audioEngine = _selectedAudioEngine.value.name
        )
    }

    private fun createMediaItem(song: Song): MediaItem {
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUri)
                    .build()
            )
            .build()
    }

    private fun updateQueue() {
        controller?.let { ctrl ->
            val queue = mutableListOf<Song>()
            for (i in 0 until ctrl.mediaItemCount) {
                val mediaId = ctrl.getMediaItemAt(i).mediaId.toLongOrNull()
                _songs.value.find { it.id == mediaId }?.let { queue.add(it) }
            }
            
            val currentIndex = ctrl.currentMediaItemIndex
            if (currentIndex != -1 && currentIndex < queue.size) {
                _currentQueue.value = queue.drop(currentIndex + 1)
            } else {
                _currentQueue.value = emptyList()
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _isScanning.value = true
            // Background scan & update
            val newSongs = repository.fetchLocalSongs()
            if (newSongs != _songs.value) {
                _songs.value = newSongs
                libraryPersistence.saveLibraryCache(newSongs)
            }
            _isSongsLoaded.value = true
            _isScanning.value = false

            // Pre-cache first 20 album arts for ultra-fast initial scroll
            newSongs.take(20).forEach { song ->
                if (song.albumArtUri != null && _bitmapCache.get(song.id) == null) {
                    launch {
                        val request = ImageRequest.Builder(repository.context)
                            .data(song.albumArtUri)
                            .size(500) // High-fidelity caching
                            .allowHardware(true)
                            .build()
                        val result = sharedImageLoader.execute(request)
                        if (result is SuccessResult) {
                            (result.drawable as? BitmapDrawable)?.bitmap?.let { 
                                addToBitmapCache(song.id, it) 
                            }
                        }
                    }
                }
            }
        }
    }

    fun addMusicFolder(path: String) {
        val current = _musicFolders.value.toMutableSet()
        if (current.add(path)) {
            _musicFolders.value = current
            libraryPersistence.saveFolders(current)
            loadSongs() // Refresh
        }
    }

    fun removeMusicFolder(path: String) {
        val current = _musicFolders.value.toMutableSet()
        if (current.remove(path)) {
            _musicFolders.value = current
            libraryPersistence.saveFolders(current)
            loadSongs() // Refresh
        }
    }

    fun startSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        _sleepTimerRemaining.value = minutes
        if (minutes == null) return

        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes * 60
            while (remaining > 0) {
                delay(1000)
                remaining--
                if (remaining % 60 == 0) {
                    _sleepTimerRemaining.value = remaining / 60
                }
            }
            togglePlayPause() // Stop playback
            _sleepTimerRemaining.value = null
        }
    }

    fun setSkipSilenceEnabled(enabled: Boolean) {
        _skipSilenceEnabled.value = enabled
        val bundle = Bundle().apply { putBoolean("enabled", enabled) }
        controller?.sendCustomCommand(SessionCommand("SET_SKIP_SILENCE_ENABLED", Bundle.EMPTY), bundle)
        saveSettings()
    }

    fun setResumeOnBT(enabled: Boolean) {
        _resumeOnBT.value = enabled
        val bundle = Bundle().apply { putBoolean("enabled", enabled) }
        controller?.sendCustomCommand(SessionCommand("SET_RESUME_ON_BT", Bundle.EMPTY), bundle)
        saveSettings()
    }

    fun setResumeOnHeadset(enabled: Boolean) {
        _resumeOnHeadset.value = enabled
        val bundle = Bundle().apply { putBoolean("enabled", enabled) }
        controller?.sendCustomCommand(SessionCommand("SET_RESUME_ON_HEADSET", Bundle.EMPTY), bundle)
        saveSettings()
    }

    fun setRememberPlaybackPosition(enabled: Boolean) {
        _rememberPlaybackPosition.value = enabled
        saveSettings()
    }

    fun setAudioSafeEnabled(enabled: Boolean) {
        _audioSafeEnabled.value = enabled
        saveSettings()
    }

    fun checkVolumeThreshold(currentVolume: Int, maxVolume: Int) {
        if (!_audioSafeEnabled.value) return
        
        val threshold = (maxVolume * 0.7f).toInt()
        if (currentVolume > threshold) {
            if (!hasWarnedForCurrentHighVolume) {
                _showAudioSafeWarning.value = true
                hasWarnedForCurrentHighVolume = true
            }
        } else {
            // Reset when volume goes back below threshold
            hasWarnedForCurrentHighVolume = false
        }
    }

    fun dismissAudioSafeWarning() {
        _showAudioSafeWarning.value = false
    }

    fun setAudioEngine(engine: AudioEngine) {
        _selectedAudioEngine.value = engine
        val bundle = Bundle().apply { putString("engine", engine.name) }
        controller?.sendCustomCommand(SessionCommand("SET_AUDIO_ENGINE", Bundle.EMPTY), bundle)
        saveSettings()
        persistCurrentState()
    }

    fun setEqualizerMode(mode: EqualizerMode) {
        _equalizerMode.value = mode
        repository.context.getSharedPreferences("haven_settings", Context.MODE_PRIVATE)
            .edit().putString("equalizer_mode", mode.name).apply()
        val bundle = Bundle().apply { putString("mode", mode.name) }
        controller?.sendCustomCommand(SessionCommand("SET_EQUALIZER_MODE", Bundle.EMPTY), bundle)
        
        if (mode == EqualizerMode.App) {
            syncEqualizerState()
        }
    }

    private fun saveSettings() {
        libraryPersistence.saveSettings(
            _rememberPlaybackPosition.value, 
            _selectedAudioEngine.value.name, 
            _audioSafeEnabled.value,
            _skipSilenceEnabled.value,
            _resumeOnBT.value,
            _resumeOnHeadset.value
        )
    }

    private fun syncEqualizerState() {
        val bandsBundle = Bundle()
        _equalizerBands.forEach { (index, level) ->
            bandsBundle.putShort(index.toString(), level.toShort())
        }
        val bundle = Bundle().apply {
            putBoolean("enabled", _equalizerEnabled.value)
            putInt("strength", _bassStrength.value)
            putInt("virtualizer", _virtualizerStrength.value)
            putInt("reverb", _reverbPreset.value)
            putBundle("bands", bandsBundle)
        }
        controller?.sendCustomCommand(SessionCommand("SYNC_EQ", Bundle.EMPTY), bundle)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        val bundle = Bundle().apply { putBoolean("enabled", enabled) }
        controller?.sendCustomCommand(SessionCommand("TOGGLE_EQ", Bundle.EMPTY), bundle)
        saveEqualizerSettings()
        eqManager.updateState(enabled, _equalizerBands.toMap(), _bassStrength.value, _virtualizerStrength.value, _reverbPreset.value, _ambience.value)
    }

    fun setBassStrength(strength: Int) {
        _bassStrength.value = strength
        val bundle = Bundle().apply { putInt("strength", strength) }
        controller?.sendCustomCommand(SessionCommand("SET_BASS_STRENGTH", Bundle.EMPTY), bundle)
        saveEqualizerSettings()
        eqManager.updateState(_equalizerEnabled.value, _equalizerBands.toMap(), strength, _virtualizerStrength.value, _reverbPreset.value, _ambience.value)
    }

    fun setVirtualizerStrength(strength: Int) {
        _virtualizerStrength.value = strength
        val bundle = Bundle().apply { putInt("strength", strength) }
        controller?.sendCustomCommand(SessionCommand("SET_VIRTUALIZER_STRENGTH", Bundle.EMPTY), bundle)
        saveEqualizerSettings()
        eqManager.updateState(_equalizerEnabled.value, _equalizerBands.toMap(), _bassStrength.value, strength, _reverbPreset.value, _ambience.value)
    }

    fun setReverbPreset(preset: Int) {
        _reverbPreset.value = preset
        val bundle = Bundle().apply { putInt("preset", preset) }
        controller?.sendCustomCommand(SessionCommand("SET_REVERB_PRESET", Bundle.EMPTY), bundle)
        saveEqualizerSettings()
        eqManager.updateState(_equalizerEnabled.value, _equalizerBands.toMap(), _bassStrength.value, _virtualizerStrength.value, preset, _ambience.value)
    }

    fun setAmbience(value: Int) {
        _ambience.value = value
        // Use a hidden custom command or just map to another effect if needed. 
        // For now we'll just persist and maybe map to virtualizer/reverb tweaks if requested.
        saveEqualizerSettings()
        eqManager.updateState(_equalizerEnabled.value, _equalizerBands.toMap(), _bassStrength.value, _virtualizerStrength.value, _reverbPreset.value, value)
    }

    fun setEqualizerBand(index: Int, level: Int) {
        _equalizerBands[index] = level
        _equalizerPreset.value = "Custom"
        val bundle = Bundle().apply {
            putInt("index", index)
            putInt("level", level)
        }
        controller?.sendCustomCommand(SessionCommand("UPDATE_EQ_BAND", Bundle.EMPTY), bundle)
        saveEqualizerSettings()
        eqManager.updateState(_equalizerEnabled.value, _equalizerBands.toMap(), _bassStrength.value, _virtualizerStrength.value, _reverbPreset.value, _ambience.value)
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        val newBands = when (preset) {
            "Flat" -> {
                _bassStrength.value = 0
                _virtualizerStrength.value = 0
                _reverbPreset.value = 0
                _ambience.value = 0
                mapOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0, 6 to 0, 7 to 0, 8 to 0)
            }
            "Bass Boost" -> {
                _bassStrength.value = 800
                mapOf(0 to 800, 1 to 600, 2 to 400, 3 to 0, 4 to 0, 5 to 0, 6 to 0, 7 to 0, 8 to 0)
            }
            "PJ Haven" -> mapOf(0 to 100, 1 to 300, 2 to 300, 3 to 100, 4 to 0, 5 to 100, 6 to 100, 7 to 200, 8 to 100)
            "PJ Fern" -> mapOf(0 to 200, 1 to 200, 2 to 100, 3 to 0, 4 to 100, 5 to 200, 6 to 200, 7 to 200, 8 to 100)
            else -> _equalizerBands
        }
        newBands.forEach { (index, level) ->
            _equalizerBands[index] = level
            val bundle = Bundle().apply {
                putInt("index", index)
                putInt("level", level)
            }
            controller?.sendCustomCommand(SessionCommand("UPDATE_EQ_BAND", Bundle.EMPTY), bundle)
        }

        // Sync Effects separately
        controller?.sendCustomCommand(SessionCommand("SET_BASS_STRENGTH", Bundle.EMPTY), Bundle().apply { putInt("strength", _bassStrength.value) })
        controller?.sendCustomCommand(SessionCommand("SET_VIRTUALIZER_STRENGTH", Bundle.EMPTY), Bundle().apply { putInt("strength", _virtualizerStrength.value) })
        controller?.sendCustomCommand(SessionCommand("SET_REVERB_PRESET", Bundle.EMPTY), Bundle().apply { putInt("preset", _reverbPreset.value) })

        saveEqualizerSettings()
        eqManager.updateState(_equalizerEnabled.value, _equalizerBands.toMap(), _bassStrength.value, _virtualizerStrength.value, _reverbPreset.value, _ambience.value)
    }

    fun setEqualizerPreGain(gain: Int) {
        _equalizerPreGain.value = gain
        // Command SET_EQ_PRE_GAIN is handled in MusicService but we don't have an effect for it yet
        val bundle = Bundle().apply { putInt("gain", gain) }
        controller?.sendCustomCommand(SessionCommand("SET_EQ_PRE_GAIN", Bundle.EMPTY), bundle)
        saveEqualizerSettings()
    }

    private fun saveEqualizerSettings() {
        libraryPersistence.saveEqualizerSettings(
            enabled = _equalizerEnabled.value,
            preset = _equalizerPreset.value,
            bands = _equalizerBands.toMap(),
            preGain = _equalizerPreGain.value,
            bassStrength = _bassStrength.value,
            virtualizerStrength = _virtualizerStrength.value,
            reverbPreset = _reverbPreset.value,
            ambience = _ambience.value
        )
    }

    fun updateAlbumArtColor(songId: Long, color: Color) {
        _albumArtColors[songId] = color
        // Batch save to improve persistent operation speed
        if (_albumArtColors.size % 5 == 0) {
            libraryPersistence.saveColorCache(_albumArtColors.mapValues { it.value.toArgb() })
        }
    }

    fun clearArtworkCache(context: Context) {
        ImageLoader(context).memoryCache?.clear()
        _albumArtColors.clear()
    }

    fun setLibrarySection(section: LibrarySection) {
        _librarySection.value = section
    }

    fun moveLibrarySection(fromIndex: Int, toIndex: Int) {
        val current = _orderedLibrarySections.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _orderedLibrarySections.value = current
            libraryPersistence.saveCategoryOrder(current)
        }
    }

    fun toggleFavorite(songId: Long) {
        val currentFavs = _favorites.value
        val newFavs = if (songId in currentFavs) currentFavs - songId else currentFavs + songId
        _favorites.value = newFavs
        libraryPersistence.saveFavorites(newFavs)
    }

    fun createPlaylist(name: String): String {
        val newPlaylists = _playlists.value.toMutableList()
        val id = UUID.randomUUID().toString()
        newPlaylists.add(Playlist(id, name, emptyList()))
        _playlists.value = newPlaylists
        libraryPersistence.savePlaylists(newPlaylists)
        return id
    }

    fun addSongToPlaylist(songId: Long, playlistId: String): Boolean {
        var added = false
        val newPlaylists = _playlists.value.map { p ->
            if (p.id == playlistId) {
                if (p.songIds.contains(songId)) {
                    added = false
                    p
                } else {
                    added = true
                    p.copy(songIds = p.songIds + songId)
                }
            } else p
        }
        if (added) {
            _playlists.value = newPlaylists
            libraryPersistence.savePlaylists(newPlaylists)
        }
        return added
    }

    fun addSongsToPlaylist(songIds: List<Long>, playlistId: String) {
        val newPlaylists = _playlists.value.map { p ->
            if (p.id == playlistId) {
                val updatedIds = (p.songIds + songIds).distinct()
                p.copy(songIds = updatedIds)
            } else p
        }
        _playlists.value = newPlaylists
        libraryPersistence.savePlaylists(newPlaylists)
    }

    fun removeSongFromPlaylist(songId: Long, playlistId: String) {
        val newPlaylists = _playlists.value.map { p ->
            if (p.id == playlistId) p.copy(songIds = p.songIds - songId) else p
        }
        _playlists.value = newPlaylists
        libraryPersistence.savePlaylists(newPlaylists)
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val newPlaylists = _playlists.value.map { p ->
            if (p.id == playlistId) p.copy(name = newName) else p
        }
        _playlists.value = newPlaylists
        libraryPersistence.savePlaylists(newPlaylists)
    }

    fun deletePlaylist(playlistId: String) {
        val newPlaylists = _playlists.value.filter { it.id != playlistId }
        _playlists.value = newPlaylists
        libraryPersistence.savePlaylists(newPlaylists)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playSong(context: Context, song: Song) {
        val mediaItems = _songs.value.map { createMediaItem(it) }
        val startIndex = _songs.value.indexOf(song)
        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
        _currentSong.value = song
        updateThemeColors(context, song)
    }

    fun playSongs(context: Context, songsToPlay: List<Song>, startIndex: Int = 0) {
        if (songsToPlay.isEmpty()) return
        val mediaItems = songsToPlay.map { createMediaItem(it) }
        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
        _currentSong.value = songsToPlay[startIndex]
        updateThemeColors(context, songsToPlay[startIndex])
    }

    fun shuffleSongs(context: Context, songsToShuffle: List<Song>) {
        if (songsToShuffle.isEmpty()) return
        val shuffled = songsToShuffle.shuffled()
        playSongs(context, shuffled, 0)
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        
        if (ctrl.playbackState == Player.STATE_IDLE || ctrl.playbackState == Player.STATE_ENDED) {
            ctrl.prepare()
        }
        
        if (ctrl.isPlaying) {
            ctrl.pause()
            _isPlaying.value = false
        } else {
            ctrl.play()
            _isPlaying.value = true
        }
    }

    fun skipToNext() { controller?.seekToNext() }
    fun skipToPrevious() { controller?.seekToPrevious() }
    fun seekTo(position: Long) { controller?.seekTo(position); _playbackPosition.value = position }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun toggleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun playFromQueue(song: Song) {
        controller?.let { ctrl ->
            for (i in 0 until ctrl.mediaItemCount) {
                if (ctrl.getMediaItemAt(i).mediaId == song.id.toString()) {
                    ctrl.seekTo(i, 0L); ctrl.play()
                    break
                }
            }
        }
    }

    fun removeFromQueue(song: Song) {
        controller?.let { ctrl ->
            for (i in 0 until ctrl.mediaItemCount) {
                if (ctrl.getMediaItemAt(i).mediaId == song.id.toString()) {
                    ctrl.removeMediaItem(i)
                    break
                }
            }
        }
    }

    fun playNext(song: Song) {
        controller?.let { ctrl ->
            val nextIndex = if (ctrl.currentMediaItemIndex != -1) ctrl.currentMediaItemIndex + 1 else 0
            ctrl.addMediaItem(nextIndex, createMediaItem(song))
        }
    }

    fun addSongsToQueue(newSongs: List<Song>) {
        controller?.let { ctrl ->
            val currentUpcomingCount = if (ctrl.currentMediaItemIndex != -1) ctrl.mediaItemCount - (ctrl.currentMediaItemIndex + 1) else 0
            val remainingSpace = 9 - currentUpcomingCount
            if (remainingSpace <= 0) return
            val songsToAdd = newSongs.take(remainingSpace)
            ctrl.addMediaItems(songsToAdd.map { createMediaItem(it) })
            if (ctrl.playbackState == Player.STATE_IDLE) ctrl.prepare()
        }
    }

    fun shuffleUpcomingQueue() {
        controller?.let { ctrl ->
            val currentIndex = ctrl.currentMediaItemIndex
            if (currentIndex == -1) return
            val upcomingCount = ctrl.mediaItemCount - (currentIndex + 1)
            if (upcomingCount <= 1) return
            val upcomingItems = mutableListOf<MediaItem>()
            for (i in currentIndex + 1 until ctrl.mediaItemCount) upcomingItems.add(ctrl.getMediaItemAt(i))
            upcomingItems.shuffle()
            ctrl.removeMediaItems(currentIndex + 1, ctrl.mediaItemCount)
            ctrl.addMediaItems(currentIndex + 1, upcomingItems)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        controller?.let { ctrl ->
            val currentIndex = ctrl.currentMediaItemIndex
            if (currentIndex != -1) {
                val from = currentIndex + 1 + fromIndex
                val to = currentIndex + 1 + toIndex
                if (from < ctrl.mediaItemCount && to < ctrl.mediaItemCount) ctrl.moveMediaItem(from, to)
            }
        }
    }

    fun clearQueue() {
        controller?.let { ctrl ->
            val currentIndex = ctrl.currentMediaItemIndex
            if (currentIndex != -1 && ctrl.mediaItemCount > currentIndex + 1) ctrl.removeMediaItems(currentIndex + 1, ctrl.mediaItemCount)
        }
    }

    fun closeNowPlaying() { _currentSong.value = null }

    fun closeYouTubePlayer() {
        _currentYouTubeVideoId.value = null
    }

    fun completeOnboarding() {
        _showWelcomeScreen.value = false
        libraryPersistence.setFirstLaunchComplete()
    }

    fun addToBitmapCache(songId: Long, bitmap: Bitmap) {
        if (_bitmapCache.get(songId) == null) {
            _bitmapCache.put(songId, bitmap)
        }
    }

    private fun updateThemeColors(context: Context, song: Song) {
        // Use existing color if available
        val existingColor = _albumArtColors[song.id]
        if (existingColor != null) {
            _dominantColor.value = existingColor
        }

        viewModelScope.launch {
            val request = ImageRequest.Builder(context)
                .data(song.albumArtUri)
                .allowHardware(false) // Palette needs hardware off to extract colors
                .size(100)
                .build()
            val result = sharedImageLoader.execute(request)
            if (result is SuccessResult) { 
                (result.drawable as? BitmapDrawable)?.bitmap?.let { 
                    extractColors(song.id, it) 
                } 
            }
        }
    }

    private fun extractColors(songId: Long, bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette -> 
            palette?.dominantSwatch?.let { 
                _dominantColor.value = Color(it.rgb)
                updateAlbumArtColor(songId, Color(it.rgb))
            } 
        }
    }

    override fun onCleared() { controller?.release(); super.onCleared() }
}

data class Album(val name: String, val artist: String, val artUri: android.net.Uri?, val songs: List<Song>)
data class Artist(val name: String, val artUri: android.net.Uri?, val songs: List<Song>)

private fun <T> State<T>.asFlow(): Flow<T> = snapshotFlow { value }
