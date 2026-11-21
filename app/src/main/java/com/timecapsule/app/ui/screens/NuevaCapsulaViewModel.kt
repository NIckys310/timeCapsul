package com.timecapsule.app.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.timecapsule.app.data.Draft
import com.timecapsule.app.data.DraftStore
import com.timecapsule.app.data.FormularioCapsula
import com.timecapsule.app.data.PickedItem
import com.timecapsule.app.data.ReglasCapsula
import com.timecapsule.app.data.Repo
import com.timecapsule.app.data.Route
import com.timecapsule.app.data.SavedDraft
import com.timecapsule.app.data.SavedItem
import com.timecapsule.app.util.LocationUtil
import com.timecapsule.app.util.MediaUtil
import com.timecapsule.app.util.NearbyPlace
import com.timecapsule.app.util.Places
import com.timecapsule.app.util.VoiceRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

/** Lo que "Nueva cápsula" necesita de la nube. En las pruebas se reemplaza por una versión falsa. */
interface ServicioCapsulas {
    suspend fun rutas(): List<Route>
    suspend fun crearRuta(nombre: String): Route
    suspend fun siguienteEnRuta(rutaId: String): Int
    suspend fun sugerir(fotos: List<String>, carta: String, lugar: String?): Pair<List<String>, Int>
    suspend fun mejorarCarta(carta: String, titulo: String, tono: String): String
    suspend fun crear(ctx: Context, d: Draft, progreso: (String, Float) -> Unit): String
    fun mensaje(e: Throwable): String
}

/** La versión real: Supabase y la función de IA. */
object ServicioSupabase : ServicioCapsulas {
    override suspend fun rutas() = Repo.routes()
    override suspend fun crearRuta(nombre: String) = Repo.createRoute(nombre)
    override suspend fun siguienteEnRuta(rutaId: String) = Repo.nextRouteOrder(rutaId)
    override suspend fun sugerir(fotos: List<String>, carta: String, lugar: String?) = Repo.aiSuggest(fotos, carta, lugar)
    override suspend fun mejorarCarta(carta: String, titulo: String, tono: String) = Repo.aiLetter(carta, titulo, tono)
    override suspend fun crear(ctx: Context, d: Draft, progreso: (String, Float) -> Unit) = Repo.create(ctx, d, progreso)
    override fun mensaje(e: Throwable) = Repo.friendly(e)
}

/**
 * NUEVA CÁPSULA · el estado y la lógica de la pantalla.
 * Vive más que la pantalla: girar el celular, abrir la cámara o la galería no borra nada.
 * Si Android cierra la app, el borrador automático (DraftStore) lo recupera.
 * La pantalla (CreateCapsuleScreen) solo dibuja y le avisa a este ViewModel.
 */
