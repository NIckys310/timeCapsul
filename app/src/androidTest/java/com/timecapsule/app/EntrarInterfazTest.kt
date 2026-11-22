package com.timecapsule.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.timecapsule.app.ui.Idioma
import com.timecapsule.app.ui.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pruebas de interfaz de la entrada: se usan con toques y teclado reales.
 * No crean cuentas ni se conectan: solo revisan lo que pasa antes de enviar.
 */
@RunWith(AndroidJUnit4::class)
class EntrarInterfazTest {
    @get:Rule(order = 0) val permisos: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()
    private lateinit var app: ActivityScenario<MainActivity>
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun abrir() {
        Idioma.cambiar(ctx, "es")
        app = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { compose.onAllNodes(hasText("Continuar con Google")).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun cerrar() {
        Idioma.cambiar(ctx, "es")
        app.close()
    }

    private fun campo(etiqueta: String) = compose.onNode(hasSetTextAction() and hasText(etiqueta))

    @Test fun laEntradaOfreceCorreoYGoogle() {
        compose.onNodeWithText("Guarda hoy lo que querrás revivir mañana.").assertIsDisplayed()
        campo("Correo").assertIsDisplayed()
        campo("Contraseña").assertIsDisplayed()
        compose.onNodeWithText("Continuar con Google").performScrollTo().assertIsDisplayed()
    }

    @Test fun crearCuentaMuestraLoQueLeFaltaALaContrasena() {
        compose.onNodeWithText("Crear cuenta").performClick()
        campo("Contraseña").performTextInput("capsula")
        compose.onNodeWithText("○ Al menos 10 caracteres").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("✓ Una letra minúscula").assertIsDisplayed()
        compose.onNodeWithText("○ Un símbolo (!@#$…)").assertIsDisplayed()
    }

    @Test fun noDejaCrearUnaCuentaConContrasenaDebil() {
        compose.onNodeWithText("Crear cuenta").performClick()
        campo("Tu nombre").performTextInput("Prueba")
        campo("Correo").performTextInput("prueba@ejemplo.invalid")
        campo("Contraseña").performTextInput("Capsula2026")
        compose.onNodeWithText("Crear mi cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Tu contraseña aún no es segura. Mira lo que le falta.").performScrollTo().assertIsDisplayed()
    }

    @Test fun enInglesLaEntradaSeTraduce() {
        compose.runOnUiThread { Idioma.cambiar(ctx, "en") }
        compose.onNodeWithText("Continue with Google").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save today what you will want to relive tomorrow.").assertIsDisplayed()
    }
}
