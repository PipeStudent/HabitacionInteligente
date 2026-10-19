// Firmware ESP32 - Habitacion inteligente (WiFi + MQTT TLS)
// Librerias: PubSubClient, DHT sensor library (Adafruit)
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <DHT.h>

// ---- CONFIGURA ESTO ----
const char* WIFI_SSID = "TU_WIFI";
const char* WIFI_PASS = "TU_CLAVE";
const char* MQTT_HOST = "broker.hivemq.com";
const int   MQTT_PORT = 8883;
const char* MQTT_USER = "";     // vacio si el broker es publico
const char* MQTT_PASS = "";
const String PREFIX   = "habitacion_felipe_01/";   // DEBE coincidir con la app

// Pines (ajusta a tu montaje anterior)
#define PIN_LUZ 26
#define PIN_FAN 27
#define PIN_PIR 14
#define PIN_DHT 4
DHT dht(PIN_DHT, DHT11);

WiFiClientSecure net;
PubSubClient mqtt(net);
bool luz = false, fan = false;
unsigned long lastPub = 0;

void publicarEstado() {
  String s = String("{\"luz\":") + (luz ? 1 : 0) + ",\"fan\":" + (fan ? 1 : 0) + "}";
  mqtt.publish((PREFIX + "estado").c_str(), s.c_str(), true);  // retenido: la 2da app lo recibe al conectar
}

void onMsg(char* topic, byte* payload, unsigned int len) {
  String t = String(topic), m;
  for (unsigned int i = 0; i < len; i++) m += (char)payload[i];
  bool on = (m == "ON");
  if (t == PREFIX + "cmd/luz") { luz = on; digitalWrite(PIN_LUZ, on); }
  else if (t == PREFIX + "cmd/ventilador") { fan = on; digitalWrite(PIN_FAN, on); }
  else return;
  publicarEstado();
}

void conectarMqtt() {
  while (!mqtt.connected()) {
    String id = "esp32-hab-" + String((uint32_t)ESP.getEfuseMac(), HEX);
    String lwt = PREFIX + "status";
    if (mqtt.connect(id.c_str(), MQTT_USER, MQTT_PASS, lwt.c_str(), 1, true, "offline")) {
      mqtt.publish(lwt.c_str(), "online", true);
      mqtt.subscribe((PREFIX + "cmd/#").c_str(), 1);
      publicarEstado();
    } else delay(3000);
  }
}

void setup() {
  pinMode(PIN_LUZ, OUTPUT); pinMode(PIN_FAN, OUTPUT); pinMode(PIN_PIR, INPUT);
  dht.begin();
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) delay(500);
  net.setInsecure();  // TLS sin validar certificado (solo pruebas). Para produccion: net.setCACert(...)
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  mqtt.setCallback(onMsg);
}

void loop() {
  if (WiFi.status() == WL_CONNECTED && !mqtt.connected()) conectarMqtt();
  mqtt.loop();
  if (millis() - lastPub > 3000) {
    lastPub = millis();
    float t = dht.readTemperature(), h = dht.readHumidity();
    if (!isnan(t) && !isnan(h)) {
      String s = String("{\"temp\":") + t + ",\"hum\":" + h + ",\"mov\":" + digitalRead(PIN_PIR) + "}";
      mqtt.publish((PREFIX + "sensores").c_str(), s.c_str());
    }
  }
}
