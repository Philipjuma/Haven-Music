package com.haven.music

import java.util.*
import kotlin.math.ln

class RecommendationEngine(private val historyManager: HistoryManager) {

    fun calculateScore(
        song: Song, 
        isFavorite: Boolean,
        topArtists: Set<String> = emptySet(),
        topAlbums: Set<String> = emptySet()
    ): Double {
        var score = 0.0
        
        // 1. Play Count Weight (Logarithmic)
        val plays = historyManager.getPlayCount(song.id)
        score += ln(plays + 1.0) * 15.0
        
        // 2. Favorite Weight (Huge boost)
        if (isFavorite) score += 60.0
        
        // 3. Completion Rate (Quality over Quantity)
        val completion = historyManager.getCompletionRate(song.id)
        score += completion * 30.0
        
        // 4. Skip Penalty (Heavy)
        val skips = historyManager.getSkipCount(song.id)
        score -= skips * 15.0
        
        // 5. Recency & Temporal Affinity
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val bucket = when (hour) {
            in 5..11 -> TimeBucket.MORNING
            in 12..17 -> TimeBucket.AFTERNOON
            in 18..22 -> TimeBucket.EVENING
            else -> TimeBucket.NIGHT
        }
        
        val bucketPlays = historyManager.getBucketAffinity(song.id, bucket)
        if (bucketPlays > 0) score += 20.0 // Strong signal for current time
        
        val dayPlays = historyManager.getDayAffinity(song.id, dayOfWeek)
        if (dayPlays > 0) score += 10.0 // Sunday Sounds, etc.

        val lastPlayed = historyManager.getLastPlayed(song.id)
        if (lastPlayed > 0) {
            val millisAgo = System.currentTimeMillis() - lastPlayed
            val daysAgo = millisAgo / (1000 * 60 * 60 * 24).toDouble()
            // Recent boost (within 3 days)
            if (daysAgo < 3) {
                score += (25.0 / (daysAgo + 1.0))
            } else {
                score += (5.0 / (daysAgo + 1.0))
            }
        }
        
        // 6. Artist & Album Affinity
        if (topArtists.contains(song.artist)) score += 15.0
        if (topAlbums.contains(song.album)) score += 10.0
        
        return score.coerceAtLeast(0.0)
    }
}
