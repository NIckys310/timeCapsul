// =====================================================================
//  TIME CAPSULE · Función de IA (Supabase Edge Function "ai-memories")
//  - action "summary":  resume una cápsula abierta mirando sus fotos
//  - action "timeline": crea capítulos de vida y notas por lugar
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

    const { action, capsule_id } = await req.json();

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

    return json({ error: "Acción no válida" }, 400);
  } catch (e) {
    return json({ error: e instanceof Error ? e.message : String(e) }, 500);
  }
});
