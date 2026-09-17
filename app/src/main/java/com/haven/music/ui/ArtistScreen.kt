package com.haven.music.ui

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.haven.music.Album
import com.haven.music.Song

@Composable
fun ArtistScreen(
    artistName: String,
    artistSongs: List<Song>,
    artistAlbums: List<Album>,
    viewModel: com.haven.music.MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: (List<Song>) -> Unit,
    onShuffleAll: (List<Song>) -> Unit,
    onAlbumClick: (Album) -> Unit
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
                    text = havenTransform("Artist"),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // 1. Premium Hero Section
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.size(180.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.05f),
                            border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.3f))
                        ) {
                            AsyncImage(
                                model = artistSongs.firstOrNull()?.albumArtUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = havenTransform(artistName, isArtistName = true),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                            color = Color.White
                        )
                        
                        Text(
                            text = havenTransform("${artistSongs.size} songs • ${artistAlbums.size} albums"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                // 2. Action Hub
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(artistSongs) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(52.dp).weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PLAY ALL", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                        
                        OutlinedButton(
                            onClick = { onShuffleAll(artistSongs) },
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(52.dp).weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHUFFLE", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // 3. Songs List
                item {
                    Text(
                        text = havenTransform("Popular Tracks"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                }
                
                items(artistSongs, key = { it.id }) { song ->
                    DetailSongItem(
                        song = song, 
                        itemColor = viewModel.albumArtColors[song.id], 
                        cachedBitmap = viewModel.bitmapCache.get(song.id),
                        onBitmapLoaded = { viewModel.addToBitmapCache(song.id, it) },
                        onClick = { onSongClick(song) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 4. Albums Grid
                if (artistAlbums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = havenTransform("Albums"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }
                    
                    val chunks = artistAlbums.chunked(2)
                    items(chunks, key = { it.first().name + it.first().artist }) { rowAlbums ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowAlbums.forEach { album ->
                                Box(modifier = Modifier.weight(1f)) {
                                    ArtistAlbumItem(album = album, onClick = { onAlbumClick(album) })
                                }
                            }
                            if (rowAlbums.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }
}

@Composable
fun ArtistAlbumItem(album: Album, onClick: () -> Unit) {
    Column(modifier = Modifier.tactilePress(onClick = onClick)) {
        Surface(
            modifier = Modifier.aspectRatio(1f).fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.Black,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            AsyncImage(
                model = album.artUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Text(
            text = havenTransform(album.name),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
        )
    }
}
