package com.dailyassistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dailyassistant.ui.AppRoot
import com.dailyassistant.ui.AppTheme
import com.dailyassistant.vm.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startRoute = intent?.getStringExtra(EXTRA_ROUTE)
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsState()
            val pendingRoute = remember { mutableStateOf(startRoute) }
            AppTheme(themeMode = settings.themeMode) {
                AppRoot(vm = vm, deepLinkRoute = pendingRoute.value, onDeepLinkHandled = { pendingRoute.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    companion object {
        const val EXTRA_ROUTE = "route"
    }
}
