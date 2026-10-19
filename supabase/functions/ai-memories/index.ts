// =====================================================================
//  TIME CAPSULE · Función de IA (Supabase Edge Function "ai-memories")
//  ⚠ YA NO SE USA: la IA ahora está en Netlify (carpeta ia/), sin claves secretas.
//    Se deja como referencia.
//  - action "summary":  resume una cápsula abierta mirando sus fotos
//  - action "timeline": crea capítulos de vida y notas por lugar
//  - action "suggest":  sugiere títulos y la mejor portada (fotos aún sin subir)
//  - action "letter":   ayuda a escribir o mejorar la carta
//  - action "year_ago": recuerdo de "hace un año"
//  - action "album":    textos para el álbum animado de una cápsula abierta
//  Necesita el secreto ANTHROPIC_API_KEY (Edge Functions → Secrets).
// =====================================================================
import { createClient } from "jsr:@supabase/supabase-js@2";
import { encodeBase64 } from "jsr:@std/encoding@1/base64";

const ANTHROPIC_KEY = Deno.env.get("ANTHROPIC_API_KEY") ?? "";
const MODEL = Deno.env.get("ANTHROPIC_MODEL") ?? "claude-sonnet-5-5";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });

async function askClaude(content: unknown, maxTokens = 1500): Promise<string> {
  if (!ANTHROPIC_KEY) throw new Error("Falta configurar ANTHROPIC_API_KEY en Supabase");
  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: { "x-api-key": ANTHROPIC_KEY, "anthropic-version": "2023-06-01", "content-type": "application/json" },
    body: JSON.stringify({ model: MODEL, max_tokens: maxTokens, messages: [{ role: "user", content }] }),
  });
  if (!res.ok) throw new Error(`IA ${res.status}: ${(await res.text()).slice(0, 200)}`);
  const data = await res.json();
  return data.content.filter((b: { type: string }) => b.type === "text").map((b: { text: string }) => b.text).join("");
}

