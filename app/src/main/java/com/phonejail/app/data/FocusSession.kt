package com.phonejail.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One completed focus session — a stint where the phone sat "in jail".
 *
 * @param startedAt epoch millis when the session began (tag tap / manual start).
 * @param endedAt epoch millis when it ended.
 * @param durationMillis convenience copy of (endedAt - startedAt).
 * @param endReason how it ended — see [EndReason].
 * @param source what kicked it off — see [Source].
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val durationMillis: Long,
    val endReason: String,
    val source: String,
) {
    val released: Boolean get() = endReason == EndReason.RELEASED

    object EndReason {
        /** Ended on purpose (tapped the tag again / pressed stop). A "good" finish. */
        const val RELEASED = "released"

        /** Broken early because the phone was picked up / unlocked. */
        const val BROKEN = "broken"
    }

    object Source {
        const val NFC = "nfc"
        const val MANUAL = "manual"
    }
}
