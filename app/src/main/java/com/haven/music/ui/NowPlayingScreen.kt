package com.haven.music.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.haven.music.Song
import androidx.media3.common.Player
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    song: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    dominantColor: Color,
    cachedBitmap: Bitmap? = null,
    onBitmapLoaded: (Bitmap) -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddFolderClick: () -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String) -> Unit,
    onRemoveFromQueue: (Song) -> Unit,
    musicFolders: Set<String>,
    onBack: () -> Unit,
    viewModel: com.haven.music.MainViewModel,
    nextSong: Song? = null,
    onUpNextClick: (Song) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    var showSongMenu by remember { mutableStateOf(false) }
    var showLyricsDialog by remember { mutableStateOf(false) }
    val visibleHints = viewModel.visibleHints
    val currentLyrics by viewModel.currentLyrics.collectAsState()
    
    // Direction tracking for fluid transitions
    var slideDirection by remember { mutableIntStateOf(1) } // 1 for Next, -1 for Prev

    // Dynamic Glass Background (Matching Library)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val glassTint = dominantColor.copy(alpha = 0.08f).compositeOver(surfaceColor.copy(alpha = 0.2f))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = glassTint
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // DIRECT PLAYER CONTENT (Removed Pager & Queue)
            PlayerContent(
                song = song,
                isPlaying = isPlaying,
                position = position,
                duration = duration,
                shuffleModeEnabled = shuffleModeEnabled,
                repeatMode = repeatMode,
                isFavorite = isFavorite,
                cachedBitmap = cachedBitmap,
                onBitmapLoaded = onBitmapLoaded,
                onTogglePlayPause = onTogglePlayPause,
                onNext = { slideDirection = 1; onNext() },
                onPrevious = { slideDirection = -1; onPrevious() },
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onToggleRepeat = onToggleRepeat,
                onToggleFavorite = onToggleFavorite,
                onLongClick = {
                    if (song != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showSongMenu = true
                    }
                },
                onSettingsClick = onSettingsClick,
                musicFolders = musicFolders,
                nextSong = nextSong,
                onUpNextClick = onUpNextClick,
                onBack = onBack,
                onAddFolderClick = onAddFolderClick,
                slideDirection = slideDirection,
                onDirectionChange = { slideDirection = it },
                onShowLyrics = { 
                    if (song != null) viewModel.fetchLyrics(song)
                    showLyricsDialog = true 
                },
                viewModel = viewModel
            )

            // Contextual Hints
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (visibleHints["player_3d"] == true) {
                    HavenHintBox(
                        title = "Did you know?",
                        message = "The buttons are tactile 3D. Try a deep press for feedback.",
                        icon = Icons.Default.TouchApp,
                        accentColor = Color(0xFFFF9800),
                        titleColor = Color(0xFFFFCC80),
                        messageColor = Color.White,
                        onDismiss = { viewModel.dismissHint("player_3d") }
                    )
                }
            }
        }
    }

    if (showLyricsDialog && song != null) {
        LyricsDialog(
            song = song,
            lyrics = currentLyrics,
            onDismiss = { showLyricsDialog = false },
            onSearchOnline = {
                val query = "${song.title} ${song.artist} lyrics"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                context.startActivity(intent)
            }
        )
    }

    if (showSongMenu && song != null) {
        val context = LocalContext.current
        SongActionMenu(
            song = song,
            isFavorite = isFavorite,
            onDismiss = { showSongMenu = false },
            onPlay = { onTogglePlayPause() },
            onAddToPlaylist = { /* Trigger playlist selection state if needed */ },
            onToggleFavorite = { onToggleFavorite() },
            onGoToArtist = { onGoToArtist(song.artist) },
            onGoToAlbum = { onGoToAlbum(song.album) },
            onAddToQueue = { /* Already playing */ },
            onPlayNext = { /* Already playing */ },
            onRemoveFromPlaylist = null,
            onRemoveFromQueue = { onRemoveFromQueue(song) },
            onLyricsClick = {
                val query = "${song.title} ${song.artist} lyrics"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                context.startActivity(intent)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContent(
    song: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    cachedBitmap: Bitmap? = null,
    onBitmapLoaded: (Bitmap) -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onLongClick: () -> Unit,
    onSettingsClick: () -> Unit,
    musicFolders: Set<String>,
    nextSong: Song? = null,
    onUpNextClick: (Song) -> Unit = {},
    onBack: () -> Unit,
    onAddFolderClick: () -> Unit,
    slideDirection: Int,
    onDirectionChange: (Int) -> Unit,
    onShowLyrics: () -> Unit,
    viewModel: com.haven.music.MainViewModel
) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val screenHeight = configuration.screenHeightDp.dp
    
    val isSmallScreen = screenHeight < 700.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. TOP BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart).tactilePress(onClick = onBack)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White)
            }
            
            HavenLogo(modifier = Modifier.scale(0.8f))
            
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd).tactilePress(onClick = onSettingsClick)
            ) {
                Icon(Icons.Default.MoreVert, null, tint = Color.White)
            }
        }

        // FLEXIBLE TOP SPACER (Art moved 2% lower)
        Spacer(modifier = Modifier.weight(0.07f))

        // 2. CONSOLE HUB
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ALBUM ART (Full Width Edge-to-Edge with Reactive Gestures)
            var totalDragX by remember { mutableStateOf(0f) }
            var totalDragY by remember { mutableStateOf(0f) }
            val gestureThreshold = 243f // Optimized threshold (reduced by 10%)
            
            // Reactive scale for tactile feedback
            val dragScale by animateFloatAsState(
                targetValue = when {
                    abs(totalDragX) > 50f -> 0.85f + (abs(totalDragX) / 1000f).coerceAtMost(0.1f)
                    abs(totalDragY) > 50f -> 0.95f
                    !isPlaying -> 0.92f // Shrink when paused
                    else -> 1f
                },
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = dragScale
                        scaleY = dragScale
                        translationX = totalDragX * 0.15f // Increased follow distance
                    }
                    .pointerInput(song?.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y
                            },
                            onDragEnd = {
                                if (abs(totalDragX) > abs(totalDragY)) {
                                    // Horizontal Skip - ONLY on Lift
                                    if (totalDragX > gestureThreshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDirectionChange(-1)
                                        onPrevious()
                                    }
                                    else if (totalDragX < -gestureThreshold) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onDirectionChange(1)
                                        onNext()
                                    }
                                } else {
                                    // Vertical Actions
                                    if (totalDragY > gestureThreshold) onBack()
                                    else if (totalDragY < -gestureThreshold && song != null) {
                                        viewModel.fetchLyrics(song)
                                        onShowLyrics()
                                    }
                                }
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDragCancel = {
                                totalDragX = 0f
                                totalDragY = 0f
                            }
                        )
                    }
                    .combinedClickable(
                        onClick = {},
                        onLongClick = onLongClick,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = song,
                    transitionSpec = {
                        val slideOffset = 700
                        if (slideDirection > 0) {
                            // Enter from Right, Exit to Left
                            (slideInHorizontally(animationSpec = tween(700, easing = EaseOutQuart)) { it } + 
                             scaleIn(animationSpec = tween(700, easing = EaseOutBack), initialScale = 0.85f) + 
                             fadeIn(animationSpec = tween(500)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(700, easing = EaseInOutQuart)) { -it / 2 } + 
                                    scaleOut(animationSpec = tween(700), targetScale = 0.9f) + 
                                    fadeOut(animationSpec = tween(500))
                                )
                        } else {
                            // Enter from Left, Exit to Right
                            (slideInHorizontally(animationSpec = tween(700, easing = EaseOutQuart)) { -it } + 
                             scaleIn(animationSpec = tween(700, easing = EaseOutBack), initialScale = 0.85f) + 
                             fadeIn(animationSpec = tween(500)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(700, easing = EaseInOutQuart)) { it / 2 } + 
                                    scaleOut(animationSpec = tween(700), targetScale = 0.9f) + 
                                    fadeOut(animationSpec = tween(500))
                                )
                        }.using(SizeTransform(clip = false))
                    },
                    label = "AlbumArtTransition"
                ) { currentSong ->
                    if (currentSong != null) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(0.dp), // Truly touch edges
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            border = BorderStroke(1.dp, if (!isPlaying) Color(0xFFFF9800).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f))
                        ) {
                            if (cachedBitmap != null && currentSong.id == song?.id) {
                                androidx.compose.foundation.Image(
                                    bitmap = cachedBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = remember(currentSong.albumArtUri) {
                                        ImageRequest.Builder(context)
                                            .data(currentSong.albumArtUri)
                                            .size(800) // High-fidelity player art
                                            .allowHardware(true)
                                            .crossfade(true)
                                            .build()
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    onSuccess = { state ->
                                        if (currentSong.id == song?.id) {
                                            (state.result.drawable as? BitmapDrawable)?.bitmap?.let { onBitmapLoaded(it) }
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                    HavenEmptyEmblem(orangeAccent = Color(0xFFFF9800))
                }
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 20.dp else 32.dp))

            // SONG INFO & PROGRESS (With Start Alignment)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 16.dp else 24.dp)
            ) {
                if (song != null) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                fontSize = 32.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee()
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFFFF9800).copy(alpha = 0.9f),
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp).basicMarquee()
                        )
                    }
                }

                // NEXT SONG INTEGRATION
                if (nextSong != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp) // Match progress bar length
                            .height(38.dp)
                            .tactilePress(onClick = { onUpNextClick(nextSong) }),
                        shape = CircleShape, // Pill shaped
                        color = Color.White.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BlinkingGreenDot()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "NEXT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 9.sp),
                                color = Color(0xFFFF9800).copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = nextSong.title,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                modifier = Modifier.weight(1f).basicMarquee()
                            )
                        }
                    }
                }

                // MECHANICAL PROGRESS
                MechanicalProgressSystem(
                    position = position,
                    duration = duration,
                    onSeek = onSeek
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 2.dp else 4.dp)) // Very tight gap to raise buttons

            // CONTROL CLUSTER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp), // Maximize reach
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Tactile3DButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Shuffle, 
                        null, 
                        tint = if (shuffleModeEnabled) Color(0xFFFF9800) else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Tactile3DButton(
                        onClick = onPrevious,
                        modifier = Modifier.size(62.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }

                    Tactile3DButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(92.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                            null, 
                            tint = Color.White, 
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Tactile3DButton(
                        onClick = onNext,
                        modifier = Modifier.size(62.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }

                Tactile3DButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, 
                        null, 
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) Color(0xFFFF9800) else Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // FLEXIBLE BOTTOM SPACER (Raised slightly above navbar)
        Spacer(modifier = Modifier.weight(0.4f))
        
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MechanicalProgressSystem(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val mutedGray = Color.White.copy(alpha = 0.4f)
    val orange = Color(0xFFFF9800)
    val trackBg = Color.White.copy(alpha = 0.1f)

    val sliderInteractionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp) // Reduced from 32.dp to lengthen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(position),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                color = mutedGray
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                color = mutedGray
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth().height(24.dp).offset(y = (-6).dp), // Moved a bit higher
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = if (duration > 0) position.toFloat() / duration.toFloat() else 0f,
                onValueChange = { onSeek((it * duration).toLong()) },
                interactionSource = sliderInteractionSource,
                modifier = Modifier.fillMaxWidth(),
                track = { sliderState ->
                    val fraction = sliderState.value
                    val orangeLiquid = Color(0xFFFF9800)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp) // Thinner track
                            .border(1.dp, orangeLiquid.copy(alpha = 0.2f), RoundedCornerShape(7.dp)), // Glassy syringe-like outline
                        contentAlignment = Alignment.CenterStart
                    ) { 
                        // Background of the "syringe"
                        Box(modifier = Modifier.fillMaxSize().background(trackBg.copy(alpha = 0.05f), RoundedCornerShape(7.dp)))
                        
                        // The "Liquid" (Progress)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(6.dp) // Liquid is thinner than the container
                                .padding(horizontal = 4.dp) // Gap from edges
                                .background(orangeLiquid, RoundedCornerShape(3.dp))
                        )
                    }
                },
                thumb = {
                    // Circular Glassy Thumb (30% bigger: 22dp * 1.3 ≈ 28dp)
                    val orangeIndicator = Color(0xFFFF9800)
                    Box(
                        modifier = Modifier
                            .size(28.dp) 
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                            .border(1.5.dp, orangeIndicator.copy(alpha = 0.8f), CircleShape)
                            .shadow(4.dp, CircleShape, spotColor = Color.Black)
                    )
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun LyricsDialog(
    song: Song,
    lyrics: String?,
    onDismiss: () -> Unit,
    onSearchOnline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "LYRICS",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = Color(0xFFFF9800)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFF9800).copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (lyrics != null) {
                    Text(
                        text = lyrics,
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Lyrics for this track are currently being synced or are available online.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSearchOnline,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
            ) {
                Text("SEARCH ON GOOGLE", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = Color.White.copy(alpha = 0.5f))
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color(0xFF1A1A1A)
    )
}

@Composable
fun HavenEmptyEmblem(orangeAccent: Color) {
    Surface(
        modifier = Modifier.size(200.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.radialGradient(listOf(orangeAccent.copy(alpha = 0.05f), Color.Transparent)))
            )
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(80.dp)
            )
            Text(
                "C",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = orangeAccent.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )
        }
    }
}