function extractJson(text: string) {
  const s = text.indexOf("{"), e = text.lastIndexOf("}");
  return JSON.parse(text.slice(s, e + 1));
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    // Cliente con el usuario que llama: respeta TODAS las reglas de seguridad
    const sb = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
      global: { headers: { Authorization: req.headers.get("Authorization") ?? "" } },
    });
    const { data: { user } } = await sb.auth.getUser();
    if (!user) return json({ error: "Inicia sesión de nuevo" }, 401);

    const body = await req.json();
    const { action, capsule_id } = body;

    // ---------------- Resumen de una cápsula ----------------
    if (action === "summary") {
      const { data: c } = await sb.from("capsules_feed").select("*").eq("id", capsule_id).maybeSingle();
      if (!c) return json({ error: "No se encontró la cápsula" }, 404);
      if (!c.is_open) return json({ error: "La cápsula todavía está cerrada" }, 403);

      const { data: media } = await sb.from("capsule_media").select("path, kind").eq("capsule_id", capsule_id);
      const photos = (media ?? []).filter((m) => m.kind === "photo").slice(0, 4);
      const videos = (media ?? []).filter((m) => m.kind === "video").length;

      const content: unknown[] = [];
      for (const p of photos) {
        const { data: blob } = await sb.storage.from("media").download(p.path);
        if (blob) content.push({
          type: "image",
          source: { type: "base64", media_type: "image/jpeg", data: encodeBase64(new Uint8Array(await blob.arrayBuffer())) },
        });
      }
      content.push({
        type: "text",
        text: `Eres el narrador cálido de una app de cápsulas del tiempo.
Esta cápsula se llama "${c.title}", se guardó el ${c.created_at}${c.place_name ? ` en ${c.place_name}` : ""}.
Tiene ${photos.length} fotos (adjuntas) y ${videos} videos.
Carta que escribió la persona: """${(c.letter ?? "").slice(0, 3000)}"""
Escribe en español un resumen emotivo de 3 a 5 frases, en segunda persona ("Ese día…", "Recuerdas…"),
mencionando detalles concretos que se ven en las fotos y el sentido de la carta.
No inventes nombres ni hechos que no aparezcan. Sin títulos, listas ni markdown.`,
      });
      const summary = (await askClaude(content, 600)).trim();
      if (c.owner === user.id) await sb.from("capsules").update({ ai_summary: summary }).eq("id", capsule_id);
      return json({ summary });
    }

    // ---------------- Línea de tiempo y mapa de recuerdos ----------------
    if (action === "timeline") {
      const { data: caps } = await sb.from("capsules_feed")
        .select("id,title,letter,created_at,unlock_at,place_name,lat,lng,is_open,ai_summary,media_count,owner_name")
        .order("created_at");
      if (!caps?.length) return json({ resumen: "Aún no tienes cápsulas.", capitulos: [], lugares: [] });

      const items = caps.map((c) => ({
        id: c.id, titulo: c.title, creada: c.created_at, se_abre: c.unlock_at,
        lugar: c.place_name, lat: c.lat, lng: c.lng, abierta: c.is_open, recuerdos: c.media_count,
        de: c.owner_name,
        // Las cápsulas cerradas NO envían su contenido: la IA no puede revelarlo
        texto: c.is_open ? (c.ai_summary || (c.letter ?? "").slice(0, 500)) : null,
      }));

      const out = extractJson(await askClaude(
        `Eres el narrador de una app de cápsulas del tiempo. Con estas cápsulas, crea una línea de tiempo de la vida del usuario.
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
Cápsulas: ${JSON.stringify(items)}`,
        3000,
      ));
      return json(out);
    }


    // ---------------- Sugerir título y portada al crear ----------------
    if (action === "suggest") {
      const imgs: string[] = (body.images ?? []).slice(0, 3);
      const content: unknown[] = imgs.map((data) => ({ type: "image", source: { type: "base64", media_type: "image/jpeg", data } }));
      content.push({ type: "text", text: `Ayudas a crear cápsulas del tiempo. Con ${imgs.length} fotos (numeradas desde 0 en el orden adjunto)${body.place ? `, tomadas en ${body.place}` : ""} y esta carta: """${String(body.letter ?? "").slice(0, 1500)}"""
Propón 3 títulos cortos (máx. 6 palabras), cálidos y en español, y elige la foto más bonita para portada.
Responde SOLO JSON: {"titulos":["","",""],"portada":0}` });
      return json(extractJson(await askClaude(content, 400)));
    }

    // ---------------- Ayuda para escribir la carta ----------------
    if (action === "letter") {
      const tono = String(body.tono ?? "cálido");
      const out = await askClaude(`Ayudas a escribir cartas para cápsulas del tiempo, en español de Colombia.
Mejora y completa esta carta con un tono ${tono}. Conserva TODO lo que la persona escribió (nombres, hechos y su forma de hablar); solo ordena, corrige y agrega 1 o 2 frases que lo hagan más emotivo. No inventes hechos.
Si la carta está casi vacía, escribe un borrador corto para que la persona lo complete con [espacios entre corchetes].
Devuelve SOLO la carta, sin explicaciones.
Carta: """${String(body.letter ?? "").slice(0, 4000)}"""
Título de la cápsula: ${String(body.title ?? "")}`, 1200);
      return json({ carta: out.trim() });
    }

    // ---------------- Recuerdo de hace un año ----------------
    if (action === "year_ago") {
      const from = new Date(Date.now() - 380 * 864e5).toISOString();
      const to = new Date(Date.now() - 350 * 864e5).toISOString();
      const { data: caps } = await sb.from("capsules_feed").select("id,title,letter,ai_summary,place_name,created_at,is_open")
        .gte("created_at", from).lte("created_at", to).eq("is_open", true).limit(3);
      if (!caps?.length) return json({ mensaje: null });
      const c = caps[0];
      const msg = await askClaude(`Escribe en español una notificación breve (máx. 2 frases, segunda persona, cálida) para recordar que hace un año la persona guardó la cápsula "${c.title}"${c.place_name ? ` en ${c.place_name}` : ""}. Contexto: ${(c.ai_summary || (c.letter ?? "")).slice(0, 600)}. Sin comillas ni emojis al inicio.`, 200);
      return json({ mensaje: msg.trim(), capsule_id: c.id, titulo: c.title });
    }

    // ---------------- Textos del álbum animado ----------------
    if (action === "album") {
      const { data: c } = await sb.from("capsules_feed").select("*").eq("id", capsule_id).maybeSingle();
      if (!c?.is_open) return json({ error: "La cápsula todavía está cerrada" }, 403);
      const { data: media } = await sb.from("capsule_media").select("path, kind").eq("capsule_id", capsule_id).eq("kind", "photo").order("created_at").limit(8);
      const content: unknown[] = [];
      for (const m of media ?? []) {
        const { data: blob } = await sb.storage.from("media").download(m.path);
        if (blob) content.push({ type: "image", source: { type: "base64", media_type: "image/jpeg", data: encodeBase64(new Uint8Array(await blob.arrayBuffer())) } });
      }
      content.push({ type: "text", text: `Vas a narrar un álbum animado de la cápsula "${c.title}" (${c.place_name ?? "sin lugar"}). Hay ${content.length} fotos en orden.
Escribe en español, cálido y concreto, sin inventar nombres:
Responde SOLO JSON: {"intro":"1 frase de apertura","fotos":["1 frase corta por foto, en el mismo orden"],"cierre":"1 frase final"}` });
      const out = extractJson(await askClaude(content, 1200));
      return json({ ...out, paths: (media ?? []).map((m) => m.path) });
    }

    return json({ error: "Acción no válida" }, 400);
  } catch (e) {
    return json({ error: e instanceof Error ? e.message : String(e) }, 500);
  }
});
