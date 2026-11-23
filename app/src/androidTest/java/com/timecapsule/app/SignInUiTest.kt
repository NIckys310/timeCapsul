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
import com.timecapsule.app.ui.Language
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
class SignInUiTest {
    @get:Rule(order = 0) val permissions: GrantPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()
    private lateinit var app: ActivityScenario<MainActivity>
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun openNow() {
        Language.change(ctx, "es")
        app = ActivityScenario.launch(MainActivity::class.java)
        compose.waitUntil(15_000) { compose.onAllNodes(hasText("Continuar con Google")).fetchSemanticsNodes().isNotEmpty() }
    }

    @After fun close() {
        Language.change(ctx, "es")
        app.close()
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))

    @Test fun signInOffersEmailAndGoogle() {
        compose.onNodeWithText("Guarda hoy lo que querrás revivir mañana.").assertIsDisplayed()
        field("Correo").assertIsDisplayed()
        field("Contraseña").assertIsDisplayed()
        compose.onNodeWithText("Continuar con Google").performScrollTo().assertIsDisplayed()
    }

    @Test fun signUpShowsWhatThePasswordIsMissing() {
        compose.onNodeWithText("Crear cuenta").performClick()
        field("Contraseña").performTextInput("capsula")
        compose.onNodeWithText("○ Al menos 10 caracteres").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("✓ Una letra minúscula").assertIsDisplayed()
        compose.onNodeWithText("○ Un símbolo (!@#$…)").assertIsDisplayed()
    }

    @Test fun doesNotAllowSignUpWithAWeakPassword() {
        compose.onNodeWithText("Crear cuenta").performClick()
        field("Tu nombre").performTextInput("Prueba")
        field("Correo").performTextInput("prueba@ejemplo.invalid")
        field("Contraseña").performTextInput("Capsula2026")
        compose.onNodeWithText("Crear mi cuenta").performScrollTo().performClick()
        compose.onNodeWithText("Tu contraseña aún no es segura. Mira lo que le falta.").performScrollTo().assertIsDisplayed()
    }

    @Test fun inEnglishTheSignInIsTranslated() {
        compose.runOnUiThread { Language.change(ctx, "en") }
        compose.onNodeWithText("Continue with Google").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save today what you will want to relive tomorrow.").assertIsDisplayed()
    }
}
