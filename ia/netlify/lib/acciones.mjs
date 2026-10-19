/* =========================================================
   TIME CAPSULE · IA · Las 6 acciones
   summary · timeline · suggest · letter · year_ago · album
   Los textos para Claude (prompts) están aquí; las funciones "armar…"
   no llaman a nadie, así que se pueden probar.
   ========================================================= */
import { leer, escribir, fotoBase64 } from './supabase.mjs';
import { preguntar, extraerJson, imagen } from './claude.mjs';

export const ACCIONES = ['summary', 'timeline', 'suggest', 'letter', 'year_ago', 'album'];
const corto = (t, n) => String(t ?? '').slice(0, n);

/* ---------------- Textos para Claude ---------------- */
export const promptResumen = (c, fotos, videos) => `Eres el narrador cálido de una app de cápsulas del tiempo.
Esta cápsula se llama "${c.title}", se guardó el ${c.created_at}${c.place_name ? ` en ${c.place_name}` : ''}.
Tiene ${fotos} fotos (adjuntas) y ${videos} videos.
Carta que escribió la persona: """${corto(c.letter, 3000)}"""
Escribe en español un resumen emotivo de 3 a 5 frases, en segunda persona ("Ese día…", "Recuerdas…"),
mencionando detalles concretos que se ven en las fotos y el sentido de la carta.
No inventes nombres ni hechos que no aparezcan. Sin títulos, listas ni markdown.`;

/** Lo que se le cuenta a Claude de cada cápsula. Las CERRADAS no envían su contenido. */
export const itemsDeLinea = (caps) => caps.map((c) => ({
  id: c.id, titulo: c.title, creada: c.created_at, se_abre: c.unlock_at,
  lugar: c.place_name, lat: c.lat, lng: c.lng, abierta: c.is_open, recuerdos: c.media_count, de: c.owner_name,
  texto: c.is_open ? (c.ai_summary || corto(c.letter, 500)) : null
}));

