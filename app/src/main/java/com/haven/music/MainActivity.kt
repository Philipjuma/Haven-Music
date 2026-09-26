package com.haven.music

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import coil.ImageLoader
import coil.compose.AsyncImage
import com.haven.music.ui.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import kotlin.math.absoluteValue
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val _intentFlow = MutableStateFlow<Intent?>(null)
    private val viewModel: MainViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this, MainViewModelFactory(MusicRepository(applicationContext)))[MainViewModel::class.java]
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _intentFlow.value = intent
    }

    override fun onResume() {
        super.onResume()
        // Always scan for local music on return to capture new device files
        viewModel.loadSongs()
    }

    override fun onPause() {
        super.onPause()
        viewModel.persistCurrentState()
    }

    override fun onStop() {
        super.onStop()
        viewModel.persistCurrentState()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        viewModel.trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        viewModel.trimMemory(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _intentFlow.value = intent
        
        // Request High Refresh Rate (90Hz/120Hz)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = display
            val modes = display?.supportedModes
            val highRefreshMode = modes?.maxByOrNull { it.refreshRate }
            if (highRefreshMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = highRefreshMode.modeId
                window.attributes = params
            }
        } else {
            val params = window.attributes
            params.preferredRefreshRate = 120f
            window.attributes = params
        }

        enableEdgeToEdge()
        
        val versionName = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        } catch (e: Exception) {
            "1.0.0"
        } ?: "1.0.0"

        setContent {
            val context = LocalContext.current
            val haptic = LocalHapticFeedback.current
            val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            val viewModel: MainViewModel = this.viewModel

            val speechLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    val data = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    if (!data.isNullOrEmpty()) {
                        viewModel.setSearchQuery(data[0])
                        viewModel.searchOnline(data[0])
                    }
                }
            }

            val triggerVoiceSearch = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for music...")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Voice search not supported", Toast.LENGTH_SHORT).show()
                }
            }
            
            val dominantColor by viewModel.dominantColor
            val allSongs by viewModel.songs.collectAsState()
            val filteredSongs by viewModel.filteredSongs.collectAsState()
            val albums by viewModel.albums.collectAsState()
            val artists by viewModel.artists.collectAsState()
            val playlists by viewModel.playlists.collectAsState()
            val searchQuery by viewModel.searchQuery.collectAsState()
            val librarySection by viewModel.librarySection
            val favorites by viewModel.favorites.collectAsState()
            
            val currentSong by viewModel.currentSong
            val isPlaying by viewModel.isPlaying
            val position by viewModel.playbackPosition
            val duration by viewModel.playbackDuration
            
            val shuffleModeEnabled by viewModel.shuffleModeEnabled
            val repeatMode by viewModel.repeatMode
            val selectedEngine by viewModel.selectedAudioEngine
            val skipSilenceEnabled by viewModel.skipSilenceEnabled
            val resumeOnBT by viewModel.resumeOnBT
            val resumeOnHeadset by viewModel.resumeOnHeadset
            val queue by viewModel.currentQueue.collectAsState()
            val musicMixes by viewModel.musicMixes.collectAsState()

            // Online Discovery State
            val onlineSearchQuery by viewModel.onlineSearchQuery.collectAsState()
            val onlineSearchResults by viewModel.onlineSearchResults.collectAsState()
            val isOnlineSearching by viewModel.isOnlineSearching.collectAsState()
            val onlineSearchError by viewModel.onlineSearchError.collectAsState()
            
            val audioSafeEnabled by viewModel.audioSafeEnabled
            val adaptiveControlsEnabled by viewModel.adaptiveControlsEnabled
            val showAudioSafeWarning by viewModel.showAudioSafeWarning
            val showWelcomeScreen by viewModel.showWelcomeScreen

            // Volume monitoring
            val audioManager = remember { context.getSystemService(AUDIO_SERVICE) as AudioManager }
            LaunchedEffect(audioSafeEnabled) {
                if (!audioSafeEnabled) return@LaunchedEffect
                
                while (isActive) {
                    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    viewModel.checkVolumeThreshold(currentVolume, maxVolume)
                    delay(500) // Check every 500ms
                }
            }
            
            // Navigation state: Room 0: Library, 1: Now Playing (Pivot), 2: Discover
            val pagerState = rememberPagerState(
                initialPage = remember { 
                    val saved = context.getSharedPreferences("haven_playback", 0).getInt("last_room", 1)
                    if (saved > 2) 1 else saved 
                }, 
                pageCount = { 3 }
            )
            val scope = rememberCoroutineScope()

            // Handle incoming intents for notification taps
            val incomingIntent by _intentFlow.collectAsState()
            LaunchedEffect(incomingIntent) {
                if (incomingIntent?.action == "OPEN_PLAYER") {
                    pagerState.scrollToPage(1)
                }
            }
            
            // Hoisted List State for Cross-Room interaction
            val libraryListState = rememberLazyListState()
            
            // UI State - DECOUPLED
            val orderedSections by viewModel.orderedLibrarySections.collectAsState()
            var isNavExpanded by remember { mutableStateOf(false) }
            var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
            
            var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }
            var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
            var selectedSongsForPlaylist by remember { mutableStateOf<List<Song>?>(null) }
            var playlistTargetForAdd by remember { mutableStateOf<Playlist?>(null) }
            
            var selectedArtistName by remember { mutableStateOf<String?>(null) }
            var selectedAlbumName by remember { mutableStateOf<String?>(null) }
            var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
            var selectedMix by remember { mutableStateOf<MusicMix?>(null) }

            var selectedAlbumForMenu by remember { mutableStateOf<Album?>(null) }
            var selectedArtistForMenu by remember { mutableStateOf<Artist?>(null) }

            var showSettings by remember { mutableStateOf(false) }
            var showEqualizer by remember { mutableStateOf(false) }
            var showSearchOverlay by remember { mutableStateOf(false) }
            
            var showSongSelection by remember { mutableStateOf(false) }
            var showSongSelectionForQueue by remember { mutableStateOf(false) }
            var scrollProgress by remember { mutableStateOf(0f) }
            var shouldRequestSearchFocus by remember { mutableStateOf(false) }

            var selectedOnlineTrackForMenu by remember { mutableStateOf<OnlineTrack?>(null) }

            // Scroll to hide logic
            var isHubVisible by remember { mutableStateOf(true) }

            val equalizerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { }

            val folderPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                if (uri != null) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    Toast.makeText(context, "Music folder added", Toast.LENGTH_SHORT).show()
                    viewModel.addMusicFolder(uri.toString())
                }
            }

            // Auto-collapse timer (12 seconds of inactivity)
            LaunchedEffect(isNavExpanded, lastInteractionTime) {
                if (isNavExpanded) {
                    delay(12000)
                    isNavExpanded = false
                }
            }

            // Persistence & Smart Visibility
            LaunchedEffect(pagerState.currentPage) {
                viewModel.persistCurrentState(room = pagerState.currentPage)
                lastInteractionTime = System.currentTimeMillis()
            }

            // Show Nav Dock when playback starts
            LaunchedEffect(isPlaying) {
                if (isPlaying) {
                    isNavExpanded = true
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
            
            var hasPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
                    } else {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    }
                )
            }
            
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasPermission = isGranted
                if (isGranted) viewModel.loadSongs()
            }
            
            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    viewModel.loadSongs()
                    @androidx.media3.common.util.UnstableApi
                    viewModel.initController(context)
                } else {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                    permissionLauncher.launch(permission)
                }
            }
            
            // BackHandler with Priority
            val youtubeVideoId by viewModel.currentYouTubeVideoId.collectAsState()
            
            BackHandler(enabled = youtubeVideoId != null || showEqualizer || showSongSelectionForQueue || showSettings || showSearchOverlay || showSongSelection || selectedSongForMenu != null || selectedSongForPlaylist != null || playlistTargetForAdd != null || selectedArtistName != null || selectedAlbumName != null || selectedPlaylist != null || selectedMix != null || selectedAlbumForMenu != null || selectedArtistForMenu != null || pagerState.currentPage != 1) {
                keyboardController?.hide()
                focusManager.clearFocus()
                
                if (youtubeVideoId != null) viewModel.closeYouTubePlayer()
                else if (showEqualizer) showEqualizer = false
                else if (showSongSelectionForQueue) showSongSelectionForQueue = false
                else if (showSettings) showSettings = false
                else if (showSearchOverlay) showSearchOverlay = false
                else if (showSongSelection) showSongSelection = false
                else if (playlistTargetForAdd != null) playlistTargetForAdd = null
                else if (selectedSongForPlaylist != null) selectedSongForPlaylist = null
                else if (selectedSongForMenu != null) selectedSongForMenu = null
                else if (selectedAlbumForMenu != null) selectedAlbumForMenu = null
                else if (selectedArtistForMenu != null) selectedArtistForMenu = null
                else if (selectedPlaylist != null) selectedPlaylist = null
                else if (selectedMix != null) selectedMix = null
                else if (selectedArtistName != null) selectedArtistName = null
                else if (selectedAlbumName != null) selectedAlbumName = null
                else scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
            }

            // Atmosphere Animations
            val atmosphereAlpha by animateFloatAsState(
                targetValue = 0.85f - (scrollProgress * 0.15f),
                animationSpec = tween(200)
            )
            val atmosphereDarkness by animateFloatAsState(
                targetValue = if (pagerState.currentPage == 1) (0.42f + (scrollProgress * 0.18f)) else 0.6f, 
                animationSpec = tween(200)
            )
            val atmosphereBlur by animateFloatAsState(
                targetValue = if (pagerState.currentPage == 1) (440f - (scrollProgress * 140f)) else 320f,
                animationSpec = tween(200)
            )
            
            val desaturateMatrix = remember { ColorMatrix() }

            // Global Keyboard Dismissal on Room Change
            LaunchedEffect(pagerState.currentPage) {
                keyboardController?.hide()
            }

            HavenTheme(dominantColor = dominantColor) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .graphicsLayer { this.alpha = 1f }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                keyboardController?.hide() // Hide keyboard when tapping background
                                focusManager.clearFocus()
                            })
                        }
                ) {
                    if (currentSong != null) {
                        AsyncImage(
                            model = currentSong!!.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { this.alpha = atmosphereAlpha }
                                .blur(atmosphereBlur.dp),
                            contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(desaturateMatrix)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { this.alpha = 1f }
                                .background(Color.Black.copy(alpha = atmosphereDarkness))
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))
                    }

                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // PREMIUM ROOM TRANSITION (Pager with Depth Parallax & Elastic Scaling)
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize().clipToBounds(),
                                beyondViewportPageCount = 1, 
                                pageSpacing = 0.dp,
                                userScrollEnabled = true
                            ) { page ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clipToBounds()
                                        .graphicsLayer {
                                            // 120Hz GPU RenderNode-Accelerated 3D See-Saw Pivot Transition
                                            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                            val absOffset = pageOffset.absoluteValue
                                            val easedProgress = FastOutSlowInEasing.transform(absOffset.coerceIn(0f, 1f))
                                            
                                            alpha = (1f - (easedProgress * 0.35f)).coerceIn(0f, 1f)
                                            scaleX = 0.96f + (0.04f * (1f - easedProgress))
                                            scaleY = 0.96f + (0.04f * (1f - easedProgress))
                                            rotationY = pageOffset * -10f
                                            transformOrigin = TransformOrigin(if (pageOffset > 0) 0f else 1f, 0.5f)
                                            clip = true
                                        }
                                ) {
                                    when (page) {
                                        0 -> {
                                            MusicListScreen(
                                                songs = filteredSongs,
                                                albums = albums,
                                                artists = artists,
                                                playlists = playlists,
                                                currentSong = currentSong,
                                                librarySection = librarySection,
                                                favorites = favorites,
                                                dominantColor = dominantColor,
                                                mixes = musicMixes,
                                                orderedSections = orderedSections,
                                                viewModel = viewModel,
                                                onSectionChange = {
                                                    viewModel.setLibrarySection(it)
                                                    viewModel.persistCurrentState(room = 0)
                                                },
                                                onMoveSection = { from, to -> viewModel.moveLibrarySection(from, to) },
                                                onSongClick = { 
                                                    viewModel.playSong(context, it)
                                                    scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                                },
                                                onSongLongClick = { selectedSongForMenu = it },
                                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                                onMixClick = { mix ->
                                                    selectedMix = mix
                                                },
                                                onRefreshMixes = { 
                                                    viewModel.loadSongs()
                                                    if (onlineSearchQuery.isNotBlank()) viewModel.searchOnline(onlineSearchQuery)
                                                },
                                                onSearchIconClick = { 
                                                    showSearchOverlay = true
                                                },
                                                onReturnToPlayer = {
                                                    scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                                },
                                                isHubVisible = isHubVisible,
                                                onHubVisibilityChange = { isHubVisible = it },
                                                onAlbumClick = { selectedAlbumName = it.name },
                                                onAlbumLongClick = { selectedAlbumForMenu = it },
                                                onArtistClick = { selectedArtistName = it.name },
                                                onArtistLongClick = { selectedArtistForMenu = it },
                                                onPlaylistClick = { selectedPlaylist = it },
                                                onNoResult = { letter ->
                                                    viewModel.searchOnline(letter.toString())
                                                    scope.launch { pagerState.animateScrollToPage(2, animationSpec = PremiumSpring) }
                                                },
                                                listState = libraryListState
                                            )
                                        }
                                        1 -> {
                                            val musicFolders by viewModel.musicFolders.collectAsState()
                                            
                                            NowPlayingScreen(
                                                song = currentSong,
                                                isPlaying = isPlaying,
                                                position = position,
                                                duration = duration,
                                                shuffleModeEnabled = shuffleModeEnabled,
                                                repeatMode = repeatMode,
                                                isFavorite = currentSong?.id in favorites,
                                                dominantColor = dominantColor,
                                                cachedBitmap = currentSong?.let { viewModel.bitmapCache.get(it.id) },
                                                onBitmapLoaded = { currentSong?.let { song -> viewModel.addToBitmapCache(song.id, it) } },
                                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                                onNext = { viewModel.skipToNext() },
                                                onPrevious = { viewModel.skipToPrevious() },
                                                onSeek = { viewModel.seekTo(it) },
                                                onToggleShuffle = { viewModel.toggleShuffle() },
                                                onToggleRepeat = { viewModel.toggleRepeat() },
                                                onToggleFavorite = { currentSong?.let { viewModel.toggleFavorite(it.id) } },
                                                onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                                                onSettingsClick = { showSettings = true },
                                                onAddFolderClick = { folderPickerLauncher.launch(null) },
                                                onGoToArtist = {
                                                    selectedArtistName = it
                                                    scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                                },
                                                onGoToAlbum = {
                                                    selectedAlbumName = it
                                                    scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                                },
                                                musicFolders = musicFolders,
                                                onBack = { scope.launch { pagerState.animateScrollToPage(0, animationSpec = PremiumSpring) } },
                                                viewModel = viewModel,
                                                nextSong = queue.firstOrNull(),
                                                onUpNextClick = { targetSong ->
                                                    viewModel.setLibrarySection(LibrarySection.Songs)
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(0, animationSpec = PremiumSpring)
                                                        val songIndex = filteredSongs.indexOfFirst { it.id == targetSong.id }
                                                        if (songIndex != -1) {
                                                            libraryListState.animateScrollToItem(songIndex)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        2 -> {
                                            DiscoverScreen(
                                                currentSong = currentSong,
                                                mixes = musicMixes,
                                                onlineSearchQuery = onlineSearchQuery,
                                                onlineSearchResults = onlineSearchResults,
                                                isOnlineSearching = isOnlineSearching,
                                                onlineSearchError = onlineSearchError,
                                                viewModel = viewModel,
                                                onOnlineSearch = { viewModel.searchOnline(it) },
                                                onVoiceClick = { triggerVoiceSearch() },
                                                onOnlineTrackClick = { viewModel.playOnlineTrack(context, it) },
                                                onOnlineTrackLongClick = { 
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    selectedOnlineTrackForMenu = it 
                                                },
                                                onMixClick = { mix ->
                                                    selectedMix = mix
                                                },
                                                onSongClick = { 
                                                    viewModel.playSong(context, it)
                                                    scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                                },
                                                onRefresh = { viewModel.loadSongs() },
                                                shouldRequestFocus = shouldRequestSearchFocus,
                                                onFocusHandled = { shouldRequestSearchFocus = false }
                                            )
                                        }
                                    }
                                }
                            }

                            // PILL-SHAPED BOTTOM HUB & MINI PLAYER
                            AnimatedVisibility(
                                visible = if (pagerState.currentPage == 1) true else isHubVisible,
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                                modifier = Modifier.align(Alignment.BottomCenter)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Global Mini Player (Visible when not in Now Playing Room)
                                    if (pagerState.currentPage != 1 && currentSong != null) {
                                        GlobalMiniPlayer(
                                            song = currentSong!!,
                                            isPlaying = isPlaying,
                                            dominantColor = dominantColor,
                                            cachedBitmap = viewModel.bitmapCache.get(currentSong!!.id),
                                            onBitmapLoaded = { viewModel.addToBitmapCache(currentSong!!.id, it) },
                                            onTogglePlayPause = { viewModel.togglePlayPause() },
                                            onNext = { viewModel.skipToNext() },
                                            onClick = { scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) } }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    BottomControlHub(
                                        selectedPage = pagerState.currentPage,
                                        dominantColor = dominantColor,
                                        adaptiveEnabled = adaptiveControlsEnabled,
                                        onPageSelected = { page ->
                                            keyboardController?.hide() // Explicit hide on room change
                                            focusManager.clearFocus()
                                            scope.launch { pagerState.animateScrollToPage(page, animationSpec = PremiumSpring) }
                                        },
                                        onSearchClick = { 
                                            showSearchOverlay = true
                                        }
                                    )
                                }
                            }

                            // Settings Screen Overlay
                            AnimatedVisibility(
                                visible = showSettings,
                                enter = slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)),
                                exit = slideOutHorizontally(tween(400)) { it } + fadeOut(tween(400))
                            ) {
                                val isScanning by viewModel.isScanning
                                val sleepTimerMinutes by viewModel.sleepTimerRemaining
                                val rememberPosition by viewModel.rememberPlaybackPosition
                                val equalizerMode by viewModel.equalizerMode
                                val musicFolders by viewModel.musicFolders.collectAsState()
                                val stats by viewModel.libraryStats.collectAsState()

                                SettingsScreen(
                                    versionName = versionName,
                                    libraryStats = stats,
                                    isScanning = isScanning,
                                    rememberPosition = rememberPosition,
                                    sleepTimerMinutes = sleepTimerMinutes,
                                    selectedEngine = selectedEngine,
                                    equalizerMode = equalizerMode,
                                    musicFolders = musicFolders,
                                    onBack = { showSettings = false },
                                    onEqualizerClick = {
                                        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, viewModel.mediaController?.sessionActivity?.creatorUid ?: 0)
                                            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                                            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                                        }
                                        try {
                                            equalizerLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "System equalizer unavailable on this device.", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onEqualizerModeSelected = { viewModel.setEqualizerMode(it) },
                                    onAddFolderClick = { folderPickerLauncher.launch(null) },
                                    onRemoveFolderClick = { viewModel.removeMusicFolder(it) },
                                    onRescanClick = {
                                        viewModel.loadSongs()
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "Music library updated.", Toast.LENGTH_SHORT).show()
                                    },
                                    onSleepTimerClick = {
                                        val next = when (sleepTimerMinutes) {
                                            null -> 15
                                            15 -> 30
                                            30 -> 60
                                            else -> null
                                        }
                                        viewModel.startSleepTimer(next)
                                    },
                                    onToggleRememberPosition = { viewModel.setRememberPlaybackPosition(it) },
                                    skipSilenceEnabled = skipSilenceEnabled,
                                    onToggleSkipSilence = { viewModel.setSkipSilenceEnabled(it) },
                                    resumeOnBT = resumeOnBT,
                                    onToggleResumeOnBT = { viewModel.setResumeOnBT(it) },
                                    resumeOnHeadset = resumeOnHeadset,
                                    onToggleResumeOnHeadset = { viewModel.setResumeOnHeadset(it) },
                                    onClearCacheClick = {
                                        viewModel.clearArtworkCache(context)
                                        Toast.makeText(context, "Artwork cache cleared.", Toast.LENGTH_SHORT).show()
                                    },
                                    onEngineSelected = { viewModel.setAudioEngine(it) },
                                    onAppEqualizerClick = { showEqualizer = true },
                                    audioSafeEnabled = audioSafeEnabled,
                                    onToggleAudioSafe = { viewModel.setAudioSafeEnabled(it) },
                                    adaptiveControlsEnabled = viewModel.adaptiveControlsEnabled.value,
                                    onToggleAdaptiveControls = { viewModel.setAdaptiveControlsEnabled(it) }
                                )
                            }

                            // Audio Safe Warning Popup
                            if (showAudioSafeWarning) {
                                Dialog(onDismissRequest = { viewModel.dismissAudioSafeWarning() }) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .wrapContentHeight(),
                                        shape = RoundedCornerShape(28.dp),
                                        color = Color(0xFF1A1A1A).copy(alpha = 0.95f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                        shadowElevation = 24.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.Default.Warning, 
                                                contentDescription = null, 
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                "HAVEN AUDIOSAFE", 
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Black, 
                                                    letterSpacing = 1.sp,
                                                    fontSize = 16.sp
                                                ),
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                "Listening at high volumes for long periods may damage your hearing.",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                                color = Color.White.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Button(
                                                onClick = { viewModel.dismissAudioSafeWarning() },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxWidth().height(48.dp)
                                            ) {
                                                Text("I UNDERSTAND", fontWeight = FontWeight.Bold, color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }

                            // Equalizer Screen Overlay
                            AnimatedVisibility(
                                visible = showEqualizer,
                                enter = slideInHorizontally(tween(400), initialOffsetX = { it }) + fadeIn(tween(400)),
                                exit = slideOutHorizontally(tween(400), targetOffsetX = { it }) + fadeOut(tween(400))
                            ) {
                                EqualizerScreen(
                                    eqManager = viewModel.eqManager,
                                    viewModel = viewModel,
                                    onBack = { showEqualizer = false }
                                )
                            }

                            // Search Screen Overlay
                            AnimatedVisibility(
                                visible = showSearchOverlay,
                                enter = slideInVertically(tween(400)) { -it } + fadeIn(tween(400)),
                                exit = slideOutVertically(tween(400)) { -it } + fadeOut(tween(400))
                            ) {
                                val searchResults by viewModel.searchResults.collectAsState()
                                SearchScreen(
                                    query = searchQuery,
                                    searchResults = searchResults,
                                    currentSong = currentSong,
                                    viewModel = viewModel,
                                    onQueryChange = { viewModel.setSearchQuery(it) },
                                    onVoiceClick = { triggerVoiceSearch() },
                                    onSongClick = {
                                        viewModel.playSong(context, it)
                                        showSearchOverlay = false
                                        scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                    },
                                    onExternalSearch = { query, provider -> openExternalSearch(query, provider) },
                                    onBack = { showSearchOverlay = false }
                                )
                            }

                            // Artist Screen Overlay
                            AnimatedVisibility(
                                visible = selectedArtistName != null,
                                enter = slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)),
                                exit = slideOutHorizontally(tween(400)) { it } + fadeOut(tween(400))
                            ) {
                                if (selectedArtistName != null) {
                                    val artistSongs = allSongs.filter { it.artist == selectedArtistName }
                                    val artistAlbums = albums.filter { it.artist == selectedArtistName }
                                    ArtistScreen(
                                        artistName = selectedArtistName!!,
                                        artistSongs = artistSongs,
                                        artistAlbums = artistAlbums,
                                        viewModel = viewModel,
                                        onBack = { selectedArtistName = null },
                                        onSongClick = {
                                            viewModel.playSong(context, it)
                                            selectedArtistName = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onPlayAll = {
                                            viewModel.playSongs(context, it)
                                            selectedArtistName = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onShuffleAll = {
                                            viewModel.shuffleSongs(context, it)
                                            selectedArtistName = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onAlbumClick = { selectedAlbumName = it.name }
                                    )
                                }
                            }

                            // Album Screen Overlay
                            AnimatedVisibility(
                                visible = selectedAlbumName != null,
                                enter = slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)),
                                exit = slideOutHorizontally(tween(400)) { it } + fadeOut(tween(400))
                            ) {
                                if (selectedAlbumName != null) {
                                    val albumSongs = allSongs.filter { it.album == selectedAlbumName }
                                    AlbumScreen(
                                        albumName = selectedAlbumName!!,
                                        artistName = albumSongs.firstOrNull()?.artist ?: "Unknown Artist",
                                        albumArtUri = albumSongs.firstOrNull()?.albumArtUri,
                                        albumSongs = albumSongs,
                                        viewModel = viewModel,
                                        onBack = { selectedAlbumName = null },
                                        onSongClick = {
                                            viewModel.playSong(context, it)
                                            selectedAlbumName = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onSongLongClick = { selectedSongForMenu = it },
                                        onPlayAll = {
                                            viewModel.playSongs(context, it)
                                            selectedAlbumName = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onShuffleAll = {
                                            viewModel.shuffleSongs(context, it)
                                            selectedAlbumName = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onAddToPlaylist = {
                                            selectedSongsForPlaylist = albumSongs
                                        },
                                        onToggleFavorite = {
                                            albumSongs.forEach { viewModel.toggleFavorite(it.id) }
                                            Toast.makeText(context, "Album songs toggled in favorites", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }

                            // Music Mix Detail Overlay
                            AnimatedVisibility(
                                visible = selectedMix != null,
                                enter = slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)),
                                exit = slideOutHorizontally(tween(400)) { it } + fadeOut(tween(400))
                            ) {
                                if (selectedMix != null) {
                                    MixDetailScreen(
                                        mix = selectedMix!!,
                                        currentSong = currentSong,
                                        viewModel = viewModel,
                                        onBack = { selectedMix = null },
                                        onSongClick = {
                                            viewModel.playSong(context, it)
                                            selectedMix = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onSongLongClick = { selectedSongForMenu = it },
                                        onPlayAll = {
                                            viewModel.playSongs(context, it)
                                            selectedMix = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onShuffleAll = {
                                            viewModel.shuffleSongs(context, it)
                                            selectedMix = null
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        }
                                    )
                                }
                            }

                            // Playlist Screen Overlay
                            AnimatedVisibility(
                                visible = selectedPlaylist != null,
                                enter = slideInHorizontally(tween(400)) { it } + fadeIn(tween(400)),
                                exit = slideOutHorizontally(tween(400)) { it } + fadeOut(tween(400))
                            ) {
                                if (selectedPlaylist != null) {
                                    val playlistSongs = allSongs.filter { it.id in selectedPlaylist!!.songIds }
                                    PlaylistScreen(
                                        playlist = selectedPlaylist!!,
                                        playlistSongs = playlistSongs,
                                        currentSong = currentSong,
                                        viewModel = viewModel,
                                        onBack = { selectedPlaylist = null },
                                        onSongClick = {
                                            viewModel.playSong(context, it)
                                            scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                        },
                                        onSongLongClick = { selectedSongForMenu = it },
                                        onAddSongs = { playlistTargetForAdd = selectedPlaylist },
                                        onRename = { newName -> viewModel.renamePlaylist(selectedPlaylist!!.id, newName) },
                                        onDelete = {
                                            viewModel.deletePlaylist(selectedPlaylist!!.id)
                                            selectedPlaylist = null
                                        },
                                        onRemoveFromPlaylist = { song ->
                                            viewModel.removeSongFromPlaylist(song.id, selectedPlaylist!!.id)
                                            Toast.makeText(context, "Removed from ${selectedPlaylist!!.name}", Toast.LENGTH_SHORT).show()
                                            selectedPlaylist = viewModel.playlists.value.find { it.id == selectedPlaylist!!.id }
                                        }
                                    )
                                }
                            }

                            // Playlist Selection Overlay
                            AnimatedVisibility(
                                visible = selectedSongForPlaylist != null || selectedSongsForPlaylist != null,
                                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.96f),
                                exit = fadeOut(tween(400)) + scaleOut(targetScale = 0.96f)
                            ) {
                                if (selectedSongForPlaylist != null || selectedSongsForPlaylist != null) {
                                    PlaylistSelectionScreen(
                                        playlists = playlists,
                                        onBack = { 
                                            selectedSongForPlaylist = null
                                            selectedSongsForPlaylist = null
                                        },
                                        onPlaylistSelected = { playlist ->
                                            if (selectedSongForPlaylist != null) {
                                                val added = viewModel.addSongToPlaylist(selectedSongForPlaylist!!.id, playlist.id)
                                                if (added) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Already in ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            } else if (selectedSongsForPlaylist != null) {
                                                viewModel.addSongsToPlaylist(selectedSongsForPlaylist!!.map { it.id }, playlist.id)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                Toast.makeText(context, "Added ${selectedSongsForPlaylist!!.size} songs to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                            }
                                            selectedSongForPlaylist = null
                                            selectedSongsForPlaylist = null
                                        },
                                        onCreateNewPlaylist = { name ->
                                            val playlistId = viewModel.createPlaylist(name)
                                            if (selectedSongForPlaylist != null) {
                                                viewModel.addSongToPlaylist(selectedSongForPlaylist!!.id, playlistId)
                                                Toast.makeText(context, "Created $name and added song", Toast.LENGTH_SHORT).show()
                                            } else if (selectedSongsForPlaylist != null) {
                                                viewModel.addSongsToPlaylist(selectedSongsForPlaylist!!.map { it.id }, playlistId)
                                                Toast.makeText(context, "Created $name and added ${selectedSongsForPlaylist!!.size} songs", Toast.LENGTH_SHORT).show()
                                            }
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedSongForPlaylist = null
                                            selectedSongsForPlaylist = null
                                        }
                                    )
                                }
                            }

                            // Bulk Add Songs to Playlist
                            AnimatedVisibility(
                                visible = playlistTargetForAdd != null,
                                enter = slideInVertically(tween(200), initialOffsetY = { it }) + fadeIn(tween(200)),
                                exit = slideOutVertically(tween(200), targetOffsetY = { it }) + fadeOut(tween(200))
                            ) {
                                if (playlistTargetForAdd != null) {
                                    SongSelectionScreen(
                                        title = "Add to ${playlistTargetForAdd!!.name}",
                                        allSongs = allSongs,
                                        onSongsSelected = { selected ->
                                            viewModel.addSongsToPlaylist(selected.map { it.id }, playlistTargetForAdd!!.id)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            Toast.makeText(context, "Added ${selected.size} songs", Toast.LENGTH_SHORT).show()
                                            selectedPlaylist = viewModel.playlists.value.find { it.id == playlistTargetForAdd!!.id }
                                            playlistTargetForAdd = null
                                        },
                                        onDismiss = { playlistTargetForAdd = null }
                                    )
                                }
                            }

                            // Song Action Menu (Long Press)
                            if (selectedSongForMenu != null && selectedSongForPlaylist == null && playlistTargetForAdd == null) {
                                val song = selectedSongForMenu!!
                                PremiumActionMenu(
                                    title = song.title,
                                    subtitle = song.artist,
                                    artUri = song.albumArtUri,
                                    isFavorite = song.id in favorites,
                                    isOnline = song.isOnline,
                                    provider = song.provider,
                                    onDismiss = { selectedSongForMenu = null },
                                    onPlay = {
                                        viewModel.playSong(context, song)
                                        scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(song.id)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onGoToArtist = {
                                        selectedArtistName = song.artist
                                        selectedSongForMenu = null
                                    },
                                    onGoToAlbum = {
                                        selectedAlbumName = song.album
                                        selectedSongForMenu = null
                                    },
                                    onLyricsClick = {
                                        val query = "${song.title} ${song.artist} lyrics"
                                        openExternalSearch(query, "Google")
                                        selectedSongForMenu = null
                                    }
                                )
                            }

                            // Album Action Menu
                            if (selectedAlbumForMenu != null) {
                                AlbumActionMenu(
                                    album = selectedAlbumForMenu!!,
                                    onDismiss = { selectedAlbumForMenu = null },
                                    onOpen = { selectedAlbumName = selectedAlbumForMenu!!.name },
                                    onPlay = {
                                        viewModel.playSongs(context, selectedAlbumForMenu!!.songs)
                                        scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                    },
                                    onAddToQueue = {
                                        viewModel.addSongsToQueue(selectedAlbumForMenu!!.songs)
                                        Toast.makeText(context, "Album added to queue", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            // Artist Action Menu
                            if (selectedArtistForMenu != null) {
                                ArtistActionMenu(
                                    artist = selectedArtistForMenu!!,
                                    onDismiss = { selectedArtistForMenu = null },
                                    onOpen = { selectedArtistName = selectedArtistForMenu!!.name },
                                    onPlay = {
                                        viewModel.playSongs(context, selectedArtistForMenu!!.songs)
                                        scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                    },
                                    onAddToQueue = {
                                        viewModel.addSongsToQueue(selectedArtistForMenu!!.songs)
                                        Toast.makeText(context, "Artist songs added to queue", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }

                            // Song Selection Overlay (Queue)
                            AnimatedVisibility(
                                visible = showSongSelectionForQueue,
                                enter = slideInVertically(tween(200), initialOffsetY = { it }) + fadeIn(tween(200)),
                                exit = slideOutVertically(tween(200), targetOffsetY = { it }) + fadeOut(tween(200))
                            ) {
                                SongSelectionScreen(
                                    title = "Add to Queue",
                                    allSongs = allSongs,
                                    onSongsSelected = { selected ->
                                        viewModel.addSongsToQueue(selected)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showSongSelectionForQueue = false
                                    },
                                    onDismiss = { showSongSelectionForQueue = false }
                                )
                            }

                            // Online Track Action Menu
                            if (selectedOnlineTrackForMenu != null) {
                                val track = selectedOnlineTrackForMenu!!
                                val trackLongId = track.id.hashCode().toLong()
                                val isFav = favorites.contains(trackLongId)
                                
                                PremiumActionMenu(
                                    title = track.title,
                                    subtitle = track.artist,
                                    artUri = track.artUrl?.let { Uri.parse(it) },
                                    isFavorite = isFav,
                                    isOnline = true,
                                    provider = track.provider,
                                    onDismiss = { selectedOnlineTrackForMenu = null },
                                    onPlay = { 
                                        viewModel.playOnlineTrack(context, track)
                                        scope.launch { pagerState.animateScrollToPage(1, animationSpec = PremiumSpring) }
                                    },
                                    onToggleFavorite = {
                                        viewModel.toggleFavorite(trackLongId)
                                        // Also ensure it's in the lib cache if favorited
                                        if (!isFav) {
                                            val dummy = Song(
                                                id = trackLongId,
                                                title = track.title,
                                                artist = track.artist,
                                                album = track.provider,
                                                duration = track.duration,
                                                albumArtUri = track.artUrl?.let { Uri.parse(it) },
                                                contentUri = Uri.parse(track.streamUrl),
                                                isOnline = true,
                                                provider = track.provider
                                            )
                                            viewModel.addSongsToLibrary(listOf(dummy))
                                        }
                                    },
                                    onGoToArtist = {
                                        viewModel.searchOnline(track.artist)
                                        selectedOnlineTrackForMenu = null
                                    },
                                    onLyricsClick = {
                                        val query = "${track.title} ${track.artist} lyrics"
                                        openExternalSearch(query, "Google")
                                        selectedOnlineTrackForMenu = null
                                    }
                                )
                            }
                        }
                    }

                    // WELCOME ONBOARDING OVERLAY
                    AnimatedVisibility(
                        visible = showWelcomeScreen,
                        enter = fadeIn(tween(600)),
                        exit = fadeOut(tween(600))
                    ) {
                        WelcomeScreen(
                            onComplete = { 
                                viewModel.completeOnboarding()
                                viewModel.setLibrarySection(LibrarySection.Songs)
                                scope.launch { pagerState.animateScrollToPage(0, animationSpec = PremiumSpring) }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun openExternalSearch(query: String, provider: String) {
        val intent = when (provider) {
            "YouTube" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
            "Spotify" -> Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$query")).apply { setPackage("com.spotify.music") }
            "Deezer" -> Intent(Intent.ACTION_VIEW, Uri.parse("deezer://www.deezer.com/search/$query"))
            else -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
        }
        try { startActivity(intent) } catch (e: Exception) {
            if (provider == "Spotify") {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$query"))) }
                catch (e2: Exception) { Toast.makeText(this, "Could not open search", Toast.LENGTH_SHORT).show() }
            } else if (provider == "Deezer") {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.deezer.com/search/$query"))) }
                catch (e2: Exception) { Toast.makeText(this, "Could not open search", Toast.LENGTH_SHORT).show() }
            } else Toast.makeText(this, "Could not open search", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun GlobalMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    dominantColor: Color,
    cachedBitmap: android.graphics.Bitmap? = null,
    onBitmapLoaded: (android.graphics.Bitmap) -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    val activeOrange = Color(0xFFFF9800)
    val frostedBg = dominantColor.copy(alpha = 0.50f).compositeOver(Color.Black.copy(alpha = 0.70f))
    
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(76.dp)
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = frostedBg,
        border = BorderStroke(1.5.dp, activeOrange.copy(alpha = 0.4f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cachedBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = cachedBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = song.albumArtUri,
                    onSuccess = { state ->
                        (state.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.let { onBitmapLoaded(it) }
                    },
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(text = havenTransform(song.title), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1)
                Text(text = havenTransform(song.artist, isArtistName = true), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onTogglePlayPause, modifier = Modifier.tactilePress(onClick = onTogglePlayPause)) {
                    Icon(imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = onNext, modifier = Modifier.tactilePress(onClick = onNext)) {
                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun BottomControlHub(
    selectedPage: Int,
    dominantColor: Color = Color(0xFF0F0D0C),
    adaptiveEnabled: Boolean = true,
    onPageSelected: (Int) -> Unit,
    onSearchClick: () -> Unit
) {
    val activeOrange = Color(0xFFFF9800)
    val iconWhite = Color.White.copy(alpha = 0.85f)
    val baseBg = if (adaptiveEnabled) dominantColor else Color(0xFF0F0D0C)
    val frostedBg = baseBg.copy(alpha = 0.50f).compositeOver(Color.Black.copy(alpha = 0.70f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
            color = frostedBg,
            border = BorderStroke(2.dp, activeOrange.copy(alpha = 0.5f)), // Refined orange border
            shadowElevation = 20.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                NavHubButton(icon = Icons.Default.LibraryMusic, isSelected = selectedPage == 0, onClick = { onPageSelected(0) })
                NavHubButton(icon = Icons.Default.MusicNote, isSelected = selectedPage == 1, onClick = { onPageSelected(1) })
                NavHubButton(icon = Icons.Default.Explore, isSelected = selectedPage == 2, onClick = { onPageSelected(2) })
                IconButton(onClick = onSearchClick, modifier = Modifier.tactilePress(onClick = onSearchClick)) {
                    Icon(Icons.Default.Search, null, tint = iconWhite, modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
fun NavHubButton(icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val activeOrange = Color(0xFFFF9800)
    val iconWhite = Color.White.copy(alpha = 0.85f)
    IconButton(onClick = onClick, modifier = Modifier.tactilePress(onClick = onClick)) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = if (isSelected) activeOrange else iconWhite,
            modifier = Modifier.size(28.dp)
        )
    }
}

class MainViewModelFactory(private val repository: MusicRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(repository) as T
    }
}
