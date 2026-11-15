package com.timecapsule.app.data

/**
 * CONTRASEÑAS SEGURAS (las mismas reglas de Guardián)
 * La app nunca guarda la contraseña: se la entrega a Supabase, que la guarda
 * cifrada con bcrypt (un "hash" que no se puede revertir). Estas reglas solo
 * se piden al crear la cuenta, así quien ya tiene una puede seguir entrando.
 */
object ReglasContrasena {
    data class Regla(val texto: String, val cumple: (String) -> Boolean)

    val reglas = listOf(
        Regla("Al menos 10 caracteres") { it.length >= 10 },
        Regla("Una letra minúscula") { p -> p.any { it.isLowerCase() } },
        Regla("Una letra mayúscula") { p -> p.any { it.isUpperCase() } },
        Regla("Un número") { p -> p.any { it.isDigit() } },
        Regla("Un símbolo (!@#$…)") { p -> p.any { !it.isLetterOrDigit() } },
    )

    private val comunes = Regex("^(123|abc|qwe|password|contraseña)", RegexOption.IGNORE_CASE)

    /** Las reglas que todavía faltan (vacía = la contraseña sirve). */
    fun faltan(p: String): List<String> = buildList {
        reglas.filterNot { it.cumple(p) }.forEach { add(it.texto) }
        if (comunes.containsMatchIn(p)) add("No empieces con algo fácil de adivinar (123, abc, password…)")
    }

    fun esSegura(p: String) = faltan(p).isEmpty()
}
