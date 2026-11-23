package com.timecapsule.app

import com.timecapsule.app.data.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Seguridad: base de datos con RLS y ninguna clave secreta en el código. */
class SecurityTest {
    // Las pruebas corren dentro de la carpeta "app"; la base de datos está en ../supabase
    private val root = File("..").canonicalFile
    private val sql = File(root, "supabase").listFiles { f -> f.name.endsWith(".sql") }!!.sortedBy { it.name }
    private val everything = sql.joinToString("\n") { it.readText() }
    private val tables = Regex("""create table (?:if not exists )?(?:public\.)?"?([a-z_]+)""", RegexOption.IGNORE_CASE)
        .findAll(everything).map { it.groupValues[1].lowercase() }.toSet()

    @Test fun `hay scripts de base de datos`() = assertTrue(sql.size >= 3 && tables.size >= 10)

    @Test fun `todas las tablas tienen seguridad por filas (RLS)`() {
        val withoutRls = tables.filterNot { Regex("""alter table (?:public\.)?"?$it"?\s+enable row level security""", RegexOption.IGNORE_CASE).containsMatchIn(everything) }
        assertEquals("tablas sin RLS", emptyList<String>(), withoutRls)
    }

    /* Tabla interna: RLS activo y SIN políticas = nadie la lee desde la app; solo la usa la función de la clave. */
    private val privateOnes = setOf("key_attempts")

    @Test fun `la tabla privada de intentos de clave solo se usa desde funciones`() {
        assertTrue("key_attempts" in tables)
        val function = Regex("""create (?:or replace )?function[\s\S]*?\$\$;""", RegexOption.IGNORE_CASE).findAll(everything)
            .map { it.value }.filter { "key_attempts" in it }.toList()
        assertTrue("ninguna función usa key_attempts", function.isNotEmpty())
        assertTrue(function.all { "security definer" in it.lowercase() })
    }

    @Test fun `cada tabla publica tiene politicas de acceso`() {
        val withoutPolicy = (tables - privateOnes).filterNot { Regex("""create policy [\s\S]{0,200}? on (?:public\.)?"?$it"?\b""", RegexOption.IGNORE_CASE).containsMatchIn(everything) }
        assertEquals("tablas sin políticas", emptyList<String>(), withoutPolicy)
    }

    @Test fun `las funciones con privilegios fijan el search_path`() {
        val functions = Regex("""create (?:or replace )?function[\s\S]*?\$\$;""", RegexOption.IGNORE_CASE).findAll(everything).map { it.value }
        val unsafe = functions.filter { "security definer" in it.lowercase() && "search_path" !in it.lowercase() }
            .map { Regex("""function\s+([\w.]+)""").find(it)!!.groupValues[1] }.toList()
        assertEquals(emptyList<String>(), unsafe)
    }

    @Test fun `la app usa la clave publishable de Supabase (nunca service_role)`() {
        assertTrue(Config.SUPABASE_KEY.startsWith("sb_publishable_"))
        assertTrue(Config.SUPABASE_URL.startsWith("https://") && Config.SUPABASE_URL.endsWith(".supabase.co"))
    }

    @Test fun `ninguna clave secreta esta escrita en el codigo`() {
        val files = File(root, "app/src/main").walkTopDown().filter { it.isFile && it.extension in setOf("kt", "xml", "json") } +
            File(root, "supabase").walkTopDown().filter { it.isFile }
        val secret = Regex("""sk-ant-[a-zA-Z0-9]|sb_secret_[A-Za-z0-9]|eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}""")
        val bad = files.filter { secret.containsMatchIn(it.readText()) }.map { it.relativeTo(root).path }.toList()
        assertEquals(emptyList<String>(), bad)
    }

    @Test fun `la IA de recuerdos lee la clave de Anthropic de los secretos de Supabase`() {
        val ai = File(root, "supabase/functions/ai-memories/index.ts").readText()
        assertTrue(ai.contains("""Deno.env.get("ANTHROPIC_API_KEY")"""))
    }

    @Test fun `las claves de capsula se guardan como huella bcrypt de costo 12`() {
        // La última versión de cada función es la que queda en la base de datos
        val last = sql.last { it.readText().contains("function public.set_capsule_key") }.readText()
        assertTrue(last.contains("gen_salt('bf', 12)"))
        assertTrue("las huellas viejas se mejoran al abrir", last.contains("substr(h, 5, 2)::int < 12"))
        assertTrue("nunca se guarda la clave en texto", !Regex("""set\s+key\s*=""").containsMatchIn(everything))
    }
}
