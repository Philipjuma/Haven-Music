package com.haven.music

import android.net.Uri
import java.util.*

data class MusicMix(
    val id: String,
    val title: String,
    val description: String,
    val songs: List<Song>,
    val imageUrls: List<Uri?>,
    val type: MixType = MixType.CARD
)

enum class MixType {
    HERO, CARD, VERTICAL_LIST, CAROUSEL
}

class MixGenerator(
    private val engine: RecommendationEngine,
    private val historyManager: HistoryManager
) {
    fun generateMixes(allSongs: List<Song>, favorites: Set<Long>): List<MusicMix> {
        if (allSongs.isEmpty()) return emptyList()
        
        val mixes = mutableListOf<MusicMix>()
        
        // Calculate Top Signals for Affinity
        val topArtists = allSongs.groupBy { it.artist }
            .mapValues { (_, songs) -> songs.sumOf { historyManager.getPlayCount(it.id) } }
            .entries.sortedByDescending { it.value }.take(5).map { it.key }.toSet()
            
        val topAlbums = allSongs.groupBy { it.album }
            .mapValues { (_, songs) -> songs.sumOf { historyManager.getPlayCount(it.id) } }
            .entries.sortedByDescending { it.value }.take(5).map { it.key }.toSet()

        val scoredSongs = allSongs.map { it to engine.calculateScore(it, it.id in favorites, topArtists, topAlbums) }
            .sortedByDescending { it.second }

        // 1. HERO: Today's Personal Mix
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val temporalTitle = when (hour) {
            in 5..11 -> "Morning Energy"
            in 18..22 -> "Evening Groove"
            in 23..24, in 0..4 -> "Late-night Lounge"
            else -> "Daily Discovery"
        }
        
        val heroSongs = scoredSongs.take(30).shuffled().take(12).map { it.first }
        if (heroSongs.isNotEmpty()) {
            mixes.add(MusicMix(
                id = "hero_mix",
                title = temporalTitle,
                description = "Customized for your current mood.",
                songs = heroSongs,
                imageUrls = heroSongs.take(4).map { it.albumArtUri },
                type = MixType.HERO
            ))
        }

        // 2. CURRENT OBSESSION (On Repeat Lately)
        // Approximate by sorting by recency and frequency
        val obsessionSongs = allSongs.filter { historyManager.getLastPlayed(it.id) > 0 }
            .sortedByDescending { 
                val daysAgo = (System.currentTimeMillis() - historyManager.getLastPlayed(it.id)) / (1000 * 60 * 60 * 24).toDouble()
                if (daysAgo < 7) historyManager.getPlayCount(it.id) else 0 
            }.take(8)
            
        if (obsessionSongs.isNotEmpty()) {
            mixes.add(MusicMix(
                id = "obsession_mix",
                title = "On Repeat Lately",
                description = "You can't get enough of these.",
                songs = obsessionSongs,
                imageUrls = obsessionSongs.take(4).map { it.albumArtUri },
                type = MixType.VERTICAL_LIST
            ))
        }

        // 3. NOSTALGIA MIX (You haven't heard this in a while)
        val nostalgiaSongs = allSongs.filter { it.id in favorites }
            .filter { 
                val lastPlayed = historyManager.getLastPlayed(it.id)
                lastPlayed > 0 && (System.currentTimeMillis() - lastPlayed) > (14L * 24 * 60 * 60 * 1000)
            }.shuffled().take(10)
            
        if (nostalgiaSongs.isNotEmpty()) {
            mixes.add(MusicMix(
                id = "nostalgia_mix",
                title = "Nostalgia Trip",
                description = "Remember these? It's been a while.",
                songs = nostalgiaSongs,
                imageUrls = nostalgiaSongs.take(4).map { it.albumArtUri },
                type = MixType.CARD
            ))
        }

        // 4. SUNDAY SOUND (If Sunday)
        if (dayOfWeek == Calendar.SUNDAY) {
            val sundaySongs = allSongs.filter { historyManager.getDayAffinity(it.id, Calendar.SUNDAY) > 0 }
                .shuffled().take(12)
            if (sundaySongs.isNotEmpty()) {
                mixes.add(MusicMix(
                    id = "sunday_mix",
                    title = "Your Sunday Sound",
                    description = "A perfect vibe for today.",
                    songs = sundaySongs,
                    imageUrls = sundaySongs.take(4).map { it.albumArtUri },
                    type = MixType.CAROUSEL
                ))
            }
        }

        // 5. HAVEN'S PICKS (Rule-based Exploration)
        val picks = scoredSongs.take(50).shuffled().take(10).map { it.first }
        if (picks.isNotEmpty()) {
            mixes.add(MusicMix(
                id = "haven_picks",
                title = "Haven's Picks",
                description = "I think you'll approve of these.",
                songs = picks,
                imageUrls = picks.take(4).map { it.albumArtUri },
                type = MixType.CARD
            ))
        }

        return mixes
    }
}
