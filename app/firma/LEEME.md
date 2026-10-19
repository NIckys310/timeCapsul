# Llave de firma de Time Capsule

`timecapsule.keystore` firma **todos** los APK de Time Capsule (los que se compilan en el PC y los que arma GitHub Actions).
Así la huella es siempre la misma y **"Continuar con Google" funciona para cualquier persona** que instale la app.

| Dato | Valor |
|---|---|
| Alias | `timecapsule` |
| Contraseña | `android` (la misma que usan las llaves de prueba de Android) |
| SHA-1 | `EF:22:37:94:03:7B:37:17:E2:88:F4:88:C6:C2:13:B4:0A:10:97:55` |
| SHA-256 | `41:E5:AA:65:86:2F:23:18:43:6A:22:21:27:60:28:52:56:AF:D3:72:3C:6E:94:9C:4B:89:CA:35:E3:84:DC:F2` |

La SHA-1 está registrada en Google Cloud (ID de cliente Android, paquete `com.timecapsule.app`).

⚠ Es una llave de proyecto escolar: está en el repositorio para que cualquier compilación funcione.
Si algún día la app se publica en Play Store, hay que crear una llave nueva **privada** (fuera del repositorio)
y registrar su huella en Google Cloud.
