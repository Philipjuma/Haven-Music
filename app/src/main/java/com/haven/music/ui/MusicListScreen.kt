package com.haven.music.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
    
    // NESTED SCROLL FOR HIDE/SHOW HUB (Universal for miniplayer)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -15) onHubVisibilityChange(false) // Swipe Up -> Hide
                if (available.y > 15) onHubVisibilityChange(true)  // Swipe Down -> Show
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
            // Editorial Header (Extreme Top)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp), 
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
                
                // GLOWING SEARCH BUTTON
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
                
                IconButton(
                    onClick = onSearchIconClick, 
                    modifier = Modifier
                        .tactilePress(onClick = onSearchIconClick)
                        .size(44.dp)
                        .background(Color(0xFF4CAF50).copy(alpha = glowAlpha), CircleShape)
                        .border(BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f)), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search", 
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { if (songs.isNotEmpty()) onSongClick(songs.first()) },
                    modifier = Modifier.weight(1f).height(36.dp).tactilePress(onClick = { if (songs.isNotEmpty()) onSongClick(songs.first()) }),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PLAY ALL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                
                OutlinedButton(
                    onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled().first()) },
                    modifier = Modifier.weight(1f).height(36.dp).tactilePress(onClick = { if (songs.isNotEmpty()) onSongClick(songs.first()) }),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SHUFFLE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            // Library Navigation (Reduced by 50%)
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                LibraryNavigation(
                    sections = orderedSections,
                    selectedSection = librarySection,
                    onSectionChange = onSectionChange,
                    onMoveSection = onMoveSection
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Premium Tab Content Transition (Crossfade)
            AnimatedContent(
                targetState = librarySection,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TabTransition",
                modifier = Modifier.weight(1f)
            ) { section ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (section) {
                        LibrarySection.Songs, LibrarySection.Favorites -> {
                            if (songs.isEmpty() && section == LibrarySection.Favorites) {
                                EmptyState(message = "No favorites yet")
                            } else {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(start = 2.dp, bottom = 180.dp)
                                    ) {
                                        itemsIndexed(
                                            items = songs, 
                                            key = { _, it -> it.id },
                                            contentType = { _, _ -> "song" }
                                        ) { index, song ->
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
                                    
                                    // ANIMATED A-Z SCROLLER RAIL
                                    if (section == LibrarySection.Songs) {
                                        AlphabetScrollerRail(
                                            songs = songs,
                                            onLetterSelected = { index ->
                                                scope.launch { listState.scrollToItem(index) }
                                            },
                                            onNoResult = onNoResult
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
                                    AlbumItem(
                                        album = album, 
                                        onClick = { onAlbumClick(album) },
                                        onLongClick = { onAlbumLongClick(album) }
                                    )
                                }
                            }
                        }
                        LibrarySection.Artists -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp)
                            ) {
                                items(artists, key = { it.name }) { artist ->
                                    ArtistItem(
                                        artist = artist, 
                                        onClick = { onArtistClick(artist) },
                                        onLongClick = { onArtistLongClick(artist) }
                                    )
                                }
                            }
                        }
                        LibrarySection.Playlists -> {
                            if (playlists.isEmpty()) {
                                EmptyState(message = "No playlists yet")
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 180.dp)
                                ) {
                                    itemsIndexed(playlists, key = { _, p -> p.id }) { _, playlist ->
                                        PlaylistItem(
                                            playlist = playlist,
                                            onClick = { onPlaylistClick(playlist) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Contextual Hints
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
                    titleColor = Color(0xFF90CAF9),
                    messageColor = Color.White,
                    onDismiss = { viewModel.dismissHint("library_long_press") }
                )
            }
            if (visibleHints["library_reorder"] == true) {
                HavenHintBox(
                    title = "Customise",
                    message = "Long-press and drag the category chips above to reorder your library.",
                    icon = Icons.Default.SettingsSuggest,
                    accentColor = Color(0xFF4CAF50),
                    titleColor = Color(0xFFA5D6A7),
                    messageColor = Color.White,
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
    val alphabet = remember { ('A'..'Z').toList() + '#' }
    var draggingLetter by remember { mutableStateOf<Char?>(null) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    
    val letterMap = remember(songs) {
        songs.mapIndexed { index, song -> song.title.firstOrNull()?.uppercaseChar() to index }
            .filter { it.first != null }
            .distinctBy { it.first }
            .toMap()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
            .width(28.dp) // Narrower rail
            .pointerInput(alphabet, letterMap) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val letterHeight = size.height / alphabet.size
                        if (letterHeight > 0) {
                            val index = (offset.y / letterHeight).toInt().coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[index]
                            draggingLetter = letter
                            activeLetter = letter
                            
                            val songIndex = letterMap[letter]
                            if (songIndex != null) {
                                onLetterSelected(songIndex)
                            } else if (letter != '#') {
                                onNoResult(letter)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val letterHeight = size.height / alphabet.size
                        if (letterHeight > 0) {
                            val index = (change.position.y / letterHeight).toInt().coerceIn(0, alphabet.size - 1)
                            val letter = alphabet[index]
                            if (letter != draggingLetter) {
                                draggingLetter = letter
                                activeLetter = letter
                                
                                val songIndex = letterMap[letter]
                                if (songIndex != null) {
                                    onLetterSelected(songIndex)
                                } else if (letter != '#') {
                                    onNoResult(letter)
                                }
                            }
                        }
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
                
                // Proximity Scale Logic (Enhanced)
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.85f else 1f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else if (hasSongs) 0.5f else 0.12f
                )

                Box(
                    modifier = Modifier
                        .height(letterHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        ),
                        color = if (isSelected) Color(0xFFFF9800) else Color.White,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    )
                }
            }
        }
    }
    
    // Aesthetic Letter Popup Overlay
    AnimatedVisibility(
        visible = activeLetter != null,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Box(
            contentAlignment = Alignment.Center, 
            modifier = Modifier.fillMaxSize().offset(y = (-40).dp) // Move it higher
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFFF9800),
                modifier = Modifier.size(120.dp), // Larger
                shadowElevation = 32.dp,
                border = BorderStroke(2.5.dp, Color.White.copy(alpha = 0.25f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = activeLetter?.toString() ?: "", 
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black), 
                        color = Color.Black
                    )
                }
            }
        }
        LaunchedEffect(activeLetter) {
            delay(1000)
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
            val scale by animateFloatAsState(
                targetValue = if (isDragged) 1.1f else 1f,
                animationSpec = tween(150)
            )
            
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.translationX = if (isDragged) dragOffset else 0f
                        this.alpha = if (isDragged) 0.8f else 1f
                    }
                    .pointerInput(sections) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { draggedIndex = index },
                            onDragEnd = {
                                draggedIndex = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedIndex = null
                                dragOffset = 0f
                            },
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
                    label = { 
                        Text(
                            text = havenTransform(section.name),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), 
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) 
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFFF9800)
                    ),
                    shape = RoundedCornerShape(10.dp), 
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedSection == section,
                        borderColor = Color.White.copy(alpha = 0.05f),
                        selectedBorderColor = Color(0xFFFF9800).copy(alpha = 0.4f),
                        borderWidth = 1.dp
                    )
                )
            }
        }
    }
}

@Composable
fun PlaylistItem(playlist: Playlist, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFF9800).copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = Color(0xFFFF9800))
                }
            }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = havenTransform(playlist.name),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = havenTransform("${playlist.songIds.size} songs"),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlbumItem(album: Album, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Card(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            AsyncImage(
                model = album.artUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = album.name,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = Modifier.padding(top = 10.dp, start = 4.dp)
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArtistItem(artist: Artist, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .tactilePress(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = artist.artUri,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Text(
                text = artist.name,
                color = Color.White,
                modifier = Modifier.padding(start = 16.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
