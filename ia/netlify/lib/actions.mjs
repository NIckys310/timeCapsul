/* =========================================================
   TIME CAPSULE · IA · Las 6 acciones
   summary · timeline · suggest · letter · year_ago · album
   Los textos para Claude (prompts) están aquí; las funciones "armar…"
   no llaman a nadie, así que se pueden probar.
   ========================================================= */
import { readRows, writeRows, photoBase64 } from './supabase.mjs';
import { askClaude, extractJson, imageBlock, userData } from './claude.mjs';

export const ACTIONS = ['summary', 'timeline', 'suggest', 'letter', 'year_ago', 'album'];
const truncate = (t, n) => String(t ?? '').slice(0, n);

/* ---------------- Textos para Claude ---------------- */
export const promptSummary = (c, photos, videos) => `Eres el narrador cálido de una app de cápsulas del tiempo.
Esta cápsula se llama "${c.title}", se guardó el ${c.created_at}${c.place_name ? ` en ${c.place_name}` : ''}.
Tiene ${photos} fotos (adjuntas) y ${videos} videos.
Carta que escribió la persona: ${userData(c.letter, 3000)}
Escribe en español un resumen emotivo de 3 a 5 frases, en segunda persona ("Ese día…", "Recuerdas…"),
mencionando detalles concretos que se ven en las fotos y el sentido de la carta.
No inventes nombres ni hechos que no aparezcan. Sin títulos, listas ni markdown.`;

/** Lo que se le cuenta a Claude de cada cápsula. Las CERRADAS no envían su contenido. */
export const timelineItems = (caps) => caps.map((c) => ({
  id: c.id, titulo: c.title, creada: c.created_at, se_abre: c.unlock_at,
  lugar: c.place_name, lat: c.lat, lng: c.lng, abierta: c.is_open, recuerdos: c.media_count, de: c.owner_name,
  texto: c.is_open ? (c.ai_summary || truncate(c.letter, 500)) : null
}));

export const promptTimeline = (items) => `Eres el narrador de una app de cápsulas del tiempo. Con estas cápsulas, crea una línea de tiempo de la vida del usuario.
Reglas:
- Agrupa las cápsulas en 2 a 8 capítulos cronológicos con un título evocador (máx. 6 palabras).
- Cada cápsula debe estar en exactamente un capítulo (usa su "id").
- Para cápsulas cerradas (texto null) NO inventes su contenido: solo di que algo espera ser abierto.
- "lugares": una nota breve por cada lugar distinto con lat/lng, resumiendo lo vivido allí.
- Español cálido y concreto, sin inventar nombres.
Responde SOLO con JSON válido con esta forma:
{"resumen":"2 frases sobre la historia completa",
 "capitulos":[{"titulo":"","periodo":"ej. Mar – Jun 2026","descripcion":"1-2 frases","emoji":"un emoji","capsulas":["id"]}],
 "lugares":[{"nombre":"","resumen":"1 frase","lat":0,"lng":0}]}
Cápsulas: ${JSON.stringify(items)}`;

export const promptSuggest = (n, place, letter) => `Ayudas a crear cápsulas del tiempo. Con ${n} fotos (numeradas desde 0 en el orden adjunto)${place ? `, tomadas en ${place}` : ''} y esta carta: ${userData(letter, 1500)}
Propón 3 títulos cortos (máx. 6 palabras), cálidos y en español, y elige la foto más bonita para portada.
Responde SOLO JSON: {"titulos":["","",""],"portada":0}`;

export const promptLetter = (letter, titleText, tone) => `Ayudas a escribir cartas para cápsulas del tiempo, en español de Colombia.
Mejora y completa esta carta con un tono ${tone}. Conserva TODO lo que la persona escribió (nombres, hechos y su forma de hablar); solo ordena, corrige y agrega 1 o 2 frases que lo hagan más emotivo. No inventes hechos.
Si la carta está casi vacía, escribe un borrador corto para que la persona lo complete con [espacios entre corchetes].
Devuelve SOLO la carta, sin explicaciones.
Carta: ${userData(letter, 4000)}
Título de la cápsula: ${userData(titleText, 200)}`;

export const promptYearAgo = (c) => `Escribe en español una notificación breve (máx. 2 frases, segunda persona, cálida) para recordar que hace un año la persona guardó la cápsula "${c.title}"${c.place_name ? ` en ${c.place_name}` : ''}. Contexto: ${truncate(c.ai_summary || c.letter, 600)}. Sin comillas ni emojis al inicio.`;

export const promptAlbum = (c, n) => `Vas a narrar un álbum animado de la cápsula "${c.title}" (${c.place_name ?? 'sin lugar'}). Hay ${n} fotos en orden.
Escribe en español, cálido y concreto, sin inventar nombres:
Responde SOLO JSON: {"intro":"1 frase de apertura","fotos":["1 frase corta por foto, en el mismo orden"],"cierre":"1 frase final"}`;

