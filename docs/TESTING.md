# Pruebas de Time Capsule

## App en la computadora (137 pruebas)
```bash
gradlew :app:testDebugUnitTest
```
Incluyen reglas de una cápsula nueva, el ViewModel de Nueva cápsula con una nube falsa, contraseñas seguras,
seguridad de la base de datos (RLS, bcrypt costo 12, ninguna clave secreta en el código), idioma, accesibilidad
(contraste WCAG AA y letras de al menos 12 sp), avisos, fechas y registro de fallos.

## App en el emulador (4 pruebas de interfaz)
```bash
gradlew :app:connectedDebugAndroidTest
```
`SignInUiTest`: la entrada ofrece correo y Google, muestra lo que le falta a la contraseña, no deja crear
una cuenta con contraseña débil y se traduce al inglés. No crea cuentas ni se conecta.

## Función de IA (21 pruebas)
```bash
cd ia && npm test
```
Sesión obligatoria, límite diario, idioma, formato de respuesta y protección contra instrucciones escondidas en la carta.

## En GitHub
`android.yml` (pruebas, lint, APK y emulador), `ia.yml` (IA) y `keep-supabase-awake.yml` (revisa cada día que la base de datos
responda y abre un aviso si algo se pausa).
