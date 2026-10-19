/* =========================================================
   TIME CAPSULE · Función de IA (Netlify)
   POST /.netlify/functions/ia   { action, ... }   con  Authorization: Bearer <sesión>
   GET  /.netlify/functions/ia   → ¿está lista la IA?
   - Exige una sesión válida de Time Capsule (Supabase).
   - Máximo LIMITE_DIARIO consultas por persona cada 24 h.
   - La clave de Claude la pone Netlify (AI Gateway): no hay claves en el código.
   ========================================================= */
import { usuario, usoDeHoy, registrarUso, LIMITE_DIARIO } from '../lib/supabase.mjs';
import { hayClave, MODELO, errorAmable } from '../lib/claude.mjs';
import { ACCIONES, ejecutar } from '../lib/acciones.mjs';

export const json = (status, body) =>
  new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' } });

export default async (req) => {
  if (req.method === 'GET') return json(200, { configurada: hayClave(), modelo: MODELO(), limite_diario: LIMITE_DIARIO });
  if (req.method !== 'POST') return json(405, { error: 'Método no permitido' });
  if (!hayClave()) return json(503, { error: 'La IA aún no está activada en el servidor.' });

  const token = (req.headers.get('authorization') || '').replace(/^Bearer\s+/i, '');
  const quien = await usuario(token);
  if (!quien) return json(401, { error: 'Inicia sesión de nuevo para usar la IA.' });

  let cuerpo;
  try { cuerpo = await req.json(); } catch { return json(400, { error: 'Solicitud inválida' }); }
  const accion = cuerpo?.action;
  if (!ACCIONES.includes(accion)) return json(400, { error: 'Acción no válida' });

  try {
    if (await usoDeHoy(token) >= LIMITE_DIARIO) return json(429, { error: `Llegaste al límite de ${LIMITE_DIARIO} usos de IA por hoy. Vuelve mañana.` });
    const [status, respuesta] = await ejecutar(accion, cuerpo, quien, token);
    if (status === 200) await registrarUso(token, accion);
    return json(status, respuesta);
  } catch (e) {
    const [status, mensaje] = errorAmable(e);
    console.error('IA', accion, e?.message);
    return json(status, { error: mensaje });
  }
};
