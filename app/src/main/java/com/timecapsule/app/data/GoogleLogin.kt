package com.timecapsule.app.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import java.security.MessageDigest
import java.util.UUID

/**
 * CONTINUAR CON GOOGLE (inicio de sesión nativo de Android)
 * 1. Android muestra tus cuentas de Google (Credential Manager).
 * 2. Google entrega un "ID token" firmado para la app.
 * 3. Supabase lo verifica y abre la sesión (o crea la cuenta la primera vez).
 * Solo usa IDs de cliente (públicos): ninguna clave secreta va en la app.
 */
object GoogleLogin {
    /** El botón solo aparece cuando el ID de cliente web ya está configurado. */
    fun disponible() = Config.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /** Huella del "nonce": evita que alguien reutilice un token robado. */
    fun huella(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray()).joinToString("") { "%02x".format(it) }

    /** Devuelve false si la persona cerró la ventana de Google sin elegir cuenta. */
    suspend fun entrar(ctx: Context): Boolean {
        val nonce = UUID.randomUUID().toString()
        val opcion = GetSignInWithGoogleOption.Builder(Config.GOOGLE_WEB_CLIENT_ID).setNonce(huella(nonce)).build()
        val pedido = GetCredentialRequest.Builder().addCredentialOption(opcion).build()
        val respuesta = try {
            CredentialManager.create(ctx).getCredential(ctx, pedido)
        } catch (e: GetCredentialCancellationException) {
            return false
        } catch (e: NoCredentialException) {
            error("No hay cuentas de Google en este celular. Agrega una en Ajustes → Cuentas.")
        }
        val cred = respuesta.credential
        if (cred !is CustomCredential || cred.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            error("Google no devolvió una cuenta válida.")
        }
        val google = GoogleIdTokenCredential.createFrom(cred.data)
        supabase.auth.signInWith(IDToken) {
            idToken = google.idToken
            provider = Google
            this.nonce = nonce
        }
        return true
    }
}
