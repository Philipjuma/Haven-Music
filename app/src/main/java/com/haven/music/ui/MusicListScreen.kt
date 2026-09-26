package com.haven.music.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haven.music.Album
import com.haven.music.Artist
import com.haven.music.LibrarySection
import com.haven.music.MusicMix
import com.haven.music.Playlist
import com.haven.music.Song
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MusicListScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    currentSong: Song?,
    librarySection: LibrarySection,
    favorites: Set<Long>,
    dominantColor: Color,
    mixes: List<MusicMix>,
    orderedSections: List<LibrarySection>,
    viewModel: com.haven.music.MainViewModel,
    onSectionChange: (LibrarySection) -> Unit,
    onMoveSection: (Int, Int) -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onMixClick: (MusicMix) -> Unit,
    onRefreshMixes: () -> Unit,
    onSearchIconClick: () -> Unit,
    onReturnToPlayer: () -> Unit,
    isHubVisible: Boolean,
    onHubVisibilityChange: (Boolean) -> Unit,
    onAlbumClick: (Album) -> Unit = {},
    onAlbumLongClick: (Album) -> Unit = {},
    onArtistClick: (Artist) -> Unit = {},
    onArtistLongClick: (Artist) -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onNoResult: (Char) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val scope = rememberCoroutineScope()
    val visibleHints = viewModel.visibleHints
    
    // Nested scroll for auto-hiding the hub
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15) onHubVisibilityChange(false)
                if (available.y > 15) onHubVisibilityChange(true)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp), 
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp
                    ),
                    color = Color.White
                )
                
                val infiniteTransition = rememberInfiniteTransition(label = "searchGlow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.05f,
                    targetValue = 0.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glowAlpha"
                )
                
                // Small Rounded Square Search Button (48dp)
                Box(
                    modifier = Modifier
                        .size(48.dp) 
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF4CAF50).copy(alpha = glowAlpha), RoundedCornerShape(12.dp)) 
                        .border(BorderStroke(1.5.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)), RoundedCornerShape(12.dp))
                        .clickable(onClick = onSearchIconClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search", 
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { if (songs.isNotEmpty()) onSongClick(songs.first()) },
                    modifier = Modifier.weight(1f).height(40.dp).tactilePress(onClick = { if (songs.isNotEmpty()) onSongClick(songs.first()) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY ALL", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled().first()) },
                    modifier = Modifier.weight(1f).height(40.dp).tactilePress(onClick = { if (songs.isNotEmpty()) onSongClick(songs.first()) }),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SHUFFLE", fontWeight = FontWeight.Bold)
                }
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LibraryNavigation(
                    sections = orderedSections,
                    selectedSection = librarySection,
                    onSectionChange = onSectionChange,
                    onMoveSection = onMoveSection
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = librarySection,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label = "TabTransition",
                modifier = Modifier.weight(1f)
            ) { section ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (section) {
                        LibrarySection.Songs, LibrarySection.Favorites -> {
                            if (songs.isEmpty() && section == LibrarySection.Favorites) {
                                EmptyState(message = "No favorites yet")
                            } else {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(start = 4.dp, end = 32.dp, bottom = 180.dp)
                                ) {
                                    itemsIndexed(items = songs, key = { _, it -> it.id }) { index, song ->
                                        SongItem(
                                            index = index + 1,
                                            song = song,
                                            isSelected = song.id == currentSong?.id,
                                            isFavorite = song.id in favorites,
                                            itemColor = viewModel.albumArtColors[song.id] ?: Color.Transparent,
                                            cachedBitmap = viewModel.bitmapCache.get(song.id),
                                            onBitmapLoaded = { viewModel.addToBitmapCache(song.id, it) },
                                            onClick = { onSongClick(song) },
                                            onToggleFavorite = { onToggleFavorite(song.id) },
                                            onLongClick = { onSongLongClick(song) }
                                        )
                                    }
                                }
                            }
                        }
                        LibrarySection.Albums -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(150.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp)
                            ) {
                                items(albums, key = { it.name + it.artist }) { album ->
                                    AlbumItem(album = album, onClick = { onAlbumClick(album) }, onLongClick = { onAlbumLongClick(album) })
                                }
                            }
                        }
                        LibrarySection.Artists -> {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp)) {
                                items(artists, key = { it.name }) { artist ->
                                    ArtistItem(artist = artist, onClick = { onArtistClick(artist) }, onLongClick = { onArtistLongClick(artist) })
                                }
                            }
                        }
                        LibrarySection.Playlists -> {
                            if (playlists.isEmpty()) { EmptyState(message = "No playlists yet") }
                            else {
                                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp)) {
                                    itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
                                        PlaylistItem(playlist = playlist, onClick = { onPlaylistClick(playlist) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // TOP-LEVEL ALPHABET RAIL (Full-Height Right Edge Anchor)
        if (librarySection == LibrarySection.Songs && songs.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 180.dp, bottom = 100.dp, end = 4.dp)
                    .width(32.dp) // Touch hit-zone
                    .zIndex(10f) 
            ) {
                AlphabetScrollerRail(
                    songs = songs,
                    onLetterSelected = { index ->
                        scope.launch { listState.scrollToItem(index) }
                    },
                    onNoResult = onNoResult
                )
            }
        }

        // CONTEXTUAL HINTS
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (visibleHints["library_long_press"] == true) {
                HavenHintBox(
                    title = "Pro Tip",
                    message = "Long-press any song to add it to a playlist or your queue.",
                    icon = Icons.Default.Lightbulb,
                    accentColor = Color(0xFF2196F3),
                    onDismiss = { viewModel.dismissHint("library_long_press") }
                )
            }
            if (visibleHints["library_reorder"] == true) {
                HavenHintBox(
                    title = "Customise",
                    message = "Long-press and drag the category chips above to reorder your library.",
                    icon = Icons.Default.SettingsSuggest,
                    accentColor = Color(0xFF4CAF50),
                    onDismiss = { viewModel.dismissHint("library_reorder") }
                )
            }
        }
    }
}