/** Ventana de "hace un año": entre 380 y 350 días atrás. */
export function yearAgoWindow(now = Date.now()) {
  return { desde: new Date(now - 380 * 864e5).toISOString(), hasta: new Date(now - 350 * 864e5).toISOString() };
}

/** Tono permitido para la carta (si llega otra cosa, se usa "cálido"). */
export const validTone = (t) => (['cálido', 'divertido', 'nostálgico', 'formal', 'poético'].includes(t) ? t : 'cálido');

/* ---------------- Las acciones ---------------- */
async function capsule(id, token) {
  const [c] = await readRows(`capsules_feed?select=*&id=eq.${encodeURIComponent(id)}&limit=1`, token);
  return c ?? null;
}

async function photosOf(id, token, max) {
  const media = await readRows(`capsule_media?select=path,kind&capsule_id=eq.${encodeURIComponent(id)}&order=created_at`, token);
  const photos = media.filter((m) => m.kind === 'photo').slice(0, max);
  const blocks = [];
  for (const f of photos) { const b = await photoBase64(f.path, token); if (b) blocks.push(imageBlock(b)); }
  return { media, fotos: photos, bloques: blocks };
}

/** Si la app está en inglés, se le pide a Claude que responda en inglés. */
export function withLanguage(messageContent, language) {
  if (language !== 'en') return messageContent;
  const note = '\n\nIMPORTANTE: la persona usa la app en inglés. Escribe en inglés todos los textos de tu respuesta (conserva las claves del JSON).';
  if (typeof messageContent === 'string') return messageContent + note;
  return messageContent.map((b, i) => (i === messageContent.length - 1 && b.type === 'text' ? { ...b, text: b.text + note } : b));
}

export async function runAction(actionName, requestBody, getUser, token) {
  const ask = (messageContent, max) => askClaude(withLanguage(messageContent, requestBody.idioma), max);
  switch (actionName) {
    case 'summary': {
      const c = await capsule(requestBody.capsule_id, token);
      if (!c) return [404, { error: 'No se encontró la cápsula' }];
      if (!c.is_open) return [403, { error: 'La cápsula todavía está cerrada' }];
      const { media, bloques: blocks } = await photosOf(c.id, token, 4);
      const videos = media.filter((m) => m.kind === 'video').length;
      const summary = (await ask([...blocks, { type: 'text', text: promptSummary(c, blocks.length, videos) }], 600)).trim();
      if (c.owner === getUser.id) await writeRows(`capsules?id=eq.${encodeURIComponent(c.id)}`, token, 'PATCH', { ai_summary: summary });
      return [200, { summary }];
    }
    case 'timeline': {
      const caps = await readRows('capsules_feed?select=id,title,letter,created_at,unlock_at,place_name,lat,lng,is_open,ai_summary,media_count,owner_name&order=created_at', token);
      if (!caps.length) return [200, { resumen: 'Aún no tienes cápsulas.', capitulos: [], lugares: [] }];
      return [200, extractJson(await ask(promptTimeline(timelineItems(caps)), 3000))];
    }
    case 'suggest': {
      const imgs = (Array.isArray(requestBody.images) ? requestBody.images : []).filter((x) => typeof x === 'string' && x.length < 1_500_000).slice(0, 3);
      const messageContent = [...imgs.map(imageBlock), { type: 'text', text: promptSuggest(imgs.length, requestBody.place, requestBody.letter) }];
      return [200, extractJson(await ask(messageContent, 400))];
    }
    case 'letter': {
      const letter = await ask(promptLetter(requestBody.letter, requestBody.title, validTone(requestBody.tono)), 1200);
      return [200, { carta: letter.trim() }];
    }
    case 'year_ago': {
      const { desde: fromDate, hasta: untilDate } = yearAgoWindow();
      const caps = await readRows(`capsules_feed?select=id,title,letter,ai_summary,place_name,created_at,is_open&created_at=gte.${encodeURIComponent(fromDate)}&created_at=lte.${encodeURIComponent(untilDate)}&is_open=eq.true&limit=3`, token);
      if (!caps.length) return [200, { mensaje: null }];
      const c = caps[0];
      return [200, { mensaje: (await ask(promptYearAgo(c), 200)).trim(), capsule_id: c.id, titulo: c.title }];
    }
    case 'album': {
      const c = await capsule(requestBody.capsule_id, token);
      if (!c?.is_open) return [403, { error: 'La cápsula todavía está cerrada' }];
      const { fotos: photos, bloques: blocks } = await photosOf(c.id, token, 8);
      const out = extractJson(await ask([...blocks, { type: 'text', text: promptAlbum(c, blocks.length) }], 1200));
      return [200, { ...out, paths: photos.map((f) => f.path) }];
    }
    default:
      return [400, { error: 'Acción no válida' }];
  }
}
