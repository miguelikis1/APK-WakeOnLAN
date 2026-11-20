# 🚀 Wake On LAN - App Nativa Android (Jetpack Compose)

Aplicación nativa de Android desarrollada en **Kotlin** con **Jetpack Compose (Material 3)**, diseñada para ser ultra ligera, de arranque instantáneo y con **tema oscuro OLED por defecto**.

Permite conectarse de forma segura mediante **TLS (puerto 8883)** a un broker MQTT de **EMQX Cloud Serverless** (`*.emqxsl.com`) para enviar la orden de **Wake-on-LAN** a un ESP32 en casa y verificar en tiempo real si el PC remoto ha encendido a través de **Tailscale**.

---

## 📱 Captura de Arquitectura y Flujo

```
+-------------------------------------------------------------+
|                     Android App (Kotlin)                    |
|  - Jetpack Compose M3 (Tema Oscuro OLED)                    |
|  - HiveMQ MQTT Client 1.3.3 (.sslWithDefaultConfig())       |
|  - DataStore Preferences (Configuración Local)              |
+-------------------------------------------------------------+
       |                                              |
       | TLS (Port 8883)                              | TCP Ping (1500ms)
       | QoS 1 "WAKE"                                 | Tailscale IP: 100.x.x.x
       v                                              v
+------------------------+                    +---------------+
|  EMQX Cloud Serverless |                    |   PC Remoto   |
|   (*.emqxsl.com)       |                    |  (Tailscale)  |
+------------------------+                    +---------------+
       |                                              ^
       | TLS (Port 8883)                              |
       v                                              | Magic Packet
+------------------------+                            | (UDP 9 Broadcast)
|     ESP32 en Casa      |----------------------------+
| (WiFiClientSecure/UDP) |  Responde "WOL_SENT"
+------------------------+
```

---

## 🌟 Características Principales

1. **Cliente MQTT Estándar y Moderno (HiveMQ MQTT Client 1.3.3)**:
   - Configurado con `.sslWithDefaultConfig()` para TLS en el puerto 8883.
   - Totalmente compatible con **Android 12, 13, 14 y 15 (API 31+)**, prescindiendo de la obsoleta librería `org.eclipse.paho.android.service`.
   - Incluye las exclusiones de empaquetado `packaging { resources { excludes += ... } }` en `build.gradle.kts` para evitar colisiones de archivos `META-INF` de Netty.

2. **Indicadores de Estado Independientes**:
   - **Broker EMQX / ESP32**: Conectado / Conectando / Desconectado con badge luminoso y visualización del último mensaje recibido en el topic de respuesta (`casa/pc/status`) junto a su marca de tiempo.
   - **PC Remoto (Tailscale)**: Online (verde) / Offline (rojo) / Comprobando mediante socket TCP directo con **timeout de 1500 ms** (por defecto al puerto 3389 RDP o 22 SSH) y botón de refresco manual.

3. **Botón Central Gigante ("ENCENDER PC")**:
   - Pulsa para publicar el payload `"WAKE"` (QoS 1) en el topic de comandos (`casa/pc/wol`).
   - Muestra animación de carga y cuenta regresiva esperando confirmación `"WOL_SENT"` en el topic de estado o timeout de 5 segundos.
   - Inicia automáticamente un **sondeo TCP cada 5 segundos durante 1 minuto (12 intentos)** para detectar el momento exacto en que el PC arranca e informar con un aviso.

4. **Ajustes Rápidos con DataStore Preferences**:
   - Diálogo minimalista para configurar:
     - Broker Address (ej. `xxxx.emqxsl.com`)
     - Puerto TLS (por defecto `8883`)
     - Usuario y Contraseña MQTT (con botón para mostrar/ocultar contraseña)
     - Topic de comando (por defecto `casa/pc/wol`) y de respuesta (por defecto `casa/pc/status`)
     - IP de Tailscale del PC (ej. `100.x.x.x`) y Puerto TCP (por defecto `3389`).

5. **Permisos Estrictos**:
   - Exclusivamente `android.permission.INTERNET` y `android.permission.ACCESS_NETWORK_STATE`.