export const promptLinea = (items) => `Eres el narrador de una app de cápsulas del tiempo. Con estas cápsulas, crea una línea de tiempo de la vida del usuario.
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

export const promptSugerir = (n, lugar, carta) => `Ayudas a crear cápsulas del tiempo. Con ${n} fotos (numeradas desde 0 en el orden adjunto)${lugar ? `, tomadas en ${lugar}` : ''} y esta carta: """${corto(carta, 1500)}"""
Propón 3 títulos cortos (máx. 6 palabras), cálidos y en español, y elige la foto más bonita para portada.
Responde SOLO JSON: {"titulos":["","",""],"portada":0}`;

export const promptCarta = (carta, titulo, tono) => `Ayudas a escribir cartas para cápsulas del tiempo, en español de Colombia.
Mejora y completa esta carta con un tono ${tono}. Conserva TODO lo que la persona escribió (nombres, hechos y su forma de hablar); solo ordena, corrige y agrega 1 o 2 frases que lo hagan más emotivo. No inventes hechos.
Si la carta está casi vacía, escribe un borrador corto para que la persona lo complete con [espacios entre corchetes].
Devuelve SOLO la carta, sin explicaciones.
Carta: """${corto(carta, 4000)}"""
Título de la cápsula: ${corto(titulo, 200)}`;

export const promptHaceUnAno = (c) => `Escribe en español una notificación breve (máx. 2 frases, segunda persona, cálida) para recordar que hace un año la persona guardó la cápsula "${c.title}"${c.place_name ? ` en ${c.place_name}` : ''}. Contexto: ${corto(c.ai_summary || c.letter, 600)}. Sin comillas ni emojis al inicio.`;

export const promptAlbum = (c, n) => `Vas a narrar un álbum animado de la cápsula "${c.title}" (${c.place_name ?? 'sin lugar'}). Hay ${n} fotos en orden.
Escribe en español, cálido y concreto, sin inventar nombres:
Responde SOLO JSON: {"intro":"1 frase de apertura","fotos":["1 frase corta por foto, en el mismo orden"],"cierre":"1 frase final"}`;

/** Ventana de "hace un año": entre 380 y 350 días atrás. */
export function ventanaHaceUnAno(ahora = Date.now()) {
  return { desde: new Date(ahora - 380 * 864e5).toISOString(), hasta: new Date(ahora - 350 * 864e5).toISOString() };
}

/** Tono permitido para la carta (si llega otra cosa, se usa "cálido"). */
export const tonoValido = (t) => (['cálido', 'divertido', 'nostálgico', 'formal', 'poético'].includes(t) ? t : 'cálido');

/* ---------------- Las acciones ---------------- */
async function capsula(id, token) {
  const [c] = await leer(`capsules_feed?select=*&id=eq.${encodeURIComponent(id)}&limit=1`, token);
  return c ?? null;
}

async function fotosDe(id, token, max) {
  const media = await leer(`capsule_media?select=path,kind&capsule_id=eq.${encodeURIComponent(id)}&order=created_at`, token);
  const fotos = media.filter((m) => m.kind === 'photo').slice(0, max);
  const bloques = [];
  for (const f of fotos) { const b = await fotoBase64(f.path, token); if (b) bloques.push(imagen(b)); }
  return { media, fotos, bloques };
}

export async function ejecutar(accion, cuerpo, usuario, token) {
  switch (accion) {
    case 'summary': {
      const c = await capsula(cuerpo.capsule_id, token);
      if (!c) return [404, { error: 'No se encontró la cápsula' }];
      if (!c.is_open) return [403, { error: 'La cápsula todavía está cerrada' }];
      const { media, bloques } = await fotosDe(c.id, token, 4);
      const videos = media.filter((m) => m.kind === 'video').length;
      const summary = (await preguntar([...bloques, { type: 'text', text: promptResumen(c, bloques.length, videos) }], 600)).trim();
      if (c.owner === usuario.id) await escribir(`capsules?id=eq.${encodeURIComponent(c.id)}`, token, 'PATCH', { ai_summary: summary });
      return [200, { summary }];
    }
    case 'timeline': {
      const caps = await leer('capsules_feed?select=id,title,letter,created_at,unlock_at,place_name,lat,lng,is_open,ai_summary,media_count,owner_name&order=created_at', token);
      if (!caps.length) return [200, { resumen: 'Aún no tienes cápsulas.', capitulos: [], lugares: [] }];
      return [200, extraerJson(await preguntar(promptLinea(itemsDeLinea(caps)), 3000))];
    }
    case 'suggest': {
      const imgs = (Array.isArray(cuerpo.images) ? cuerpo.images : []).filter((x) => typeof x === 'string' && x.length < 1_500_000).slice(0, 3);
      const contenido = [...imgs.map(imagen), { type: 'text', text: promptSugerir(imgs.length, cuerpo.place, cuerpo.letter) }];
      return [200, extraerJson(await preguntar(contenido, 400))];
    }
    case 'letter': {
      const carta = await preguntar(promptCarta(cuerpo.letter, cuerpo.title, tonoValido(cuerpo.tono)), 1200);
      return [200, { carta: carta.trim() }];
    }
    case 'year_ago': {
      const { desde, hasta } = ventanaHaceUnAno();
      const caps = await leer(`capsules_feed?select=id,title,letter,ai_summary,place_name,created_at,is_open&created_at=gte.${encodeURIComponent(desde)}&created_at=lte.${encodeURIComponent(hasta)}&is_open=eq.true&limit=3`, token);
      if (!caps.length) return [200, { mensaje: null }];
      const c = caps[0];
      return [200, { mensaje: (await preguntar(promptHaceUnAno(c), 200)).trim(), capsule_id: c.id, titulo: c.title }];
    }
    case 'album': {
      const c = await capsula(cuerpo.capsule_id, token);
      if (!c?.is_open) return [403, { error: 'La cápsula todavía está cerrada' }];
      const { fotos, bloques } = await fotosDe(c.id, token, 8);
      const out = extraerJson(await preguntar([...bloques, { type: 'text', text: promptAlbum(c, bloques.length) }], 1200));
      return [200, { ...out, paths: fotos.map((f) => f.path) }];
    }
    default:
      return [400, { error: 'Acción no válida' }];
  }
}
