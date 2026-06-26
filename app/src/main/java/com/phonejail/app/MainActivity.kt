package com.phonejail.app

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.phonejail.app.data.FocusSession
import com.phonejail.app.session.SessionManager
import com.phonejail.app.ui.HomeScreen
import com.phonejail.app.ui.HomeViewModel
import com.phonejail.app.ui.theme.PhoneJailTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels()
    private var nfcAdapter: NfcAdapter? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PhoneJailTheme {
                val state by viewModel.uiState.collectAsState()
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    HomeScreen(
                        state = state,
                        onStart = { SessionManager.start(this, FocusSession.Source.MANUAL) },
                        onStop = { SessionManager.stop(this, FocusSession.EndReason.RELEASED) },
                        onDismissResult = { viewModel.dismissResult() },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }

        handleNfcIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Catch taps while the app is in the foreground (no app chooser).
        val pending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE,
        )
        nfcAdapter?.enableForegroundDispatch(this, pending, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    /** A tag tap toggles the session: start if idle, release if running. */
    private fun handleNfcIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val isTag = action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED
        if (!isTag) return

        if (SessionManager.isRunning) {
            SessionManager.stop(this, FocusSession.EndReason.RELEASED)
        } else {
            SessionManager.start(this, FocusSession.Source.NFC)
        }
    }
}
