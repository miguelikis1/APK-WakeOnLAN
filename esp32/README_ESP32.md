# ESP32 WoL MQTT Gateway (EMQX Cloud Serverless)

Este firmware permite que un ESP32 reciba la orden de encendido de forma remota a través de tu broker de **EMQX Cloud Serverless** por TLS y envíe el paquete mágico Wake-on-LAN a la tarjeta de red de tu ordenador en la red local.

---

## 🛠️ Requisitos de Hardware y Software
1. **ESP32** (NodeMCU-32S, ESP32 DevKit, Wemos D1 Mini ESP32, etc.) conectado a la misma red Wi-Fi o router que el PC (vía Ethernet).
2. **Arduino IDE** o **PlatformIO** con el core oficial de ESP32 instalado.
3. Librería **PubSubClient** (de Nick O'Leary) instalada en el gestor de librerías.

---

## ⚙️ Pasos de Configuración en el Código (`esp32_wol_mqtt.ino`)

1. **Credenciales Wi-Fi**:
   ```cpp
   const char* WIFI_SSID     = "TU_RED_WIFI";
   const char* WIFI_PASSWORD = "TU_CONTRASEÑA";
   ```
2. **Endpoint de EMQX Cloud**:
   Copia el host asignado a tu despliegue Serverless en [EMQX Cloud Console](https://cloud.emqx.com/) (terminado en `.emqxsl.com`):
   ```cpp
   const char* MQTT_BROKER   = "tu-instancia.emqxsl.com";
   const int   MQTT_PORT     = 8883; // TLS
   const char* MQTT_USER     = "usuario_creado_en_emqx";
   const char* MQTT_PASSWORD = "password_creado_en_emqx";
   ```
3. **Dirección MAC de la tarjeta de red del PC**:
   Abre la consola en tu PC (`cmd` en Windows) y ejecuta `getmac /v` o `ipconfig /all`. Localiza la dirección física del adaptador Ethernet:
   ```cpp
   byte TARGET_PC_MAC[6] = { 0x04, 0xD4, 0xC4, 0x56, 0x78, 0x9A };
   ```

---

## 🔄 Flujo de Comunicación con la App Android

```
[App Android] --- (TLS 8883: "WAKE" en casa/pc/wol) ---> [EMQX Cloud Broker]
                                                                  |
                                                                  v (TLS 8883)
                                                              [ESP32]
                                                                  |
                                                     [UDP Broadcast :9]
                                                                  |
                                                                  v
                                                            [PC Remoto] (Arranca)
                                                                  |
[App Android] <-- (TLS 8883: "WOL_SENT" en casa/pc/status) --- [ESP32]
     |
     +--> Inicia sondeo TCP (1500ms cada 5s) a la IP de Tailscale del PC (puerto 3389/22)
     |
     v
[PC ONLINE!] Detectado por la App Android
```
