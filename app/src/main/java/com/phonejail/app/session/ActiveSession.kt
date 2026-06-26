package com.phonejail.app.session

import com.phonejail.app.data.FocusSession

/** The session currently in progress (held in memory, not yet persisted). */
data class ActiveSession(
    val startedAt: Long,
    val source: String,
) {
    fun elapsed(now: Long = System.currentTimeMillis()): Long = (now - startedAt).coerceAtLeast(0)

    /** Build the row to persist once the session ends. */
    fun finish(endReason: String, now: Long = System.currentTimeMillis()): FocusSession {
        val end = now.coerceAtLeast(startedAt)
        return FocusSession(
            startedAt = startedAt,
            endedAt = end,
            durationMillis = end - startedAt,
            endReason = endReason,
            source = source,
        )
    }
}
