package com.musique.partitions.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import com.musique.partitions.data.ThemeMode
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musique.partitions.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) vm.handleIntent(intent)
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status bar icons stay readable when the chosen theme differs from the system one.
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            AppTheme(settings.theme) { AppRoot(vm) }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.handleIntent(intent)
    }

    /** Android 13+: asked once, so the background check can post the "update available" notification. */
    private fun askNotificationsOnce() {
        if (Build.VERSION.SDK_INT < 33) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val prefs = getSharedPreferences("update_notif", Context.MODE_PRIVATE)
        if (prefs.getBoolean("permissionAsked", false)) return
        prefs.edit().putBoolean("permissionAsked", true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Bluetooth pedal or keyboard: handled here so it does not depend on Compose focus. */
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
        // Checks for updates at startup and on each return to the app (rate-limited by UpdateChecker).
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
        Screen.Favourites -> FavouritesScreen(vm)
        Screen.Settings -> SettingsScreen(vm)
    }
}
