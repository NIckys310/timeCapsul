/* =========================================================
   TIME CAPSULE · IA · Llamadas a Claude
   La clave la pone Netlify sola (AI Gateway): no hay ninguna clave en el código.
   ========================================================= */
import Anthropic from '@anthropic-ai/sdk';

export const MODELO = () => process.env.ANTHROPIC_MODEL || 'claude-sonnet-5-5';
export const hayClave = () => Boolean(process.env.ANTHROPIC_API_KEY);

let cliente;
const claude = () => (cliente ??= new Anthropic());

/** Pregunta a Claude y devuelve solo el texto. `contenido` es texto o una lista de bloques (imágenes + texto). */
export async function preguntar(contenido, maxTokens = 1500) {
  const msg = await claude().messages.create({
    model: MODELO(),
    max_tokens: maxTokens,
    messages: [{ role: 'user', content: contenido }]
  });
  if (msg.stop_reason === 'refusal') throw new Error('La IA no pudo responder a esto.');
  return msg.content.filter((b) => b.type === 'text').map((b) => b.text).join('');
}

/** Saca el objeto JSON de una respuesta (aunque venga con texto alrededor). */
export function extraerJson(texto) {
  const a = texto.indexOf('{'), b = texto.lastIndexOf('}');
  if (a < 0 || b <= a) throw new Error('La IA no respondió en el formato esperado. Intenta de nuevo.');
  return JSON.parse(texto.slice(a, b + 1));
}

export const imagen = (base64) => ({ type: 'image', source: { type: 'base64', media_type: 'image/jpeg', data: base64 } });

/** Mensaje amable según el error de Claude. */
export function errorAmable(e) {
  if (e instanceof Anthropic.RateLimitError) return [429, 'La IA está muy ocupada. Intenta en un minuto.'];
  if (e instanceof Anthropic.AuthenticationError) return [503, 'La IA aún no está activada en el servidor.'];
  if (e instanceof Anthropic.APIError) return [502, 'La IA no respondió. Intenta de nuevo en un momento.'];
  return [500, e?.message || 'Algo salió mal con la IA.'];
}
