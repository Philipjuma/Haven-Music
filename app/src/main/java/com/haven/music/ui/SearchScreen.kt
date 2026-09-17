package com.haven.music.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haven.music.Song

@Composable
fun SearchScreen(
    query: String,
    searchResults: List<Song>,
    currentSong: Song?,
    viewModel: com.haven.music.MainViewModel,
    onQueryChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onExternalSearch: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val placeholder = getRotatingSearchPlaceholder()

    // Auto-focus and show keyboard on entrance
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212) // Solid dark background to match other overlays
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp, start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    keyboardController?.hide()
                    onBack() 
                }, modifier = Modifier.tactilePress(onClick = onBack)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = havenTransform("Search"),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.White
                )
            }

            // Global Search Bar
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .focusRequester(focusRequester),
                placeholder = { Text(placeholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = onVoiceClick, modifier = Modifier.tactilePress(onClick = onVoiceClick)) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Search")
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    disabledContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFF9800)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (query.isNotBlank() && searchResults.isEmpty()) {
                    // High-visibility External Search options (Top Half)
                    item {
                        ExternalSearchState(
                            query = query,
                            onExternalSearch = onExternalSearch
                        )
                    }
                } else if (query.isNotBlank()) {
                    // Show local results
                    itemsIndexed(searchResults, key = { _, s -> s.id }) { index, song ->
                        SongItem(
                            index = index + 1,
                            song = song,
                            isSelected = song.id == currentSong?.id,
                            isFavorite = false,
                            itemColor = viewModel.albumArtColors[song.id] ?: Color.Transparent,
                            cachedBitmap = viewModel.bitmapCache.get(song.id),
                            onBitmapLoaded = { viewModel.addToBitmapCache(song.id, it) },
                            onClick = { onSongClick(song) },
                            onToggleFavorite = { /* Not in search room */ }
                        )
                    }
                } else {
                    // Greeting state - Pushed to the upper middle
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxHeight(0.6f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = havenTransform("Can't find it in your library?"),
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = havenTransform("Search elsewhere."),
                                color = Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
