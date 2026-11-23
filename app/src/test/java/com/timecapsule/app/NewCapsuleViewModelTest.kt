package com.timecapsule.app

import android.app.Application
import android.content.Context
import com.timecapsule.app.data.Draft
import com.timecapsule.app.data.Route
import com.timecapsule.app.ui.screens.NewCapsuleViewModel
import com.timecapsule.app.ui.screens.CapsuleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** El ViewModel de "Nueva cápsula" con una nube falsa: sin internet, sin Supabase y sin IA. */
@OptIn(ExperimentalCoroutinesApi::class)
class NewCapsuleViewModelTest {
    private class FakeCloud : CapsuleService {
        var createdOnes = mutableListOf<Draft>()
        var fails: Throwable? = null
        override suspend fun routes() = listOf(Route(id = "r1", title = "Viaje"))
        override suspend fun createNewRoute(name: String) = Route(id = "r2", title = name)
        override suspend fun nextInRoute(routeIdValue: String) = 3
        override suspend fun suggest(photos: List<String>, letter: String, place: String?) = listOf("Uno", "Dos", "Tres") to 0
        override suspend fun improveLetter(letter: String, title: String, tone: String) = "$letter (mejorada)"
        override suspend fun create(ctx: Context, d: Draft, progress: (String, Float) -> Unit): String {
            fails?.let { throw it }
            progress("Creando la cápsula…", 0.5f); createdOnes += d; return "cap-1"
        }
        override fun message(e: Throwable) = "Mensaje: ${e.message}"
    }

    private lateinit var cloud: FakeCloud
    private lateinit var vm: NewCapsuleViewModel

    @Before fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cloud = FakeCloud()
        vm = NewCapsuleViewModel(Application(), cloud, loadDraft = false)
    }

    @After fun after() = Dispatchers.resetMain()

    @Test fun `al abrir carga las rutas del usuario`() {
        assertEquals(listOf("r1"), vm.routes.map { it.id })
    }

    @Test fun `no deja sellar una capsula vacia y explica por que`() {
        vm.seal()
        assertEquals("Ponle un título a tu cápsula.", vm.error)
        assertTrue(cloud.createdOnes.isEmpty())
        assertFalse(vm.saving)
    }

    @Test fun `sella una capsula valida y avisa a la pantalla`() {
        vm.changeTitle("Mi graduación"); vm.changeLetter("Querido yo…"); vm.changeUsePlace(false)
        vm.seal()
        assertNull(vm.error)
        assertEquals("cap-1", vm.created)
        assertEquals("Mi graduación", cloud.createdOnes.single().title)
        assertNull("sin lugar no se envían coordenadas", cloud.createdOnes.single().lat)
    }

    @Test fun `si la nube falla muestra el mensaje y permite reintentar`() {
        cloud.fails = IllegalStateException("sin internet")
        vm.changeTitle("Hola"); vm.changeLetter("Carta"); vm.changeUsePlace(false)
        vm.seal()
        assertEquals("Mensaje: sin internet", vm.error)
        assertFalse(vm.saving)
        assertNull(vm.created)
    }

    @Test fun `la ia mejora la carta y se puede deshacer`() {
        vm.changeLetter("Hola")
        vm.improveLetter("cálido")
        assertEquals("Hola (mejorada)", vm.letter)
        vm.undoLetter()
        assertEquals("Hola", vm.letter)
        assertNull(vm.previousLetter)
    }

    @Test fun `la ia sugiere titulos`() {
        vm.changeLetter("Un viaje al mar")
        vm.suggestTitle()
        assertEquals(listOf("Uno", "Dos", "Tres"), vm.titleIdeas)
        assertNull(vm.aiBusy)
    }

    @Test fun `crear una ruta la selecciona`() {
        vm.createNewRoute("Colegio")
        assertEquals("r2", vm.routeIdValue)
        assertEquals("Colegio", vm.routes.first().title)
    }

    @Test fun `las preguntas no se repiten`() {
        vm.addQuestion("¿Sigues aquí?"); vm.addQuestion("¿Sigues aquí?")
        assertEquals(1, vm.questions.size)
    }

    @Test fun `el enlace de la cancion se pega solo si esta vacio`() {
        vm.pasteSongIfAny("mira https://youtu.be/abc123")
        assertNotNull(vm.songLink.ifBlank { null })
        val before = vm.songLink
        vm.pasteSongIfAny("https://open.spotify.com/track/otro")
        assertEquals(before, vm.songLink)
    }

    @Test fun `el borrador guarda lo escrito`() {
        vm.changeTitle("T"); vm.changeEmails("a@b.co")
        val b = vm.draft()
        assertEquals("T", b.title); assertEquals("a@b.co", b.emails)
    }
}
