# Time Capsule · App móvil (Android)

Cápsulas del tiempo digitales con fotos, videos y cartas que solo se abren en una fecha
o al volver a un lugar. Se comparten con familia y amigos, y la IA resume los recuerdos,
crea una línea de tiempo interactiva y un mapa de recuerdos.

## Qué necesitas
- Android Studio (Ladybug o más nuevo)
- Una cuenta gratis en https://supabase.com (cuentas, base de datos y archivos)
- Una clave de API de Anthropic (https://console.anthropic.com) para la IA
- Un celular Android 8 o superior, o el emulador de Android Studio

## Paso 1 · Crear la nube en Supabase
1. Entra a supabase.com → **New project**. Ponle `time-capsule`, crea una contraseña y elige la región más cercana.
2. Espera 1–2 minutos a que termine de crearse.
3. Menú izquierdo → **SQL Editor** → **New query**. Abre `supabase/1_base_de_datos.sql`,
   copia todo, pégalo y pulsa **Run**. Debe decir *Success*.
4. Menú → **Authentication** → **Sign In / Providers** → **Email** → desactiva **Confirm email** → Save.
   (Así las cuentas funcionan al instante. Si lo dejas activo, cada persona debe confirmar su correo.)
5. Menú → **Project Settings** → **API Keys**. Copia:
   - **Project URL** (algo como `https://abcd1234.supabase.co`)
   - La clave **anon public** (pestaña *Legacy API Keys*) o la **Publishable key**.

## Paso 1b · Actualización 2 (clave de apertura)
En **SQL Editor → New query** pega `supabase/2_clave_de_apertura.sql` y pulsa **Run**.

## Paso 2 · Activar la IA
1. Menú → **Edge Functions** → **Deploy a new function** → **Via Editor**.
2. Nombre: `ai-memories`. Borra el código de ejemplo y pega todo `supabase/functions/ai-memories/index.ts`.
3. Pulsa **Deploy function**.
4. Menú → **Edge Functions** → **Secrets** → **Add new secret**:
   - Name: `ANTHROPIC_API_KEY`  ·  Value: tu clave `sk-ant-…`

## Paso 3 · Abrir el proyecto en Android Studio
1. Descomprime `TimeCapsule.zip`.
2. Android Studio → **File → Open** → elige la carpeta `TimeCapsule` → **Trust project**.
3. Espera a que termine **Gradle sync** (la primera vez descarga librerías: 3–10 min).
   Si te ofrece actualizar el *Android Gradle Plugin*, puedes aceptar.
4. Abre `app/src/main/java/com/timecapsule/app/data/Config.kt` y pega tu URL y tu clave.

## Paso 4 · Ejecutar
- **En tu celular**: Ajustes → Acerca del teléfono → toca 7 veces *Número de compilación*.
  Luego Ajustes → Opciones de desarrollador → activa **Depuración USB**. Conecta el cable y acepta.
- **En el emulador**: Device Manager → **Create device** → Pixel 8 → imagen **con Google Play**.
- Pulsa el botón verde ▶ **Run**.

## Paso 5 · Generar el APK para instalarlo en otros celulares
**Build → Generate Signed App Bundle / APK → APK** → crea un *keystore* nuevo (guárdalo bien) → **release**.
El archivo queda en `app/release/app-release.apk`. Envíalo por WhatsApp o Drive y ábrelo en el celular
(permite "instalar apps de origen desconocido").

## Probar las funciones
| Función | Cómo probarla |
|---|---|
| Apertura por fecha | Crea una cápsula y elige una hora 2 minutos en el futuro. Te llega la notificación y se abre sola. |
| Recuerdos por lugar | Perfil → **Activar** recuerdos por ubicación → "Permitir todo el tiempo". Crea una cápsula con el lugar. En el emulador: **⋯ → Location**, mueve el punto lejos y luego de vuelta. |
| Solo se abre en el lugar | Activa "Solo se abre estando en este lugar". En la cápsula pulsa **Estoy aquí, abrir**. |
| Compartir | Crea una segunda cuenta y escribe su correo al crear o desde la cápsula. |
| Resumen con IA | En una cápsula abierta con fotos → **Resumir este recuerdo**. |
| Línea de tiempo y mapa | Pestañas **Línea de tiempo → Crear con IA** y **Mapa**. |

## Qué archivo cambiar
| Quiero cambiar… | Archivo |
|---|---|
| Colores y tipografía | `ui/theme/Theme.kt` |
| Inicio de sesión | `ui/screens/AuthScreen.kt` |
| Lista de cápsulas y pestañas | `ui/screens/HomeScreen.kt` y `Components.kt` |
| Crear cápsula | `ui/screens/CreateCapsuleScreen.kt` |
| Ver/abrir cápsula | `ui/screens/CapsuleDetailScreen.kt` |
| Línea de tiempo / Mapa / Perfil | `TimelineScreen.kt` / `MapScreen.kt` / `ProfileScreen.kt` |
| Textos de notificaciones | `notify/Workers.kt` y `geo/Receivers.kt` |
| Lo que hace la IA | `supabase/functions/ai-memories/index.ts` |
| Ícono de la app | `res/mipmap-*` (o clic derecho en `res` → New → Image Asset) |

## Límites del plan gratis
- Supabase gratis: 500 MB de base de datos, 1 GB de archivos y videos de máximo 50 MB.
- Un proyecto gratis se pausa tras 7 días sin uso: entra al panel y pulsa *Restore*.
- La IA consume créditos de tu cuenta de Anthropic (cada resumen cuesta centavos).

## Pruebas automáticas
Time Capsule tiene **101 pruebas** que corren en la computadora, sin celular:

```bash
gradlew :app:testDebugUnitTest
```

| Archivo | Qué revisa |
|---|---|
| `FormatoTest` | "Faltan 3 años", "5 días", "2 h 10 min"; fechas en español |
| `CapsulaTest` | Cuándo y dónde se abre una cápsula; fechas dañadas; clave para abrir antes |
| `PistasTest` | "Caliente o frío" para encontrar la cápsula; tendencia y distancia |
| `BorradorTest` | El borrador que no se pierde al ir a Spotify/YouTube; nunca guarda la clave |
| `PlantillasTest` | Plantillas de carta y preguntas para el futuro |
| `MensajesTest` | Errores en español sencillo; línea de tiempo guardada |
| `ReglasCapsulaTest` | Reglas para sellar una cápsula: título, carta, fecha, lugar, clave, 20 fotos y 1 video, correos, canción y preguntas |
| `AvisosTest` | Cuenta regresiva (1 semana, 1 día, 1 hora), avisos de apertura, cápsulas compartidas y actividad |
| `SeguridadTest` | RLS en todas las tablas, funciones seguras y ninguna clave secreta en el código |

En GitHub, cada cambio corre las pruebas, el análisis de Android y crea el APK (`.github/workflows/android.yml`).

## Cómo está organizado el código de las pantallas
| Pantalla | Archivos |
|---|---|
| Nueva cápsula | `CreateCapsuleScreen.kt` (estado y borrador automático) + `CreateCapsuleSections.kt` (título, carta, notas de voz, canción, preguntas, fotos, fecha, clave, lugar, compartir) |
| Reglas de una cápsula nueva | `data/NuevaCapsula.kt` (validación y límites, con pruebas) |
| Ver una cápsula | `CapsuleDetailScreen.kt` + `CapsuleLocked.kt` (cerrada) + `CapsuleOpen.kt` (abierta) + `MediaViewer.kt` + `SharePanel.kt` |
| Extras de una cápsula | `CapsuleExtras.kt` (canción y notas de voz) + `CapsuleQuestions.kt` + `CapsuleSocial.kt` (reacciones, comentarios, cartas) + `CapsuleTogether.kt` (aportar, abrir juntos, rutas) + `CapsuleInvite.kt` + `HotColdFinder.kt` |
| Textos de las notificaciones | `notify/Avisos.kt` (con pruebas) |
| Caliente o frío | `util/Pistas.kt` |