@Composable
fun AlphabetScrollerRail(
    songs: List<Song>,
    onLetterSelected: (Int) -> Unit,
    onNoResult: (Char) -> Unit = {}
) {
    val alphabet = remember { listOf('#') + ('A'..'Z').toList() }
    var draggingLetter by remember { mutableStateOf<Char?>(null) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    
    val letterMap = remember(songs) {
        val map = mutableMapOf<Char, Int>()
        songs.forEachIndexed { index, song ->
            val firstChar = song.title.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar.isLetter()) firstChar else '#'
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }

    val handleTouchAt: (Float, Float) -> Unit = { y, totalHeight ->
        if (totalHeight > 0f) {
            val letterHeight = totalHeight / alphabet.size.toFloat()
            val index = (y / letterHeight).toInt().coerceIn(0, alphabet.size - 1)
            val letter = alphabet[index]
            if (letter != draggingLetter) {
                draggingLetter = letter
                activeLetter = letter
                val songIndex = letterMap[letter]
                if (songIndex != null) onLetterSelected(songIndex) else if (letter != '#') onNoResult(letter)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(alphabet, letterMap) {
                detectTapGestures(
                    onPress = { offset ->
                        handleTouchAt(offset.y, size.height.toFloat())
                        try {
                            tryAwaitRelease()
                        } finally {
                            draggingLetter = null
                        }
                    }
                )
            }
            .pointerInput(alphabet, letterMap) {
                detectDragGestures(
                    onDragStart = { offset ->
                        handleTouchAt(offset.y, size.height.toFloat())
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        handleTouchAt(change.position.y, size.height.toFloat())
                    },
                    onDragEnd = { draggingLetter = null },
                    onDragCancel = { draggingLetter = null }
                )
            }
    ) {
        val letterHeight = maxHeight / alphabet.size
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEach { letter ->
                val hasSongs = letterMap.containsKey(letter)
                val isSelected = draggingLetter == letter
                val scale by animateFloatAsState(targetValue = if (isSelected) 1.75f else 1f, label = "railScale")
                val alpha by animateFloatAsState(targetValue = if (isSelected) 1f else if (hasSongs) 0.85f else 0.25f, label = "railAlpha")
                Box(modifier = Modifier.height(letterHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Black),
                        color = if (isSelected) Color(0xFFFF9800) else Color(0xFF4CAF50),
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    )
                }
            }
        }
    }
    
    AnimatedVisibility(visible = activeLetter != null, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().offset(x = (-60).dp)) {
            Surface(shape = CircleShape, color = Color(0xFF4CAF50), modifier = Modifier.size(72.dp), shadowElevation = 16.dp, border = BorderStroke(2.dp, Color.White.copy(alpha = 0.25f))) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = activeLetter?.toString() ?: "", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black), color = Color.Black)
                }
            }
        }
        LaunchedEffect(activeLetter) {
            delay(800)
            if (draggingLetter == null) activeLetter = null
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryNavigation(
    sections: List<LibrarySection>,
    selectedSection: LibrarySection,
    onSectionChange: (LibrarySection) -> Unit,
    onMoveSection: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = Modifier.height(40.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(sections, key = { _, s -> s.name }) { index, section ->
            val isDragged = draggedIndex == index
            val scale by animateFloatAsState(targetValue = if (isDragged) 1.1f else 1f, animationSpec = tween(150))
            Box(
                modifier = Modifier
                    .graphicsLayer { this.scaleX = scale; this.scaleY = scale; this.translationX = if (isDragged) dragOffset else 0f; this.alpha = if (isDragged) 0.8f else 1f }
                    .pointerInput(sections) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggedIndex = index },
                            onDragEnd = { draggedIndex = null; dragOffset = 0f },
                            onDragCancel = { draggedIndex = null; dragOffset = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.x
                                val itemWidth = 80f 
                                if (dragOffset > itemWidth && index < sections.size - 1) {
                                    onMoveSection(index, index + 1)
                                    dragOffset -= itemWidth
                                    draggedIndex = index + 1
                                } else if (dragOffset < -itemWidth && index > 0) {
                                    onMoveSection(index, index - 1)
                                    dragOffset += itemWidth
                                    draggedIndex = index - 1
                                }
                            }
                        )
                    }
            ) {
                FilterChip(
                    selected = selectedSection == section,
                    onClick = { onSectionChange(section) },
                    modifier = Modifier.tactilePress(onClick = { onSectionChange(section) }),
                    label = { Text(text = havenTransform(section.name), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) },
                    colors = FilterChipDefaults.filterChipColors(containerColor = Color.White.copy(alpha = 0.05f), labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f), selectedLabelColor = Color(0xFFFF9800)),
                    shape = RoundedCornerShape(10.dp), 
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedSection == section, borderColor = Color.White.copy(alpha = 0.05f), selectedBorderColor = Color(0xFFFF9800).copy(alpha = 0.4f), borderWidth = 1.dp)
                )
            }
        }
    }
}

@Composable
fun PlaylistItem(playlist: Playlist, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().tactilePress(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp), color = Color(0xFFFF9800).copy(alpha = 0.1f)) {
                Box(contentAlignment = Alignment.Center) { Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = Color(0xFFFF9800)) }
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(text = havenTransform(playlist.name), color = Color.White, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                Text(text = havenTransform("${playlist.songIds.size} songs"), color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumItem(album: Album, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().tactilePress(onClick = onClick, onLongClick = onLongClick)) {
        Card(modifier = Modifier.aspectRatio(1f).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.Black), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))) {
            AsyncImage(model = album.artUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Text(text = album.name, color = Color.White, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
        Text(text = album.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.5f), maxLines = 1, modifier = Modifier.padding(start = 4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistItem(artist: Artist, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Surface(modifier = Modifier.fillMaxWidth().tactilePress(onClick = onClick, onLongClick = onLongClick), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = artist.artUri, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Text(text = artist.name, color = Color.White, modifier = Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}
