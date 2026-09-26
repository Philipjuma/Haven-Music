package com.haven.music.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haven.music.MixType
import com.haven.music.MusicMix
import com.haven.music.OnlineTrack
import com.haven.music.Song
import kotlinx.coroutines.delay

@Composable
fun DiscoverScreen(
    currentSong: Song?,
    mixes: List<MusicMix>,
    onlineSearchQuery: String,
    onlineSearchResults: List<OnlineTrack>,
    isOnlineSearching: Boolean,
    onlineSearchError: String?,
    viewModel: com.haven.music.MainViewModel,
    onOnlineSearch: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onOnlineTrackClick: (OnlineTrack) -> Unit,
    onOnlineTrackLongClick: (OnlineTrack) -> Unit,
    onMixClick: (MusicMix) -> Unit,
    onSongClick: (Song) -> Unit,
    onRefresh: () -> Unit,
    shouldRequestFocus: Boolean = false,
    onFocusHandled: () -> Unit = {}
) {
    val greeting = getPersonalityGreeting(currentSong?.artist, viewModel)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val insights by viewModel.deepInsights.collectAsState()
    val localSearchResults by viewModel.searchResults.collectAsState()

    // Trigger online search on entrance if query exists
    LaunchedEffect(onlineSearchQuery) {
        if (onlineSearchQuery.isNotBlank() && onlineSearchResults.isEmpty() && !isOnlineSearching) {
            onOnlineSearch(onlineSearchQuery)
        }
    }

    // Focus Logic for Search Trigger
    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            focusRequester.requestFocus()
            delay(100) // Ensure layout is ready
            keyboardController?.show()
            onFocusHandled()
        }
    }

    // Sync insights with current song
    LaunchedEffect(currentSong) {
        currentSong?.let { viewModel.fetchDeepInsights(it) }
    }

    val prompts = remember {
        listOf(
            "Your next favorite might be out there.",
            "Can't find it here? Search online.",
            "Looking for something new?",
            "Find something you haven't heard yet.",
            "Your library is only the beginning.",
            "Search beyond your collection.",
            "Something different today?",
            "Let Haven look around."
        )
    }
    var currentPromptIndex by remember { mutableIntStateOf(0) }
    val youtubeVideoId by viewModel.currentYouTubeVideoId.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            currentPromptIndex = (currentPromptIndex + 1) % prompts.size
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. EDITORIAL HEADER & ONLINE SEARCH
            item {
                Column(modifier = Modifier.padding(top = 24.dp, start = 8.dp)) {
                    Text(
                        text = havenTransform("DISCOVER"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 3.sp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.8f) // Green for online discovery
                    )
                    
                    Text(
                        text = havenTransform(greeting),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Online Search Field
                    TextField(
                        value = onlineSearchQuery,
                        onValueChange = onOnlineSearch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .focusRequester(focusRequester),
                        placeholder = { 
                            Text(
                                text = prompts[currentPromptIndex],
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                modifier = Modifier.basicMarquee()
                            ) 
                        },
                        supportingText = { Text("Can't find it in your library? Search online.") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF4CAF50)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isOnlineSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF4CAF50))
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                IconButton(onClick = onVoiceClick) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice Search", tint = Color(0xFF4CAF50))
                                }
                            }
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White.copy(alpha = 0.08f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF4CAF50),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            }

            // 1.5 ARTIST INSIGHTS (Back to Discover Room)
            if (insights != null && onlineSearchQuery.isBlank()) {
                item {
                    DeepInsightSlideshow(insights = insights!!)
                }
            }

            // EXPERIMENTAL YOUTUBE PLAYER
            if (youtubeVideoId != null) {
                item {
                    YouTubeWebPlayer(
                        videoId = youtubeVideoId!!,
                        onClose = { viewModel.closeYouTubePlayer() }
                    )
                }
            }

            // 2. SEARCH RESULTS (Mixed Layer)
            if (onlineSearchQuery.isNotBlank()) {
                // A. Local Results First
                if (localSearchResults.isNotEmpty()) {
                    item {
                        Text(
                            text = "LOCAL LIBRARY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                            color = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                        )
                    }
                    itemsIndexed(localSearchResults) { index, song ->
                        SongItem(
                            index = index + 1,
                            song = song,
                            isSelected = song.id == currentSong?.id,
                            isFavorite = false,
                            itemColor = viewModel.albumArtColors[song.id] ?: Color.Transparent,
                            cachedBitmap = viewModel.bitmapCache.get(song.id),
                            onBitmapLoaded = { viewModel.addToBitmapCache(song.id, it) },
                            onClick = { onSongClick(song) },
                            onToggleFavorite = { /* Not in discover */ }
                        )
                    }
                }

                // B. Online Results
                item {
                    Text(
                        text = "ONLINE DISCOVERY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
                        color = Color(0xFF4CAF50).copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp, top = 16.dp)
                    )
                }

                if (onlineSearchError != null) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = onlineSearchError, color = Color.White.copy(alpha = 0.6f))
                            TextButton(onClick = { onOnlineSearch(onlineSearchQuery) }) {
                                Text("Retry", color = Color(0xFF4CAF50))
                            }
                        }
                    }
                } else if (isOnlineSearching && onlineSearchResults.isEmpty()) {
                    items(5) {
                        ShimmeringTrackItem()
                    }
                } else if (onlineSearchResults.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("No tracks found online.", color = Color.White.copy(alpha = 0.4f))
                        }
                    }
                } else {
                    items(onlineSearchResults, key = { it.id }) { track ->
                        OnlineTrackItem(
                            track = track,
                            onClick = { onOnlineTrackClick(track) },
                            onLongClick = { onOnlineTrackLongClick(track) }
                        )
                    }
                }
            } else {
                // 3. REGULAR DISCOVER SECTIONS (Visible when not searching)
                if (mixes.isEmpty()) {
                    item { EmptyMixState() }
                } else {
                    // HERO: Temporal Mix
                    mixes.find { it.type == MixType.HERO }?.let { heroMix ->
                        item(key = heroMix.id) {
                            HeroMixCard(mix = heroMix, onClick = { onMixClick(heroMix) })
                        }
                    }

                    // SUPPORTING BOARD: Two Medium Cards
                    val cardMixes = mixes.filter { it.type == MixType.CARD }
                    if (cardMixes.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                cardMixes.take(2).forEachIndexed { index, mix ->
                                    Box(modifier = Modifier.weight(if (index == 0) 1.2f else 1f)) {
                                        MediumMixCard(mix = mix, onClick = { onMixClick(mix) })
                                    }
                                }
                            }
                        }
                    }

                    // OBSESSION: Horizontal Carousel
                    mixes.find { it.id == "obsession_mix" }?.let { obsession ->
                        item(key = obsession.id) {
                            Column {
                                SectionHeader(title = obsession.title)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    items(obsession.songs.take(6)) { song ->
                                        Column(
                                            modifier = Modifier
                                                .width(140.dp)
                                                .tactilePress(onClick = { onSongClick(song) }),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            AsyncImage(
                                                model = song.albumArtUri,
                                                contentDescription = null,
                                                modifier = Modifier.size(140.dp).clip(RoundedCornerShape(24.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                                maxLines = 1,
                                                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECONDARY BOARD: Small Editorial Cards
                    if (cardMixes.size > 2) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                cardMixes.drop(2).forEach { mix ->
                                    MixCardSmall(mix = mix, onClick = { onMixClick(mix) })
                                }
                            }
                        }
                    }

                    // EXPLORATORY CAROUSELS
                    val carousels = mixes.filter { it.type == MixType.CAROUSEL }
                    if (carousels.isNotEmpty()) {
                        item {
                            Column {
                                SectionHeader(title = "Explore Moods")
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    items(carousels, key = { it.id }) { mix ->
                                        MoodMixCard(mix = mix, onClick = { onMixClick(mix) })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onRefresh: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = havenTransform(title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        if (onRefresh != null) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFFFF9800).copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
fun HeroMixCard(mix: MusicMix, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = mix.imageUrls.getOrNull(0),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.3f),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(text = havenTransform(mix.title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(text = havenTransform(mix.description), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = havenTransform("${mix.songs.size} songs"), color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.1f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun MediumMixCard(mix: MusicMix, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = mix.imageUrls.getOrNull(0), contentDescription = null, modifier = Modifier.fillMaxSize().alpha(0.2f), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
                Text(text = mix.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(text = "${mix.songs.size} tracks", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9800))
            }
        }
    }
}

@Composable
fun MixCardSmall(mix: MusicMix, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(24.dp))) {
                AsyncImage(model = mix.imageUrls.getOrNull(0), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = havenTransform(mix.title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(text = havenTransform(mix.description), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
            }
            IconButton(onClick = onClick, modifier = Modifier.padding(end = 8.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun MoodMixCard(mix: MusicMix, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp)
            .tactilePress(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.1f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(16.dp)) {
            Text(text = havenTransform(mix.title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun EmptyMixState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = Color.White.copy(alpha = 0.05f), border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.2f))) {
            Box(contentAlignment = Alignment.Center) { Text("✨", fontSize = 40.sp) }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = havenTransform("Give Haven some music."), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text(text = havenTransform("Add a few songs and I'll start making mixes for you."), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
    }
}
