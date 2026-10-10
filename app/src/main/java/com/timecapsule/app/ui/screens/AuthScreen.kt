package com.timecapsule.app.ui.screens

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import com.timecapsule.app.ui.theme.Typewriter
import com.timecapsule.app.ui.components.WaxSeal
import com.timecapsule.app.ui.components.floating
import com.timecapsule.app.ui.components.paper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timecapsule.app.R
import com.timecapsule.app.data.Repo
import com.timecapsule.app.ui.theme.Muted
import kotlinx.coroutines.launch

@Composable
fun AuthScreen() {
    var register by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        msg = null
        if (email.isBlank() || pass.length < 6 || (register && name.isBlank())) {
            msg = "Completa los datos. La contraseña necesita al menos 6 caracteres."; return
        }
        busy = true
        scope.launch {
            runCatching {
                if (register) {
                    val ready = Repo.signUp(name, email, pass)
                    if (!ready) msg = "Te enviamos un correo para confirmar la cuenta. Ábrelo y luego inicia sesión."
                } else Repo.signIn(email, pass)
            }.onFailure { msg = Repo.friendly(it) }
            busy = false
        }
    }

    Column(
        Modifier.fillMaxSize().paper().systemBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(120.dp).floating(amplitude = 4.dp).clip(RoundedCornerShape(6.dp)))
            WaxSeal(Modifier.offset(x = 14.dp, y = 14.dp), size = 46.dp, glow = true)
        }
        Spacer(Modifier.height(24.dp))
        Text("Time Capsule", style = MaterialTheme.typography.displaySmall)
        Text("Guarda hoy lo que querrás revivir mañana.", color = Muted, textAlign = TextAlign.Center, fontFamily = Typewriter)
        Spacer(Modifier.height(28.dp))

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = !register, onClick = { register = false },
                shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Iniciar sesión") }
            SegmentedButton(selected = register, onClick = { register = true },
                shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Crear cuenta") }
        }
        Spacer(Modifier.height(16.dp))
        if (register) {
            OutlinedTextField(name, { name = it }, label = { Text("Tu nombre") }, singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(email, { email = it }, label = { Text("Correo") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text("Contraseña") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth())
        msg?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.Center) }
        Spacer(Modifier.height(20.dp))
        Button(onClick = ::submit, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text(if (register) "Crear mi cuenta" else "Entrar")
        }
        if (!register) TextButton(onClick = { register = true }) { Text("¿No tienes cuenta? Crea una") }
    }
}
