package com.haven.music.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.haven.music.MusicMix
import com.haven.music.Song

@Composable
fun MixDetailScreen(
    mix: MusicMix,
    currentSong: Song?,
    viewModel: com.haven.music.MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 180.dp)
        ) {
            // 1. Navigation Header & Collage / Hero
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.tactilePress(onClick = onBack)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text(
                            text = havenTransform("Music Mix"),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, 
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                    ) {
                        Box(modifier = Modifier.size(200.dp).clip(RoundedCornerShape(32.dp))) {
                            AsyncImage(
                                model = mix.imageUrls.getOrNull(0),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = havenTransform(mix.title),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = havenTransform(mix.description),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
                        )
                    }
                }
            }

            // 2. Action Hub
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { onPlayAll(mix.songs) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp).weight(1f).tactilePress(onClick = { onPlayAll(mix.songs) })
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PLAY", fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = { onShuffleAll(mix.songs) },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp).weight(1f).tactilePress(onClick = { onShuffleAll(mix.songs) }),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SHUFFLE")
                    }
                }
            }

            // 3. Songs List
            itemsIndexed(mix.songs, key = { _, s -> s.id }) { index, song ->
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    SongItem(
                        index = index + 1,
                        song = song,
                        isSelected = song.id == currentSong?.id,
                        isFavorite = false,
                        itemColor = viewModel.albumArtColors[song.id] ?: Color.Transparent,
                        cachedBitmap = viewModel.bitmapCache.get(song.id),
                        onBitmapLoaded = { viewModel.addToBitmapCache(song.id, it) },
                        onClick = { onSongClick(song) },
                        onToggleFavorite = { /* Not in mix room directly */ },
                        onLongClick = { onSongLongClick(song) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
