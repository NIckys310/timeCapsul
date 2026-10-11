package com.timecapsule.app.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.timecapsule.app.ui.components.LockScreen
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
import com.timecapsule.app.data.AppSettings
import com.timecapsule.app.notify.SyncWorker
import com.timecapsule.app.ui.screens.AuthScreen
import com.timecapsule.app.ui.screens.CapsuleDetailScreen
import com.timecapsule.app.ui.screens.CreateCapsuleScreen
import com.timecapsule.app.ui.screens.HomeScreen
import com.timecapsule.app.ui.theme.TimeCapsuleTheme
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : FragmentActivity() {
    /** Bloqueo con huella: se pide al abrir y al volver después de 30 s. */
    private val locked = mutableStateOf(false)
    private var leftAt = 0L

    /** Cápsula a abrir cuando se toca una notificación. */
    private val pendingCapsule = mutableStateOf<String?>(null)
    /** Código de invitación recibido por enlace/QR (timecapsule://invitar/CODIGO). */
    private val pendingInvite = mutableStateOf<String?>(null)
    /** Abrir directo "Nueva cápsula" (timecapsule://nueva, lo usa Gestores). */
    private val pendingCreate = mutableStateOf(false)

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingCapsule.value = intent.getStringExtra(Notifier.EXTRA_CAPSULE)
        pendingInvite.value = inviteCode(intent)
        pendingCreate.value = wantsCreate(intent)
        if (Build.VERSION.SDK_INT >= 33 && !Notifier.canNotify(this)) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        AppSettings.load(this)
        locked.value = AppSettings.look.value.appLock
        // Ocultar en recientes / bloquear capturas según el ajuste
        lifecycleScope.launch {
            AppSettings.look.collect { l ->
                if (l.hideRecents) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        setContent {
            val look by AppSettings.look.collectAsState()
            TimeCapsuleTheme(themeMode = look.theme, highContrast = look.highContrast, textScale = look.textScale) {
                val vm: MainViewModel = viewModel()
                val status by vm.session.collectAsState()
                val nav = rememberNavController()
                val pending by pendingCapsule

                val isLocked by locked
                // El bloqueo se pone ENCIMA de la app: lo que estabas haciendo no se borra
                Box(Modifier.fillMaxSize()) {
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
                        val create by pendingCreate
                        LaunchedEffect(create) {
                            if (create) { pendingCreate.value = false; nav.navigate("create") }
                        }
                        val invite by pendingInvite
                        LaunchedEffect(invite) {
                            invite?.let { code ->
                                pendingInvite.value = null
                                runCatching { com.timecapsule.app.data.Repo.acceptInvite(code) }
                                    .onSuccess { id -> vm.refresh(); nav.navigate("capsule/$id") }
                                    .onFailure { vm.message.value = com.timecapsule.app.data.Repo.friendly(it) }
                            }
                        }
                        NavHost(nav, startDestination = "home",
                            enterTransition = { androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(380)) { it / 3 } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(380)) },
                            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.scaleOut(targetScale = 0.96f) },
                            popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.96f) },
                            popExitTransition = { androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(320)) { it / 3 } + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(320)) },
                        ) {
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
                androidx.compose.animation.AnimatedVisibility(
                    visible = isLocked && status is SessionStatus.Authenticated,
                    enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(targetScale = 1.1f),
                ) { LockScreen(onUnlocked = { locked.value = false }) }
                }
            }
        }
    }

    override fun onStop() { super.onStop(); leftAt = System.currentTimeMillis() }
    override fun onStart() {
        super.onStart()
        if (AppSettings.look.value.appLock && leftAt > 0 && System.currentTimeMillis() - leftAt > 30_000) locked.value = true
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Notifier.EXTRA_CAPSULE)?.let { pendingCapsule.value = it }
        inviteCode(intent)?.let { pendingInvite.value = it }
        if (wantsCreate(intent)) pendingCreate.value = true
    }

    private fun wantsCreate(i: Intent?) = i?.data?.let { it.scheme == "timecapsule" && it.host == "nueva" } == true

    private fun inviteCode(i: Intent?): String? =
        i?.data?.takeIf { it.scheme == "timecapsule" && it.host == "invitar" }?.lastPathSegment?.uppercase()
}
