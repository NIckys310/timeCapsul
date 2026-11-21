package com.timecapsule.app

import android.app.Application
import android.content.Context
import com.timecapsule.app.data.Draft
import com.timecapsule.app.data.Route
import com.timecapsule.app.ui.screens.NuevaCapsulaViewModel
import com.timecapsule.app.ui.screens.ServicioCapsulas
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
class NuevaCapsulaViewModelTest {
    private class NubeFalsa : ServicioCapsulas {
        var creadas = mutableListOf<Draft>()
        var falla: Throwable? = null
        override suspend fun rutas() = listOf(Route(id = "r1", title = "Viaje"))
        override suspend fun crearRuta(nombre: String) = Route(id = "r2", title = nombre)
        override suspend fun siguienteEnRuta(rutaId: String) = 3
        override suspend fun sugerir(fotos: List<String>, carta: String, lugar: String?) = listOf("Uno", "Dos", "Tres") to 0
        override suspend fun mejorarCarta(carta: String, titulo: String, tono: String) = "$carta (mejorada)"
        override suspend fun crear(ctx: Context, d: Draft, progreso: (String, Float) -> Unit): String {
            falla?.let { throw it }
            progreso("Creando la cápsula…", 0.5f); creadas += d; return "cap-1"
        }
        override fun mensaje(e: Throwable) = "Mensaje: ${e.message}"
    }

    private lateinit var nube: NubeFalsa
    private lateinit var vm: NuevaCapsulaViewModel

    @Before fun antes() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        nube = NubeFalsa()
        vm = NuevaCapsulaViewModel(Application(), nube, cargarBorrador = false)
    }

    @After fun despues() = Dispatchers.resetMain()

    @Test fun `al abrir carga las rutas del usuario`() {
        assertEquals(listOf("r1"), vm.rutas.map { it.id })
    }

    @Test fun `no deja sellar una capsula vacia y explica por que`() {
        vm.sellar()
        assertEquals("Ponle un título a tu cápsula.", vm.error)
        assertTrue(nube.creadas.isEmpty())
        assertFalse(vm.guardando)
    }

    @Test fun `sella una capsula valida y avisa a la pantalla`() {
        vm.cambiarTitulo("Mi graduación"); vm.cambiarCarta("Querido yo…"); vm.cambiarUsarLugar(false)
        vm.sellar()
        assertNull(vm.error)
        assertEquals("cap-1", vm.creada)
        assertEquals("Mi graduación", nube.creadas.single().title)
        assertNull("sin lugar no se envían coordenadas", nube.creadas.single().lat)
    }

    @Test fun `si la nube falla muestra el mensaje y permite reintentar`() {
        nube.falla = IllegalStateException("sin internet")
        vm.cambiarTitulo("Hola"); vm.cambiarCarta("Carta"); vm.cambiarUsarLugar(false)
        vm.sellar()
        assertEquals("Mensaje: sin internet", vm.error)
        assertFalse(vm.guardando)
        assertNull(vm.creada)
    }

    @Test fun `la ia mejora la carta y se puede deshacer`() {
        vm.cambiarCarta("Hola")
        vm.mejorarCarta("cálido")
        assertEquals("Hola (mejorada)", vm.carta)
        vm.deshacerCarta()
        assertEquals("Hola", vm.carta)
        assertNull(vm.cartaAnterior)
    }

    @Test fun `la ia sugiere titulos`() {
        vm.cambiarCarta("Un viaje al mar")
        vm.sugerirTitulo()
        assertEquals(listOf("Uno", "Dos", "Tres"), vm.ideasDeTitulo)
        assertNull(vm.iaOcupada)
    }

    @Test fun `crear una ruta la selecciona`() {
        vm.crearRuta("Colegio")
        assertEquals("r2", vm.rutaId)
        assertEquals("Colegio", vm.rutas.first().title)
    }

    @Test fun `las preguntas no se repiten`() {
        vm.agregarPregunta("¿Sigues aquí?"); vm.agregarPregunta("¿Sigues aquí?")
        assertEquals(1, vm.preguntas.size)
    }

    @Test fun `el enlace de la cancion se pega solo si esta vacio`() {
        vm.pegarCancionSiHay("mira https://youtu.be/abc123")
        assertNotNull(vm.enlaceCancion.ifBlank { null })
        val antes = vm.enlaceCancion
        vm.pegarCancionSiHay("https://open.spotify.com/track/otro")
        assertEquals(antes, vm.enlaceCancion)
    }

    @Test fun `el borrador guarda lo escrito`() {
        vm.cambiarTitulo("T"); vm.cambiarCorreos("a@b.co")
        val b = vm.borrador()
        assertEquals("T", b.title); assertEquals("a@b.co", b.emails)
    }
}
