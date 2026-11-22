/* =========================================================
   TIME CAPSULE · Función de IA (Netlify)
   POST /.netlify/functions/ia   { action, ... }   con  Authorization: Bearer <sesión>
   GET  /.netlify/functions/ia   → ¿está lista la IA?
   - Exige una sesión válida de Time Capsule (Supabase).
   - Máximo LIMITE_DIARIO consultas por persona cada 24 h.
   - La clave de Claude la pone Netlify (AI Gateway): no hay claves en el código.
   ========================================================= */
import { getUser, usageToday, recordUsage, DAILY_LIMIT } from '../lib/supabase.mjs';
import { hasApiKey, MODEL, toFriendlyError } from '../lib/claude.mjs';
import { ACTIONS, runAction } from '../lib/acciones.mjs';

export const json = (status, body) =>
  new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store' } });

export default async (req) => {
  if (req.method === 'GET') return json(200, { configurada: hasApiKey(), modelo: MODEL(), limite_diario: DAILY_LIMIT });
  if (req.method !== 'POST') return json(405, { error: 'Método no permitido' });
  if (!hasApiKey()) return json(503, { error: 'La IA aún no está activada en el servidor.' });

  const token = (req.headers.get('authorization') || '').replace(/^Bearer\s+/i, '');
  const caller = await getUser(token);
  if (!caller) return json(401, { error: 'Inicia sesión de nuevo para usar la IA.' });

  let requestBody;
  try { requestBody = await req.json(); } catch { return json(400, { error: 'Solicitud inválida' }); }
  const actionName = requestBody?.action;
  if (!ACTIONS.includes(actionName)) return json(400, { error: 'Acción no válida' });

  try {
    if (await usageToday(token) >= DAILY_LIMIT) return json(429, { error: `Llegaste al límite de ${DAILY_LIMIT} usos de IA por hoy. Vuelve mañana.` });
    const [status, reply] = await runAction(actionName, requestBody, caller, token);
    if (status === 200) await recordUsage(token, actionName);
    return json(status, reply);
  } catch (e) {
    const [status, messageText] = toFriendlyError(e);
    console.error('IA', actionName, e?.message);
    return json(status, { error: messageText });
  }
};
