# Firma de Time Capsule

La llave de producción es **privada**: no está en este repositorio.

| Dato | Valor |
|---|---|
| Archivo | `C:/Users/Nicky/.claves-android/timecapsule-release.jks` (fuera del repo) |
| Alias | `timecapsule` |
| Contraseña | cifrada con DPAPI de Windows en `~/.gradle/gradle.properties` como `TC_STORE_PASSWORD_DPAPI`: solo tu usuario de Windows en este PC puede descifrarla. Nunca está en texto plano ni en GitHub |
| SHA-1 | `F4:C6:29:1F:A0:FE:5E:87:CA:37:74:8C:15:14:8B:31:9A:DA:02:8A` (registrada en Google Cloud) |

## Crear el APK de producción

```
./gradlew :app:assembleRelease
```

Sale en `app/build/outputs/apk/release/app-release.apk`, firmado y optimizado con R8.

## Importante

- **Haz una copia de seguridad** de la carpeta `.claves-android` (por ejemplo en una memoria USB).
  Sin esa llave no se pueden publicar actualizaciones que se instalen encima de la app.
- La llave anterior (contraseña `android`) estuvo pública en este repositorio: se retiró y su huella
  ya no está autorizada en Google Cloud, así que no sirve para "Continuar con Google".
- Las compilaciones de depuración usan la llave de prueba de Android; "Continuar con Google"
  solo funciona en el APK de producción.
