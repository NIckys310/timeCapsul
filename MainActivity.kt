package com.timecapsule.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timecapsule.app.notify.Notifier
import com.timecapsule.app.notify.SyncWorker
import com.timecapsule.app.ui.screens.AuthScreen
import com.timecapsule.app.ui.screens.CapsuleDetailScreen
import com.timecapsule.app.ui.screens.CreateCapsuleScreen
import com.timecapsule.app.ui.screens.HomeScreen
import com.timecapsule.app.ui.theme.TimeCapsuleTheme
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {
    /** Cápsula a abrir cuando se toca una notificación. */
    private val pendingCapsule = mutableStateOf<String?>(null)

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingCapsule.value = intent.getStringExtra(Notifier.EXTRA_CAPSULE)
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(this)) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            TimeCapsuleTheme {
                val vm: MainViewModel = viewModel()
                val status by vm.session.collectAsState()
                val nav = rememberNavController()
                val pending by pendingCapsule

                when (status) {
                    is SessionStatus.Initializing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is SessionStatus.Authenticated -> {
                        LaunchedEffect(Unit) {
                            vm.refresh()
                            SyncWorker.schedulePeriodic(applicationContext)
                        }
                        LaunchedEffect(pending) {
                            pending?.let { nav.navigate("capsule/$it"); pendingCapsule.value = null }
                        }
                        NavHost(nav, startDestination = "home") {
                            composable("home") {
                                HomeScreen(vm,
                                    onCreate = { nav.navigate("create") },
                                    onOpen = { id -> nav.navigate("capsule/$id") })
                            }
                            composable("create") {
                                CreateCapsuleScreen(
                                    onBack = { nav.popBackStack() },
                                    onCreated = { id ->
                                        vm.refresh()
                                        nav.popBackStack()
                                        nav.navigate("capsule/$id")
                                    })
                            }
                            composable("capsule/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                                CapsuleDetailScreen(
                                    id = entry.arguments?.getString("id").orEmpty(),
                                    onBack = { nav.popBackStack() },
                                    onChanged = { vm.refresh() })
                            }
                        }
                    }
                    else -> AuthScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifier.EXTRA_CAPSULE)?.let { pendingCapsule.value = it }
    }
}
