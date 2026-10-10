package com.timecapsule.app.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/** Cliente único de Supabase para toda la app. */
val supabase by lazy {
    createSupabaseClient(
        supabaseUrl = Config.SUPABASE_URL,
        supabaseKey = Config.SUPABASE_KEY,
    ) {
        install(Auth)
        install(Postgrest)
        install(Storage)
        install(Functions)
    }
}
