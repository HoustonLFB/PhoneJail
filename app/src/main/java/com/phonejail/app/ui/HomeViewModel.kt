package com.phonejail.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phonejail.app.PhoneJailApp
import com.phonejail.app.data.FocusSession
import com.phonejail.app.data.FocusStats
import com.phonejail.app.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay

data class HomeUiState(
    val isRunning: Boolean = false,
    val elapsedMillis: Long = 0,
    val sessions: List<FocusSession> = emptyList(),
    val stats: FocusStats = FocusStats.EMPTY,
    val lastResult: FocusSession? = null,
)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as PhoneJailApp).repository

    // Drives the live timer; only the elapsed value changes each tick.
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }

    val uiState: StateFlow<HomeUiState> =
        combine(
            SessionManager.active,
            repository.sessions,
            SessionManager.lastResult,
            ticker,
        ) { active, sessions, lastResult, now ->
            HomeUiState(
                isRunning = active != null,
                elapsedMillis = active?.elapsed(now) ?: 0,
                sessions = sessions,
                stats = FocusStats.from(sessions, now),
                lastResult = lastResult,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun dismissResult() = SessionManager.clearLastResult()
}