6. **Manejo Robusto de Excepciones**:
   - Todas las llamadas de red y sockets se ejecutan en corrutinas (`Dispatchers.IO`).
   - Captura completa de errores sin cierres forzados (`crashes`), retroalimentando al usuario mediante `Snackbar`.

---

## 📁 Estructura del Proyecto

```
APK-WakeOnLAN/
├── app/
│   ├── build.gradle.kts             # Dependencias (HiveMQ 1.3.3, DataStore, Compose M3) y packaging excludes
│   ├── proguard-rules.pro           # Reglas de ofuscación para Netty y HiveMQ
│   └── src/main/
│       ├── AndroidManifest.xml      # Permisos y actividad principal
│       ├── java/com/wol/app/
│       │   ├── MainActivity.kt      # Actividad contenedora de Compose
│       │   ├── WolApplication.kt    # Inicialización de DataStore
│       │   ├── data/
│       │   │   ├── AppSettings.kt        # Modelo de configuración
│       │   │   └── SettingsRepository.kt # Persistencia con DataStore Preferences
│       │   ├── network/
│       │   │   ├── MqttManager.kt        # HiveMQ MQTT Client con TLS (.sslWithDefaultConfig)
│       │   │   └── TcpPingManager.kt     # Comprobación de socket TCP (1500 ms)
│       │   └── ui/
│       │       ├── components/
│       │       │   ├── StatusCards.kt    # Tarjetas de estado Broker y PC
│       │       │   └── WakeButton.kt     # Botón central interactivo con animación
│       │       ├── screens/
│       │       │   ├── MainScreen.kt     # Pantalla principal minimalista
│       │       │   └── SettingsDialog.kt # Diálogo de configuración
│       │       ├── theme/
│       │       │   ├── Color.kt          # Paleta oscura OLED de alto contraste
│       │       │   ├── Theme.kt          # Tema oscuro por defecto
│       │       │   └── Type.kt           # Tipografía moderna
│       │       └── viewmodel/
│       │           ├── MainViewModel.kt  # Lógica de estados, publicación y sondeo
│       │           └── MainViewModelFactory.kt
│       └── res/                     # Strings, colores, temas e iconos vectoriales
├── esp32/
│   ├── esp32_wol_mqtt.ino           # Firmware listo para ESP32 (PubSubClient + UDP WoL)
│   └── README_ESP32.md              # Guía de configuración del ESP32
├── build.gradle.kts                 # Configuración raíz de Gradle
├── settings.gradle.kts              # Repositorios y módulos
└── gradle.properties
```

---

## 🛠️ Cómo Compilar y Ejecutar

1. Abre **Android Studio** (Koala, Iguana, Hedgehog o superior).
2. Selecciona **Open** y elige la carpeta `c:\Users\migue\Desktop\APK-WakeOnLAN`.
3. Espera a que Gradle sincronice las dependencias de Maven Central y Google.
4. Conecta tu teléfono Android o inicia un emulador con API 26 o superior.
5. Haz clic en **Run 'app'** (`Shift + F10`) o genera el APK desde el menú `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

---

## 🔌 Configuración en la App

1. Al abrir la app, pulsa el icono de **Ajustes** (arriba a la derecha).
2. Introduce los datos de tu broker de **EMQX Cloud Serverless**:
   - **Broker Address**: ej. `c0123abc.emqxsl.com`
   - **Puerto TLS**: `8883`
   - **Usuario MQTT**: el usuario creado en *Authentication* en la consola de EMQX
   - **Contraseña MQTT**: la clave asociada al usuario
3. Define los topics (o déjalos por defecto):
   - Comando: `casa/pc/wol`
   - Estado: `casa/pc/status`
4. Introduce la **IP de Tailscale** de tu PC (ej. `100.85.12.34`) y el **Puerto TCP** (ej. `3389` para Escritorio Remoto de Windows o `22` para SSH).
5. Pulsa **Guardar y Conectar**. El indicador del Broker se pondrá en verde (**Conectado**) y el botón "ENCENDER PC" estará listo para usarse.
