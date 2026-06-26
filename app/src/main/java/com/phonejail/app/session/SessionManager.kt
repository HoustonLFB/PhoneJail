package com.phonejail.app.session

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.phonejail.app.data.FocusRepository
import com.phonejail.app.data.FocusSession
import com.phonejail.app.service.FocusService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for the live focus session. The UI observes [active];
 * [FocusService] keeps the timer alive and reports when the phone gets touched.
 */
object SessionManager {

    private lateinit var repository: FocusRepository
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _active = MutableStateFlow<ActiveSession?>(null)
    val active: StateFlow<ActiveSession?> = _active.asStateFlow()

    /** Last session result, so the UI can show a "released / broken" summary. */
    private val _lastResult = MutableStateFlow<FocusSession?>(null)
    val lastResult: StateFlow<FocusSession?> = _lastResult.asStateFlow()

    /** Guards against a tag re-tap right after a stop spinning up a fresh session. */
    private var lastEndedAt = 0L
    private const val RESTART_DEBOUNCE_MS = 1_500L

    fun init(repository: FocusRepository) {
        this.repository = repository
    }

    val isRunning: Boolean get() = _active.value != null

    /** Begin a session if one is not already running. Idempotent on re-tap. */
    fun start(context: Context, source: String) {
        if (_active.value != null) return
        if (System.currentTimeMillis() - lastEndedAt < RESTART_DEBOUNCE_MS) return
        _active.value = ActiveSession(startedAt = System.currentTimeMillis(), source = source)
        _lastResult.value = null
        startService(context.applicationContext)
    }

    /**
     * End the running session.
     * @param reason one of [FocusSession.EndReason].
     */
    fun stop(context: Context, reason: String) {
        val current = _active.value ?: return
        _active.value = null
        lastEndedAt = System.currentTimeMillis()

        val finished = current.finish(reason)
        _lastResult.value = finished
        scope.launch { repository.record(finished) }

        context.applicationContext.stopService(Intent(context, FocusService::class.java))
    }

    fun clearLastResult() {
        _lastResult.value = null
    }

    private fun startService(context: Context) {
        val intent = Intent(context, FocusService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }
}
