package com.haven.music

import android.content.Context
import android.content.SharedPreferences
import java.util.*

enum class TimeBucket {
    MORNING, AFTERNOON, EVENING, NIGHT
}

class HistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("haven_history", Context.MODE_PRIVATE)

    fun recordPlay(songId: Long) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val bucket = when (hour) {
            in 5..11 -> TimeBucket.MORNING
            in 12..17 -> TimeBucket.AFTERNOON
            in 18..22 -> TimeBucket.EVENING
            else -> TimeBucket.NIGHT
        }

        val count = getPlayCount(songId)
        prefs.edit()
            .putInt("play_$songId", count + 1)
            .putLong("last_$songId", now)
            .putInt("bucket_${bucket.name}_$songId", prefs.getInt("bucket_${bucket.name}_$songId", 0) + 1)
            .putInt("day_${dayOfWeek}_$songId", prefs.getInt("day_${dayOfWeek}_$songId", 0) + 1)
            .apply()
            
        // Track global artist affinity signals
        // Note: Actual artist extraction happens in ViewModel
    }

    fun recordSkip(songId: Long) {
        val count = getSkipCount(songId)
        prefs.edit().putInt("skip_$songId", count + 1).apply()
    }

    fun recordFinish(songId: Long) {
        val count = getFinishCount(songId)
        prefs.edit().putInt("finish_$songId", count + 1).apply()
    }

    fun getPlayCount(songId: Long): Int = prefs.getInt("play_$songId", 0)
    fun getSkipCount(songId: Long): Int = prefs.getInt("skip_$songId", 0)
    fun getFinishCount(songId: Long): Int = prefs.getInt("finish_$songId", 0)
    fun getLastPlayed(songId: Long): Long = prefs.getLong("last_$songId", 0L)
    
    fun getBucketAffinity(songId: Long, bucket: TimeBucket): Int = prefs.getInt("bucket_${bucket.name}_$songId", 0)
    fun getDayAffinity(songId: Long, dayOfWeek: Int): Int = prefs.getInt("day_${dayOfWeek}_$songId", 0)
    
    fun getCompletionRate(songId: Long): Float {
        val plays = getPlayCount(songId)
        if (plays == 0) return 0f
        return getFinishCount(songId).toFloat() / plays.toFloat()
    }
}
