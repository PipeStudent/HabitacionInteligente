# Habitación Inteligente – App Android (Unidad 2)

App Kotlin que controla y monitorea el ESP32 de la habitación inteligente por **WiFi (MQTT sobre TLS)**.
Dos celulares con la app instalada ven el mismo estado y se sincronizan entre sí a través del broker.

## Configurar Firebase (obligatorio antes de compilar)
1. Entra a https://console.firebase.google.com → *Agregar proyecto*.
2. *Agregar app Android* con el nombre de paquete `com.felipe.habitacioninteligente` → descarga `google-services.json` y colócalo en `app/`.
3. *Authentication → Sign-in method* → habilita **Correo electrónico/contraseña**.
4. *Firestore Database → Crear base de datos* (modo producción) → pestaña *Reglas* → pega el contenido de `firestore.rules` → Publicar.
5. Registra tu cuenta en la app; luego en Firestore, colección `users`, abre tu documento y cambia `role` de `VIEWER` a `ADMIN`.
   (Por seguridad las cuentas nuevas siempre nacen VIEWER; solo un ADMIN puede cambiar roles.)

## Cómo abrirla
1. Android Studio (Koala o superior) → *Open* → carpeta `HabitacionInteligente`.
2. Si pide el Gradle wrapper o actualizar AGP/Gradle, acepta. Sync y *Run*.
3. Instala el APK en **dos dispositivos** (o un celular + emulador).

## Cómo probar
1. Carga `esp32_firmware/habitacion_esp32.ino` en el ESP32 (ajusta WiFi, pines y `PREFIX`).
2. En cada celular crea una cuenta con correo (una será ADMIN y la otra VIEWER/solo lectura, según el paso 5 de Firebase).
3. Prende la luz en el celular A → el switch cambia también en el celular B y en el ESP32.
4. Sin ESP32: usa el cliente web de HiveMQ (broker público) y publica en `habitacion_felipe_01/sensores` el JSON `{"temp":24.5,"hum":50,"mov":1}`.

> El broker público es visible por todos: cambia el prefijo `habitacion_felipe_01/` por uno único (app y firmware) o usa un broker con usuario/clave.

## Topics MQTT
| Topic | Dirección | Contenido |
|---|---|---|
| `<prefijo>/sensores` | ESP32 → apps | `{"temp":..,"hum":..,"mov":0/1}` cada 3 s |
| `<prefijo>/estado` (retenido) | ESP32 → apps | `{"luz":0/1,"fan":0/1}` |
| `<prefijo>/status` (retenido, LWT) | ESP32 → apps | `online` / `offline` |
| `<prefijo>/cmd/luz`, `cmd/ventilador` | apps → ESP32 | `ON` / `OFF` |

## Mapa con la guía
- **2.1.1 Herramientas**: Android Studio + Kotlin, ViewBinding, Material 3, Firebase Auth + Cloud Firestore, HiveMQ MQTT client, androidx.security-crypto.
- **2.1.2 Conexión inalámbrica**: WiFi + MQTT (pub/sub, soporta varios clientes, TLS, reconexión automática, LWT para detectar el ESP32 caído).
- **2.1.3 Seguridad (ISO 27400 / OT)**: autenticación con Firebase Auth (hash gestionado por Google, correo/contraseña) y reglas de Firestore por rol; sesión y credenciales del broker en `EncryptedSharedPreferences` (Keystore); TLS al broker; tráfico sin cifrar prohibido (`network_security_config`); roles ADMIN/VIEWER; bloqueo tras 5 intentos fallidos; `allowBackup=false`; bitácora de eventos.
- **2.1.4 Interconexión**: dos instancias de la app + ESP32 sincronizados por el broker.
- **Login/almacenamiento**: Firebase Authentication + Cloud Firestore. Colecciones: `users` (correo y rol), `readings` (historial de sensores cada 30 s, escrito por el ADMIN) y `events` (bitácora de acciones). Justificación: los usuarios y datos se comparten entre los dos celulares, escala sin backend propio y usa TLS por defecto; Firestore cachea offline.

## Pantallas (mockup)
Login/Registro (correo) → Dashboard (sensores, switches luz/ventilador, estado de conexión) → Historial/Eventos → Configuración MQTT (solo ADMIN).

## Entrega
Sube el proyecto a GitHub y pega el link en un bloc de notas (.txt) para el AAI.
