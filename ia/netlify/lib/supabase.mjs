/* =========================================================
   TIME CAPSULE · IA · Conexión con Supabase
   Todo se hace CON EL TOKEN DE LA PERSONA que pide la IA: así se respetan
   todas las reglas de seguridad (RLS) y nadie lee cápsulas ajenas.
   La clave "publishable" es pública (es la misma que lleva la app).
   ========================================================= */
export const SB_URL = process.env.TC_SUPABASE_URL || 'https://frascwpukyygsaeeohnr.supabase.co';
export const SB_KEY = process.env.TC_SUPABASE_KEY || 'sb_publishable_HlD6zjfxofaUGFguDr5yNQ_7K5CVBPY';

/** Consultas por día y por persona (protege lo que cuesta la IA). */
export const LIMITE_DIARIO = Number(process.env.TC_IA_LIMITE || 40);

const cabeceras = (token, extra = {}) => ({ apikey: SB_KEY, authorization: `Bearer ${token}`, ...extra });

/** ¿Quién pide? Devuelve el usuario o null si el token no sirve. */
export async function usuario(token) {
  if (!token) return null;
  const r = await fetch(`${SB_URL}/auth/v1/user`, { headers: cabeceras(token) });
  return r.ok ? r.json() : null;
}

/** Lee la base de datos (REST) como la persona. */
export async function leer(ruta, token) {
  const r = await fetch(`${SB_URL}/rest/v1/${ruta}`, { headers: cabeceras(token) });
  if (!r.ok) throw new Error(`No se pudo leer la base de datos (${r.status})`);
  return r.json();
}

/** Escribe en la base de datos como la persona (solo lo que sus reglas permiten). */
export async function escribir(ruta, token, metodo, cuerpo) {
  const r = await fetch(`${SB_URL}/rest/v1/${ruta}`, {
    method: metodo, headers: cabeceras(token, { 'content-type': 'application/json', prefer: 'return=minimal' }), body: JSON.stringify(cuerpo)
  });
  return r.ok;
}

/** Descarga una foto guardada y la devuelve en base64 (para mostrársela a Claude). */
export async function fotoBase64(ruta, token) {
  const r = await fetch(`${SB_URL}/storage/v1/object/authenticated/media/${ruta.split('/').map(encodeURIComponent).join('/')}`, { headers: cabeceras(token) });
  if (!r.ok) return null;
  return Buffer.from(await r.arrayBuffer()).toString('base64');
}

/** ¿Cuántas consultas de IA lleva hoy? (tabla ai_uso, cada quien ve solo las suyas) */
export async function usoDeHoy(token) {
  const desde = new Date(Date.now() - 864e5).toISOString();
  const r = await fetch(`${SB_URL}/rest/v1/ai_uso?select=id&creado=gte.${encodeURIComponent(desde)}`, {
    headers: cabeceras(token, { prefer: 'count=exact', range: '0-0' })
  });
  if (!r.ok) throw new Error('No se pudo revisar el límite de IA');
  const total = Number((r.headers.get('content-range') || '').split('/')[1]);
  return Number.isFinite(total) ? total : 0;
}

export const registrarUso = (token, accion) => escribir('ai_uso', token, 'POST', { accion });
