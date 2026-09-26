package com.haven.music.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.haven.music.Playlist
import com.haven.music.Song

@Composable
fun PlaylistScreen(
    playlist: Playlist,
    playlistSongs: List<Song>,
    currentSong: Song?,
    viewModel: com.haven.music.MainViewModel,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAddSongs: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (Song) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(playlist.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
            // 1. Navigation Bar & Hero Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack, modifier = Modifier.tactilePress(onClick = onBack)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Text(
                                text = havenTransform("Playlist"),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }, modifier = Modifier.tactilePress(onClick = { showMenu = true })) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add Songs") },
                                    onClick = { 
                                        showMenu = false
                                        onAddSongs()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = { 
                                        showMenu = false
                                        showRenameDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Playlist", color = MaterialTheme.colorScheme.error) },
                                    onClick = { 
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = havenTransform(playlist.name),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = havenTransform("${playlistSongs.size} songs"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 2. Songs List
            itemsIndexed(playlistSongs, key = { _, s -> s.id }) { index, song ->
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
                        onToggleFavorite = { /* Long press menu handles this */ },
                        onLongClick = { onSongLongClick(song) }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Playlist") },
            text = {
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        onRename(newName)
                        showRenameDialog = false
                    }
                }) {
                    Text("Save", color = Color(0xFFFF9800))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Playlist?") },
            text = { Text("This will remove the playlist but will not delete your music.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
