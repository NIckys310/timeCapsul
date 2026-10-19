package com.timecapsule.app.notify

import com.timecapsule.app.data.Activity
import com.timecapsule.app.data.Capsule
import java.time.Duration

/* =========================================================
   TEXTOS Y REGLAS DE LOS AVISOS (sin Android: se pueden probar)
   Los Workers y Sync los usan para mostrar las notificaciones.
   ========================================================= */
object Avisos {
    /** Cuenta regresiva: se avisa 1 semana, 1 día y 1 hora antes de que se abra. */
    val CUENTA_REGRESIVA = listOf(Duration.ofDays(7) to "1 semana", Duration.ofDays(1) to "1 día", Duration.ofHours(1) to "1 hora")

    /** Cuánto esperar para cada aviso de cuenta regresiva (los que ya pasaron no se programan). */
    fun recordatorios(faltan: Duration): List<Pair<Duration, String>> =
        if (faltan.isNegative) emptyList()
        else CUENTA_REGRESIVA.mapNotNull { (antes, texto) -> faltan.minus(antes).takeUnless { it.isNegative }?.let { it to texto } }

    /** El lugar al que hay que ir, si la cápsula solo se abre allá. */
    fun lugarParaAbrir(c: Capsule): String? = if (c.requirePlace) c.placeName ?: "su lugar" else null

    fun tituloApertura(titulo: String) = "¡“$titulo” se puede abrir!"

    fun textoApertura(lugar: String?) =
        if (lugar != null) "Ya llegó la fecha. Ve a $lugar para abrirla." else "Ya puedes abrirla y revivir lo que guardaste."

    fun tituloRecordatorio(cuanto: String) = "⏳ Falta $cuanto"
    fun textoRecordatorio(titulo: String) = "Tu cápsula “$titulo” se abrirá pronto."

    /** Cápsulas que alguien te acaba de compartir (no las conocías y no son tuyas). */
    fun nuevasCompartidas(lista: List<Capsule>, conocidas: Set<String>, yo: String?): List<Capsule> =
        lista.filter { it.id !in conocidas && it.owner != yo }

    fun tituloCompartida(c: Capsule) = "${c.ownerName ?: "Alguien"} compartió una cápsula contigo"
    fun textoCompartida(c: Capsule) = "“${c.title}”. ${if (c.isOpen) "Ya puedes verla." else "Te avisaremos cuando se abra."}"

    /** "Ana comentó: ¡qué bonito!" o "Ana reaccionó ❤️" */
    fun textoActividad(a: Activity) =
        if (a.kind == "comment") "${a.who ?: "Alguien"} comentó: ${a.body}" else "${a.who ?: "Alguien"} reaccionó ${a.body}"

    /** Máximo 5 avisos de actividad por revisión, para no llenar el celular. */
    const val MAX_ACTIVIDAD = 5
}
