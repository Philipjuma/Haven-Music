package com.haven.music.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haven.music.Song

@Composable
fun AlbumScreen(
    albumName: String,
    artistName: String,
    albumArtUri: android.net.Uri?,
    albumSongs: List<Song>,
    viewModel: com.haven.music.MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onAddToPlaylist: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF121212)
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = havenTransform("Album"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // 1. Hero / Album Art
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier.size(220.dp),
                            shape = RoundedCornerShape(32.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            AsyncImage(
                                model = albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = havenTransform(albumName),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                            color = Color.White
                        )
                        
                        Text(
                            text = havenTransform(artistName, isArtistName = true),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // 2. Action Hub
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { onPlayAll(albumSongs) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp).weight(1f).tactilePress(onClick = { onPlayAll(albumSongs) })
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLAY", fontWeight = FontWeight.Bold)
                            }
                            
                            OutlinedButton(
                                onClick = { onShuffleAll(albumSongs) },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(48.dp).weight(1f).tactilePress(onClick = { onShuffleAll(albumSongs) }),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SHUFFLE")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onToggleFavorite,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(44.dp).weight(1f).tactilePress(onClick = onToggleFavorite),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("FAVORITE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onAddToPlaylist,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(44.dp).weight(1f).tactilePress(onClick = onAddToPlaylist),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.7f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("PLAYLIST", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 3. Songs List
                items(albumSongs, key = { it.id }) { song ->
                    DetailSongItem(
                        song = song, 
                        itemColor = viewModel.albumArtColors[song.id], 
                        cachedBitmap = viewModel.bitmapCache.get(song.id),
                        onBitmapLoaded = { viewModel.addToBitmapCache(song.id, it) },
                        onClick = { onSongClick(song) },
                        onLongClick = { onSongLongClick(song) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                
                item {
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }
}
