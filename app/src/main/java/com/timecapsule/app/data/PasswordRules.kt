package com.timecapsule.app.data

/**
 * CONTRASEÑAS SEGURAS (las mismas reglas de Guardián)
 * La app nunca guarda la contraseña: se la entrega a Supabase, que la guarda
 * cifrada con bcrypt (un "hash" que no se puede revertir). Estas reglas solo
 * se piden al crear la cuenta, así quien ya tiene una puede seguir entrando.
 */
object PasswordRules {
    data class Requirement(val text: String, val meets: (String) -> Boolean)

    val rules = listOf(
        Requirement("Al menos 10 caracteres") { it.length >= 10 },
        Requirement("Una letra minúscula") { p -> p.any { it.isLowerCase() } },
        Requirement("Una letra mayúscula") { p -> p.any { it.isUpperCase() } },
        Requirement("Un número") { p -> p.any { it.isDigit() } },
        Requirement("Un símbolo (!@#$…)") { p -> p.any { !it.isLetterOrDigit() } },
    )

    private val common = Regex("^(123|abc|qwe|password|contraseña)", RegexOption.IGNORE_CASE)

    /** Las reglas que todavía faltan (vacía = la contraseña sirve). */
    fun missingItems(p: String): List<String> = buildList {
        rules.filterNot { it.meets(p) }.forEach { add(it.text) }
        if (common.containsMatchIn(p)) add("No empieces con algo fácil de adivinar (123, abc, password…)")
    }

    fun isSafe(p: String) = missingItems(p).isEmpty()
}
