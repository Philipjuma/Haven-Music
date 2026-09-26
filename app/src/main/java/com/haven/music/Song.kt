package com.haven.music

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val albumArtUri: Uri?,
    val contentUri: Uri,
    val lyrics: String? = null,
    val isOnline: Boolean = false,
    val provider: String? = null
)
