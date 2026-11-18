package com.musique.partitions.ui

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) vm.handleIntent(intent)
        setContent { AppTheme { AppRoot(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.handleIntent(intent)
    }

    /** Pédale Bluetooth ou clavier : traité ici pour ne pas dépendre du focus Compose. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (vm.stack.value.lastOrNull() is Screen.Viewer) {
            val direction = when (keyCode) {
                KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SPACE -> 1
                KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> -1
                else -> 0
            }
            if (direction != 0) {
                vm.pageTurns.tryEmit(direction)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        // Vérifie les mises à jour au démarrage et à chaque retour dans l'appli (limité par UpdateChecker).
        vm.checkUpdate()
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    val stack by vm.stack.collectAsStateWithLifecycle()
    BackHandler(enabled = stack.size > 1) { vm.pop() }
    when (val screen = stack.last()) {
        Screen.Home -> HomeScreen(vm)
        is Screen.Piece -> PieceScreen(vm, screen.id)
        is Screen.Viewer -> ViewerScreen(vm, screen.id)
        Screen.About -> AboutScreen(vm)
    }
}
