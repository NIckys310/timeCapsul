package com.timecapsule.app

import com.timecapsule.app.data.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Seguridad: base de datos con RLS y ninguna clave secreta en el código. */
class SeguridadTest {
    // Las pruebas corren dentro de la carpeta "app"; la base de datos está en ../supabase
    private val raiz = File("..").canonicalFile
    private val sql = File(raiz, "supabase").listFiles { f -> f.name.endsWith(".sql") }!!.sortedBy { it.name }
    private val todo = sql.joinToString("\n") { it.readText() }
    private val tablas = Regex("""create table (?:if not exists )?(?:public\.)?"?([a-z_]+)""", RegexOption.IGNORE_CASE)
        .findAll(todo).map { it.groupValues[1].lowercase() }.toSet()

    @Test fun `hay scripts de base de datos`() = assertTrue(sql.size >= 3 && tablas.size >= 10)

    @Test fun `todas las tablas tienen seguridad por filas (RLS)`() {
        val sinRls = tablas.filterNot { Regex("""alter table (?:public\.)?"?$it"?\s+enable row level security""", RegexOption.IGNORE_CASE).containsMatchIn(todo) }
        assertEquals("tablas sin RLS", emptyList<String>(), sinRls)
    }

    /* Tabla interna: RLS activo y SIN políticas = nadie la lee desde la app; solo la usa la función de la clave. */
    private val privadas = setOf("key_attempts")

    @Test fun `la tabla privada de intentos de clave solo se usa desde funciones`() {
        assertTrue("key_attempts" in tablas)
        val funcion = Regex("""create (?:or replace )?function[\s\S]*?\$\$;""", RegexOption.IGNORE_CASE).findAll(todo)
            .map { it.value }.filter { "key_attempts" in it }.toList()
        assertTrue("ninguna función usa key_attempts", funcion.isNotEmpty())
        assertTrue(funcion.all { "security definer" in it.lowercase() })
    }

    @Test fun `cada tabla publica tiene politicas de acceso`() {
        val sinPolitica = (tablas - privadas).filterNot { Regex("""create policy [\s\S]{0,200}? on (?:public\.)?"?$it"?\b""", RegexOption.IGNORE_CASE).containsMatchIn(todo) }
        assertEquals("tablas sin políticas", emptyList<String>(), sinPolitica)
    }

    @Test fun `las funciones con privilegios fijan el search_path`() {
        val funciones = Regex("""create (?:or replace )?function[\s\S]*?\$\$;""", RegexOption.IGNORE_CASE).findAll(todo).map { it.value }
        val inseguras = funciones.filter { "security definer" in it.lowercase() && "search_path" !in it.lowercase() }
            .map { Regex("""function\s+([\w.]+)""").find(it)!!.groupValues[1] }.toList()
        assertEquals(emptyList<String>(), inseguras)
    }

    @Test fun `la app usa la clave publishable de Supabase (nunca service_role)`() {
        assertTrue(Config.SUPABASE_KEY.startsWith("sb_publishable_"))
        assertTrue(Config.SUPABASE_URL.startsWith("https://") && Config.SUPABASE_URL.endsWith(".supabase.co"))
    }

    @Test fun `ninguna clave secreta esta escrita en el codigo`() {
        val archivos = File(raiz, "app/src/main").walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml", "json") } +
            File(raiz, "supabase").walkTopDown().filter { it.isFile }
        val secreto = Regex("""sk-ant-[a-zA-Z0-9]|sb_secret_[A-Za-z0-9]|eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}""")
        val malos = archivos.filter { secreto.containsMatchIn(it.readText()) }.map { it.relativeTo(raiz).path }.toList()
        assertEquals(emptyList<String>(), malos)
    }

    @Test fun `la IA de recuerdos lee la clave de Anthropic de los secretos de Supabase`() {
        val ia = File(raiz, "supabase/functions/ai-memories/index.ts").readText()
        assertTrue(ia.contains("""Deno.env.get("ANTHROPIC_API_KEY")"""))
    }
}
