package com.haven.music

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(val context: Context) {

    suspend fun fetchLocalSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        
        // Move Prefs reading outside the loop for performance
        val savedFolders = context.getSharedPreferences("haven_library", Context.MODE_PRIVATE)
            .getStringSet("music_folders", emptySet()) ?: emptySet()

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val dataPath = cursor.getString(dataColumn)
                
                // Only filter if folders have been explicitly selected and matched
                // For now, we restore full library access to fix the bug
                if (savedFolders.isNotEmpty()) {
                    val isIncluded = savedFolders.any { folder -> 
                        // Smarter matching: check if the file path contains the folder fragment
                        // or if it's a URI we can resolve (future-proofing)
                        val folderPath = if (folder.startsWith("content://")) {
                            // Extract basic folder name or ID for a loose match
                            folder.substringAfterLast("%3A", "").ifBlank { folder.substringAfterLast("/") }
                        } else folder
                        
                        folderPath.isNotBlank() && dataPath.contains(folderPath, ignoreCase = true)
                    }
                    // If no match found for any selected folder, skip this song
                    if (!isIncluded) continue
                }

                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                songsList.add(Song(id, title, artist, album, duration, albumArtUri, contentUri))
            }
        }
        return@withContext songsList
    }
}
