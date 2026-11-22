# Seguridad de Time Capsule

| Tema | Cómo se protege |
|---|---|
| Contraseñas de cuenta | La app nunca las guarda: las recibe Supabase Auth y guarda solo su huella **bcrypt** (no se puede revertir). |
| Contraseñas nuevas | Mínimo 10 caracteres con minúscula, mayúscula, número y símbolo, en la app **y en el servidor** (Supabase rechaza las débiles). No se aceptan las fáciles de adivinar (123…, password…). |
| Al escribir contraseñas | Se ven como puntos y el teclado no las aprende ni sugiere (`autoCorrectEnabled = false`). |
| Claves de cápsula | Solo se guarda su huella **bcrypt costo 12**; las huellas viejas se mejoran solas al abrir. 5 intentos fallidos → bloqueo de 15 minutos. |
| Entrar con Google | Credential Manager + `nonce` de un solo uso: un token robado no se puede reutilizar. En la app solo hay IDs públicos. |
| Datos | Las 13 tablas tienen seguridad por filas (RLS): cada persona ve solo sus cápsulas y las que le compartieron. Las fotos van a un espacio privado. |
| IA | La clave de Claude vive solo en Netlify. Lo que escribe la persona llega a Claude marcado como dato, nunca como instrucción. Las cápsulas cerradas nunca se envían. Máximo 40 usos al día. |
| Celular | Bloqueo opcional con huella o PIN, ocultar en recientes y bloquear capturas. |
| Llave de firma | Privada, fuera del repositorio; su contraseña está cifrada con DPAPI ([SIGNING.md](SIGNING.md)). |
| Fallos | Se registran en el celular (últimos 60) sin enviarse a terceros. |
