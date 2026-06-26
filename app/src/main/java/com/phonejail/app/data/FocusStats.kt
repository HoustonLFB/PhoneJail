package com.phonejail.app.data

import java.util.Calendar

/**
 * Aggregate numbers shown on the home screen and (later) shared Strava-style.
 */
data class FocusStats(
    val totalMillis: Long,
    val sessionCount: Int,
    val bestMillis: Long,
    val todayMillis: Long,
    val currentStreakDays: Int,
) {
    companion object {
        val EMPTY = FocusStats(0, 0, 0, 0, 0)

        /** Derive stats from the full history. Cheap enough to run on every change. */
        fun from(sessions: List<FocusSession>, now: Long = System.currentTimeMillis()): FocusStats {
            if (sessions.isEmpty()) return EMPTY

            val total = sessions.sumOf { it.durationMillis }
            val best = sessions.maxOf { it.durationMillis }

            val startOfToday = startOfDay(now)
            val todayMillis = sessions
                .filter { it.startedAt >= startOfToday }
                .sumOf { it.durationMillis }

            return FocusStats(
                totalMillis = total,
                sessionCount = sessions.size,
                bestMillis = best,
                todayMillis = todayMillis,
                currentStreakDays = currentStreak(sessions, now),
            )
        }

        /** Consecutive days (ending today or yesterday) with at least one session. */
        private fun currentStreak(sessions: List<FocusSession>, now: Long): Int {
            val days = sessions.map { startOfDay(it.startedAt) }.toSortedSet().toList().reversed()
            if (days.isEmpty()) return 0

            val today = startOfDay(now)
            val oneDay = 24L * 60 * 60 * 1000

            // Streak only counts if the most recent active day is today or yesterday.
            if (days.first() != today && days.first() != today - oneDay) return 0

            var streak = 0
            var expected = days.first()
            for (day in days) {
                if (day == expected) {
                    streak++
                    expected -= oneDay
                } else {
                    break
                }
            }
            return streak
        }

        private fun startOfDay(epochMillis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = epochMillis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
