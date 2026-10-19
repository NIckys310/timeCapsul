package com.timecapsule.app.data

/**
 * Datos de tu proyecto de Supabase
 * Supabase → Project Settings → API Keys
 */
object Config {
    const val SUPABASE_URL = "https://frascwpukyygsaeeohnr.supabase.co"
    const val SUPABASE_KEY = "sb_publishable_HlD6zjfxofaUGFguDr5yNQ_7K5CVBPY"

    /** Funciones de IA (Netlify): la clave de Claude la pone Netlify, no va en la app. */
    const val IA_URL = "https://timecapsule-ia.netlify.app/.netlify/functions/ia"
}
