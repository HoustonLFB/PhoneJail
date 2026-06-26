package com.phonejail.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class FocusStatsTest {

    private val oneDay = 24L * 60 * 60 * 1000

    /** A fixed "now" at midday so day-boundary math is unambiguous. */
    private val now: Long = Calendar.getInstance().apply {
        set(2026, Calendar.JUNE, 26, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun session(startedAt: Long, durationMillis: Long) = FocusSession(
        startedAt = startedAt,
        endedAt = startedAt + durationMillis,
        durationMillis = durationMillis,
        endReason = FocusSession.EndReason.RELEASED,
        source = FocusSession.Source.NFC,
    )

    @Test
    fun emptyHistoryHasZeroStats() {
        assertEquals(FocusStats.EMPTY, FocusStats.from(emptyList(), now))
    }

    @Test
    fun aggregatesTotalBestAndToday() {
        val sessions = listOf(
            session(now, 10 * 60_000),                 // today, 10m
            session(now - 30 * 60_000, 25 * 60_000),   // today, 25m (best)
            session(now - 2 * oneDay, 5 * 60_000),     // 2 days ago, 5m
        )

        val stats = FocusStats.from(sessions, now)

        assertEquals(40 * 60_000L, stats.totalMillis)
        assertEquals(25 * 60_000L, stats.bestMillis)
        assertEquals(35 * 60_000L, stats.todayMillis)
        assertEquals(3, stats.sessionCount)
    }

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val sessions = listOf(
            session(now, 60_000),
            session(now - oneDay, 60_000),
            session(now - 2 * oneDay, 60_000),
        )
        assertEquals(3, FocusStats.from(sessions, now).currentStreakDays)
    }

    @Test
    fun streakBreaksOnGapAndIgnoresStaleHistory() {
        // Most recent activity was 3 days ago -> streak is 0.
        val stale = listOf(
            session(now - 3 * oneDay, 60_000),
            session(now - 4 * oneDay, 60_000),
        )
        assertEquals(0, FocusStats.from(stale, now).currentStreakDays)

        // Active today + yesterday, then a gap -> streak of 2.
        val withGap = listOf(
            session(now, 60_000),
            session(now - oneDay, 60_000),
            session(now - 3 * oneDay, 60_000),
        )
        assertEquals(2, FocusStats.from(withGap, now).currentStreakDays)
    }
}
