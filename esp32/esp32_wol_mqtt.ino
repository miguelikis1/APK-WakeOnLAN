/**
 * ESP32 Wake-on-LAN via MQTT TLS (EMQX Cloud Serverless)
 *
 * Requisitos de librerías en Arduino IDE / PlatformIO:
 * - WiFi (incluida en el core ESP32)
 * - WiFiClientSecure (incluida en el core ESP32)
 * - WiFiUDP (incluida en el core ESP32)
 * - PubSubClient (de Nick O'Leary)
 */

#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <WiFiUdp.h>
#include <PubSubClient.h>

// ====== 1. Configuración de Wi-Fi ======
const char* WIFI_SSID     = "TU_SSID_WIFI";
const char* WIFI_PASSWORD = "TU_PASSWORD_WIFI";

// ====== 2. Configuración de EMQX Cloud Serverless ======
const char* MQTT_BROKER   = "xxxx.emqxsl.com"; // Tu endpoint de EMQX Cloud
const int   MQTT_PORT     = 8883;               // Puerto TLS estándar
const char* MQTT_USER     = "tu_usuario_emqx";  // Usuario creado en Authentication de EMQX
const char* MQTT_PASSWORD = "tu_password_emqx";

// ====== 3. Topics MQTT ======
const char* TOPIC_COMMAND = "casa/pc/wol";     // Topic donde escucha la orden WAKE
const char* TOPIC_STATUS  = "casa/pc/status";  // Topic donde responde WOL_SENT

// ====== 4. Configuración del PC destino ======
// Dirección MAC física de la tarjeta de red (Ethernet) del PC a despertar:
byte TARGET_PC_MAC[6] = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55 };

// Instancias de Red y MQTT
WiFiClientSecure espClient;
PubSubClient mqttClient(espClient);
WiFiUDP udp;

// Envía el paquete mágico Wake-on-LAN por broadcast UDP (puerto 9)
void sendWakeOnLan(const byte* macAddress) {
  byte magicPacket[102];
  
  // 6 bytes con valor 0xFF
  for (int i = 0; i < 6; i++) {
    magicPacket[i] = 0xFF;
  }
  
  // 16 repeticiones de la dirección MAC
  for (int i = 1; i <= 16; i++) {
    memcpy(&magicPacket[i * 6], macAddress, 6);
  }
  
  // Broadcast a 255.255.255.255 en el puerto 9 (o 7)
  udp.beginPacket(IPAddress(255, 255, 255, 255), 9);
  udp.write(magicPacket, sizeof(magicPacket));
  udp.endPacket();
  
  Serial.println("[WoL] Paquete mágico enviado a la red local.");
}

// Callback cuando llega un mensaje MQTT
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  String message = "";
  for (unsigned int i = 0; i < length; i++) {
    message += (char)payload[i];
  }
  message.trim();
  
  Serial.printf("[MQTT] Mensaje recibido en topic [%s]: %s\n", topic, message.c_str());

  if (String(topic) == TOPIC_COMMAND && message.equalsIgnoreCase("WAKE")) {
    Serial.println("[WoL] Orden 'WAKE' detectada. Despertando PC...");
    
    // 1. Enviar el paquete WoL
    sendWakeOnLan(TARGET_PC_MAC);
    
    // 2. Responder confirmación al topic de estado
    if (mqttClient.publish(TOPIC_STATUS, "WOL_SENT", true)) {
      Serial.println("[MQTT] Confirmación 'WOL_SENT' publicada.");
    } else {
      Serial.println("[MQTT] Error al publicar confirmación 'WOL_SENT'.");
    }
  }
}

void setupWifi() {
  delay(10);
  Serial.println();
  Serial.printf("[WiFi] Conectando a %s", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println("\n[WiFi] Conectado exitosamente.");
  Serial.print("[WiFi] Dirección IP asignada: ");
  Serial.println(WiFi.localIP());
}

void reconnectMqtt() {
  while (!mqttClient.connected()) {
    Serial.print("[MQTT] Conectando a EMQX Cloud Serverless vía TLS...");
    String clientId = "ESP32-WoL-" + String(random(0xffff), HEX);
    
    if (mqttClient.connect(clientId.c_str(), MQTT_USER, MQTT_PASSWORD)) {
      Serial.println(" Conectado!");
      
      // Suscribirse al topic de comandos
      mqttClient.subscribe(TOPIC_COMMAND, 1);
      Serial.printf("[MQTT] Suscrito a topic: %s\n", TOPIC_COMMAND);
      
      // Publicar estado de listo
      mqttClient.publish(TOPIC_STATUS, "ESP32_READY", false);
    } else {
      Serial.printf(" Falló, rc=%d. Reintentando en 5 segundos...\n", mqttClient.state());
      delay(5000);
    }
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n=== ESP32 WoL MQTT Gateway Iniciando ===");

  setupWifi();

  // Para EMQX Cloud Serverless por TLS en el puerto 8883:
  // espClient.setInsecure() permite validar la conexión TLS sin necesidad de incrustar certificados CA en el firmware.
  espClient.setInsecure();

  mqttClient.setServer(MQTT_BROKER, MQTT_PORT);
  mqttClient.setCallback(mqttCallback);
  udp.begin(9);
}

void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    setupWifi();
  }

  if (!mqttClient.connected()) {
    reconnectMqtt();
  }

  mqttClient.loop();
}