class NuevaCapsulaViewModel(
    app: Application,
    private val nube: ServicioCapsulas,
    private val cargarBorrador: Boolean = true,
) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()

    // ---------- Contenido ----------
    var titulo by mutableStateOf(""); private set
    var carta by mutableStateOf(""); private set
    val medios = mutableStateListOf<PickedItem>()
    val notasDeVoz = mutableStateListOf<File>()
    var cancion by mutableStateOf(""); private set
    var enlaceCancion by mutableStateOf(""); private set
    val preguntas = mutableStateListOf<String>()
    var plantilla by mutableStateOf<String?>(null); private set

    // ---------- Apertura ----------
    var usarFecha by mutableStateOf(true); private set
    var abreEl by mutableStateOf<Instant?>(ZonedDateTime.now().plusYears(1).toInstant()); private set
    var usarClave by mutableStateOf(false); private set
    var clave by mutableStateOf(""); private set
    var clave2 by mutableStateOf(""); private set

    // ---------- Lugar ----------
    var usarLugar by mutableStateOf(true); private set
    var soloEnElLugar by mutableStateOf(false); private set
    var nombreLugar by mutableStateOf(""); private set
    var direccion by mutableStateOf<String?>(null); private set
    var cercanos by mutableStateOf<List<NearbyPlace>>(emptyList()); private set
    var lat by mutableStateOf<Double?>(null); private set
    var lng by mutableStateOf<Double?>(null); private set
    var precision by mutableStateOf<Int?>(null); private set
    var buscandoLugar by mutableStateOf(false); private set
    var radio by mutableIntStateOf(150); private set
    var rutas by mutableStateOf<List<Route>>(emptyList()); private set
    var rutaId by mutableStateOf<String?>(null); private set

    // ---------- Compartir ----------
    var correos by mutableStateOf(""); private set
    var colaborativa by mutableStateOf(false); private set
    var abrirJuntos by mutableStateOf(false); private set

    // ---------- Estado de la pantalla ----------
    var error by mutableStateOf<String?>(null); private set
    var guardando by mutableStateOf(false); private set
    var progreso by mutableFloatStateOf(0f); private set
    var textoProgreso by mutableStateOf(""); private set
    /** Id de la cápsula creada: la pantalla la abre y se cierra. */
    var creada by mutableStateOf<String?>(null); private set
    var borradorRecuperado by mutableStateOf(false); private set
    var iaOcupada by mutableStateOf<String?>(null); private set
    var ideasDeTitulo by mutableStateOf<List<String>>(emptyList()); private set
    var cartaAnterior by mutableStateOf<String?>(null); private set
    var grabando by mutableStateOf(false); private set
    var msGrabados by mutableLongStateOf(0L); private set
    var verFecha by mutableStateOf(false)
    var verHora by mutableStateOf(false)
    var fechaElegida by mutableStateOf<LocalDate?>(null)
    /** Foto que la cámara está tomando (sobrevive si se gira el celular mientras tanto). */
    var fotoEnCamara by mutableStateOf<Uri?>(null)

    private val grabadora by lazy { VoiceRecorder(ctx) }

    init {
        viewModelScope.launch {
            rutas = runCatching { nube.rutas() }.onFailure { registrar("rutas", it) }.getOrDefault(emptyList())
        }
        if (cargarBorrador) viewModelScope.launch {
            DraftStore.load(ctx)?.let { restaurar(it) }
            guardarBorradorSolo()
        }
    }

    // ---------- Cambios desde la pantalla ----------
    fun cambiarTitulo(v: String) { titulo = v }
    fun cambiarCarta(v: String) { carta = v }
    fun cambiarCancion(v: String) { cancion = v }
    fun cambiarEnlaceCancion(v: String) { enlaceCancion = v }
    fun cambiarUsarFecha(v: Boolean) { usarFecha = v }
    fun cambiarAbreEl(v: Instant?) { abreEl = v }
    fun cambiarUsarClave(v: Boolean) { usarClave = v }
    fun cambiarClave(v: String) { clave = v }
    fun cambiarClave2(v: String) { clave2 = v }
    fun cambiarUsarLugar(v: Boolean) { usarLugar = v }
    fun cambiarSoloEnElLugar(v: Boolean) { soloEnElLugar = v }
    fun cambiarNombreLugar(v: String) { nombreLugar = v }
    fun cambiarRadio(v: Int) { radio = v }
    fun cambiarRuta(v: String?) { rutaId = v }
    fun cambiarCorreos(v: String) { correos = v }
    fun cambiarColaborativa(v: Boolean) { colaborativa = v }
    fun cambiarAbrirJuntos(v: Boolean) { abrirJuntos = v }
    fun mostrarError(v: String?) { error = v }

    fun usarPlantilla(id: String, texto: String) { plantilla = id; carta = ReglasCapsula.conPlantilla(carta, texto) }
    fun deshacerCarta() { cartaAnterior?.let { carta = it }; cartaAnterior = null }
    fun quitarMedio(i: Int) { medios.removeAt(i) }
    fun quitarPregunta(i: Int) { preguntas.removeAt(i) }
    fun agregarPregunta(q: String) {
        val nuevas = ReglasCapsula.agregarPregunta(preguntas, q)
        if (nuevas.size != preguntas.size) preguntas += nuevas.last()
    }

    /** Al volver de YouTube o Spotify: si copiaste el enlace de la canción, se pega solo. */
    fun pegarCancionSiHay(portapapeles: String) {
        if (enlaceCancion.isBlank()) ReglasCapsula.enlaceDeCancion(portapapeles)?.let { enlaceCancion = it }
    }

    fun empezarDeNuevo() {
        runCatching { DraftStore.clear(ctx) }.onFailure { registrar("borrador", it) }; borradorRecuperado = false
        titulo = ""; carta = ""; medios.clear(); notasDeVoz.clear(); cancion = ""; enlaceCancion = ""
        preguntas.clear(); plantilla = null; correos = ""
    }

    // ---------- Fotos y videos ----------
    fun agregarMedios(uris: List<Uri>) {
        var omitidos = 0
        uris.forEach { u ->
            if (medios.any { it.uri == u }) return@forEach
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                .onFailure { registrar("permiso de archivo", it) }
            val tipo = MediaUtil.kindOf(ctx, u)
            if (ReglasCapsula.cabe(tipo, medios.count { it.kind == "photo" }, medios.count { it.kind == "video" })) medios += PickedItem(u, tipo)
            else omitidos++
        }
        if (omitidos > 0) error = "Máximo ${ReglasCapsula.MAX_FOTOS} fotos y ${ReglasCapsula.MAX_VIDEOS} video por cápsula. Se omitieron $omitidos."
    }

    fun fotoTomada(ok: Boolean) {
        val uri = fotoEnCamara ?: return
        fotoEnCamara = null
        if (!ok) return
        if (ReglasCapsula.cabe("photo", medios.count { it.kind == "photo" }, 0)) medios += PickedItem(uri, "photo")
        else error = "Máximo ${ReglasCapsula.MAX_FOTOS} fotos por cápsula."
    }

    // ---------- Notas de voz ----------
    fun alternarGrabacion(permisoDado: Boolean) {
        if (grabando) { detenerGrabacion(); return }
        if (!permisoDado) { error = "Sin permiso del micrófono no se pueden grabar notas de voz."; return }
        runCatching { grabadora.start(); grabando = true }
            .onFailure { registrar("micrófono", it); error = "No se pudo usar el micrófono." }
        if (grabando) viewModelScope.launch {
            while (grabando) {
                msGrabados = grabadora.elapsedMs
                if (msGrabados >= ReglasCapsula.NOTA_DE_VOZ_MAX_MS) detenerGrabacion()
                delay(200)
            }
        }
    }

    private fun detenerGrabacion() {
        grabadora.stop()?.let { notasDeVoz += it }
        grabando = false
    }

    fun borrarNota(i: Int) { notasDeVoz.removeAt(i).delete() }

    // ---------- Lugar ----------
    /** Toma el GPS real y busca la dirección y los sitios con nombre alrededor. */
    fun ubicar() {
        buscandoLugar = true
        viewModelScope.launch {
            val loc = runCatching { LocationUtil.current(ctx) }.onFailure { registrar("GPS", it) }.getOrNull()
            if (loc == null) {
                error = "No se pudo obtener tu ubicación. Revisa que el GPS esté encendido."
            } else {
                lat = loc.latitude; lng = loc.longitude; precision = loc.accuracy.toInt()
                direccion = Places.address(ctx, loc.latitude, loc.longitude)
                cercanos = Places.nearby(loc.latitude, loc.longitude)
                if (nombreLugar.isBlank()) nombreLugar = ReglasCapsula.lugarSugerido(cercanos, direccion)
            }
            buscandoLugar = false
        }
    }

    fun crearRuta(nombre: String) = viewModelScope.launch {
        runCatching { nube.crearRuta(nombre) }
            .onSuccess { r -> rutas = listOf(r) + rutas; rutaId = r.id }
            .onFailure { error = nube.mensaje(it) }
    }

    // ---------- IA ----------
    fun sugerirTitulo() {
        iaOcupada = "titulo"; error = null
        viewModelScope.launch {
            runCatching {
                val fotos = medios.filter { it.kind == "photo" }.take(3)
                val imagenes = fotos.map { Base64.encodeToString(MediaUtil.compressPhoto(ctx, it.uri, 640), Base64.NO_WRAP) }
                val (titulos, portada) = nube.sugerir(imagenes, carta, nombreLugar.ifBlank { null })
                ideasDeTitulo = titulos
                // La foto elegida como portada pasa al primer lugar
                fotos.getOrNull(portada)?.let { mejor -> medios.remove(mejor); medios.add(0, mejor) }
            }.onFailure { error = nube.mensaje(it) }
            iaOcupada = null
        }
    }

    fun mejorarCarta(tono: String) {
        iaOcupada = "carta"; error = null
        viewModelScope.launch {
            runCatching { nube.mejorarCarta(carta, titulo, tono) }
                .onSuccess { cartaAnterior = carta; carta = it }
                .onFailure { error = nube.mensaje(it) }
            iaOcupada = null
        }
    }

    // ---------- Sellar ----------
    fun formulario() = FormularioCapsula(
        titulo = titulo, carta = carta, medios = medios.size, usarFecha = usarFecha, abreEl = abreEl,
        usarLugar = usarLugar, soloEnElLugar = soloEnElLugar, tieneUbicacion = lat != null,
        usarClave = usarClave, clave = clave, claveRepetida = clave2,
    )

    fun sellar() {
        error = ReglasCapsula.validar(formulario())
        if (error != null) return
        guardando = true
        viewModelScope.launch {
            runCatching {
                val lista = ReglasCapsula.correos(correos)
                nube.crear(ctx, Draft(
                    title = titulo, letter = carta, items = medios.toList(),
                    unlockAt = if (usarFecha) abreEl else null,
                    placeName = if (usarLugar) nombreLugar.ifBlank { direccion } else null,
                    lat = if (usarLugar) lat else null, lng = if (usarLugar) lng else null,
                    requirePlace = usarLugar && soloEnElLugar,
                    shareEmails = lista,
                    key = if (usarClave) clave else null,
                    voiceNotes = notasDeVoz.toList(),
                    songUrl = enlaceCancion, songTitle = cancion, template = plantilla,
                    questions = preguntas.toList(), radiusM = radio,
                    openTogether = abrirJuntos && lista.isNotEmpty(), shareContribute = colaborativa,
                    routeId = rutaId, routeOrder = rutaId?.let { runCatching { nube.siguienteEnRuta(it) }.getOrDefault(1) },
                )) { texto, p -> textoProgreso = texto; progreso = p }
            }.onSuccess { id -> runCatching { DraftStore.clear(ctx) }.onFailure { registrar("borrador", it) }; creada = id }
             .onFailure { error = nube.mensaje(it); guardando = false }
        }
    }

    // ---------- Borrador automático ----------
    private fun restaurar(d: SavedDraft) {
        titulo = d.title; carta = d.letter; plantilla = d.template
        medios.clear(); medios.addAll(d.items.map { PickedItem(Uri.parse(it.uri), it.kind) })
        notasDeVoz.clear(); notasDeVoz.addAll(d.voiceNotes.map { File(it) }.filter { it.exists() })
        usarFecha = d.useDate; abreEl = d.unlockAtEpoch?.let { Instant.ofEpochMilli(it) } ?: abreEl
        usarLugar = d.usePlace; soloEnElLugar = d.requirePlace; nombreLugar = d.placeName
        correos = d.emails; cancion = d.songTitle; enlaceCancion = d.songUrl
        preguntas.clear(); preguntas.addAll(d.questions)
        radio = d.radius; abrirJuntos = d.openTogether; colaborativa = d.contribute; rutaId = d.routeId
        borradorRecuperado = true
    }

    fun borrador() = SavedDraft(
        title = titulo, letter = carta, items = medios.map { SavedItem(it.uri.toString(), it.kind) },
        voiceNotes = notasDeVoz.map { it.absolutePath }, unlockAtEpoch = abreEl?.toEpochMilli(), useDate = usarFecha,
        usePlace = usarLugar, requirePlace = soloEnElLugar, placeName = nombreLugar, emails = correos, template = plantilla,
        songTitle = cancion, songUrl = enlaceCancion, questions = preguntas.toList(), radius = radio,
        openTogether = abrirJuntos, contribute = colaborativa, routeId = rutaId,
    )

    /** Cada cambio se guarda 300 ms después (no en cada letra). */
    private suspend fun guardarBorradorSolo() {
        snapshotFlow { borrador() }.collectLatest { d -> delay(300); DraftStore.save(ctx, d) }
    }

    override fun onCleared() {
        if (grabando) detenerGrabacion()
    }

    private fun registrar(donde: String, e: Throwable) = Log.w("NuevaCapsula", "Falló $donde", e)

    companion object {
        val Fabrica: ViewModelProvider.Factory = viewModelFactory {
            initializer { NuevaCapsulaViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!, ServicioSupabase) }
        }
    }
}
