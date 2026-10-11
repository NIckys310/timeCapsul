package com.timecapsule.app.ui.components

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.timecapsule.app.ui.theme.LocalPalette
import com.timecapsule.app.ui.theme.Typewriter

/** ¿El celular tiene huella, rostro o PIN configurado? */
fun canLock(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS

/** Pide la huella, el rostro o el PIN del celular. */
fun askUnlock(activity: FragmentActivity, onOk: () -> Unit, onFail: (String) -> Unit) {
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
        override fun onAuthenticationError(code: Int, msg: CharSequence) = onFail(msg.toString())
    })
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Desbloquear Time Capsule")
            .setSubtitle("Usa tu huella, tu rostro o el PIN del celular")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
    )
}

/** Pantalla de bloqueo: tus recuerdos quedan ocultos hasta que te identifiques. */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity
    val p = LocalPalette.current
    var msg by remember { mutableStateOf<String?>(null) }
    fun unlock() = askUnlock(activity, onOk = onUnlocked, onFail = { msg = it })
    LaunchedEffect(Unit) { unlock() }
    Column(
        Modifier.fillMaxSize().paper().padding(32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WaxSeal(size = 96.dp, glow = true)
        Spacer(Modifier.height(24.dp))
        Text("Tus recuerdos están sellados", style = MaterialTheme.typography.headlineSmall, color = p.ink, textAlign = TextAlign.Center)
        Text("Identifícate para abrir Time Capsule", fontFamily = Typewriter, color = p.muted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = ::unlock) { Icon(Icons.Filled.Fingerprint, null); Text("  Desbloquear") }
        msg?.let { Text(it, color = p.wax, modifier = Modifier.padding(top = 12.dp)) }
    }
}
