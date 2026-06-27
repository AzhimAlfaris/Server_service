# Panduan Konfigurasi ESP32

Dokumen ini menjelaskan cara menghubungkan ESP32 ke project Verdant Flow untuk menerima command siram manual secara real-time dari RabbitMQ MQTT.

Panduan ini ditulis untuk Arduino IDE, dengan library utama:

- `PubSubClient` untuk koneksi MQTT
- `ArduinoJson` untuk parsing payload JSON
- `WiFi` bawaan ESP32 untuk koneksi jaringan

Fokus panduan ini:

- koneksi WiFi
- koneksi MQTT ke RabbitMQ
- subscribe ke topic command manual
- parsing payload JSON
- kontrol relay untuk 3 pot tanaman

## 1. Gambaran Singkat Alur

Alur command manual saat ini adalah:

1. User mengirim request ke `application-service`.
2. `application-service` memvalidasi JWT dan ownership device.
3. Backend mem-publish payload ke RabbitMQ dengan routing key MQTT-style:

```text
device/command/<cleanAddress>/pot/<potIndex>
```

4. RabbitMQ MQTT plugin meneruskan message ke queue subscription MQTT.
5. ESP32 subscribe ke topic tersebut dan menerima command secara real-time.
6. ESP32 membaca `pot_index` dan `duration` dari JSON payload.
7. ESP32 menyalakan relay yang sesuai dengan pot yang dituju.

## 2. Format Topic MQTT

Karena backend memakai RabbitMQ MQTT plugin, format topic yang dipakai adalah **slash** `/`, bukan titik `.`.

Contoh:

```text
device/command/24A0C4827D64/pot/1
device/command/24A0C4827D64/pot/2
device/command/24A0C4827D64/pot/3
```

Untuk subscribe semua pot milik satu ESP32, gunakan wildcard:

```text
device/command/24A0C4827D64/pot/+
```

## 3. Client ID MQTT

Client ID MQTT sebaiknya memakai MAC Address asli ESP32 agar konsisten dengan queue MQTT subscription RabbitMQ.

Contoh Client ID:

```text
24A0C4827D64
```

Catatan:

- MAC asli ESP32 biasanya tampil dengan tanda titik dua saat dibaca dari firmware.
- Untuk Client ID, gunakan versi tanpa tanda titik dua.
- Untuk topic device, backend juga mengirim versi MAC tanpa titik dua.

## 4. Payload JSON Command

Payload yang diterima ESP32 dari backend sekarang membawa field:

- `email`
- `address`
- `command`
- `duration`
- `pot_index`

Contoh payload:

```json
{
  "email": "user@example.com",
  "address": "24:0A:C4:82:7D:64",
  "command": "PUMP_ON_MANUAL",
  "duration": 5,
  "pot_index": 1
}
```

Yang paling penting untuk kontrol relay:

- `pot_index` menentukan pot mana yang disiram
- `duration` menentukan berapa lama relay ON

## 5. Mapping GPIO ke Pot

Contoh mapping yang dipakai di panduan ini:

- Pot 1 -> GPIO 12
- Pot 2 -> GPIO 13
- Pot 3 -> GPIO 14

Silakan sesuaikan bila wiring hardware Anda berbeda.

## 6. Sketch Arduino IDE

Kode berikut bisa dipakai sebagai dasar firmware ESP32.

```cpp
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// =============================
// WiFi Configuration
// =============================
const char* WIFI_SSID = "ISI_WIFI_ANDA";
const char* WIFI_PASSWORD = "ISI_PASSWORD_WIFI_ANDA";

// =============================
// MQTT Configuration
// =============================
const char* MQTT_HOST = "192.168.1.10";  // ganti dengan IP/hostname RabbitMQ
const uint16_t MQTT_PORT = 1883;

// RabbitMQ MQTT plugin biasanya tidak memerlukan username/password
// jika broker Anda dibuka secara publik di jaringan lokal.
// Jika Anda mengaktifkan auth MQTT, sesuaikan di sini.
const char* MQTT_USERNAME = ""; // optional
const char* MQTT_PASSWORD = ""; // optional

// =============================
// Device Identity
// =============================
String deviceMacRaw;
String deviceMacClean;
String mqttClientId;

// =============================
// Relay Pins
// =============================
const int RELAY_POT_1 = 12;
const int RELAY_POT_2 = 13;
const int RELAY_POT_3 = 14;

// Sebagian modul relay aktif LOW.
// Jika relay Anda aktif HIGH, ubah nilai ini.
const int RELAY_ON = LOW;
const int RELAY_OFF = HIGH;

// =============================
// MQTT Client
// =============================
WiFiClient espClient;
PubSubClient mqttClient(espClient);

// =============================
// Helpers
// =============================
String macWithoutColon(const String& mac) {
  String out = mac;
  out.replace(":", "");
  out.toUpperCase();
  return out;
}

String topicForAllPots(const String& cleanMac) {
  return "device/command/" + cleanMac + "/pot/+";
}

void relayOffAll() {
  digitalWrite(RELAY_POT_1, RELAY_OFF);
  digitalWrite(RELAY_POT_2, RELAY_OFF);
  digitalWrite(RELAY_POT_3, RELAY_OFF);
}

void setRelayByPotIndex(int potIndex, bool on) {
  int pin = -1;

  switch (potIndex) {
    case 1:
      pin = RELAY_POT_1;
      break;
    case 2:
      pin = RELAY_POT_2;
      break;
    case 3:
      pin = RELAY_POT_3;
      break;
    default:
      Serial.printf("Pot index tidak valid: %d\n", potIndex);
      return;
  }

  digitalWrite(pin, on ? RELAY_ON : RELAY_OFF);
}

void runWatering(int potIndex, int durationSeconds) {
  if (durationSeconds <= 0) {
    durationSeconds = 1;
  }

  Serial.printf("Mulai siram pot %d selama %d detik\n", potIndex, durationSeconds);
  setRelayByPotIndex(potIndex, true);
  delay(durationSeconds * 1000UL);
  setRelayByPotIndex(potIndex, false);
  Serial.printf("Selesai siram pot %d\n", potIndex);
}

// =============================
// MQTT Callback
// =============================
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.println("Pesan MQTT masuk:");
  Serial.println(topic);

  String payloadText;
  payloadText.reserve(length + 1);
  for (unsigned int i = 0; i < length; i++) {
    payloadText += (char)payload[i];
  }

  Serial.println(payloadText);

  DynamicJsonDocument doc(512);
  DeserializationError err = deserializeJson(doc, payloadText);
  if (err) {
    Serial.print("Gagal parsing JSON: ");
    Serial.println(err.c_str());
    return;
  }

  int potIndex = doc["pot_index"] | 0;
  int duration = doc["duration"] | 0;
  const char* command = doc["command"] | "";

  if (potIndex < 1 || potIndex > 3) {
    Serial.printf("pot_index tidak valid: %d\n", potIndex);
    return;
  }

  if (String(command) != "PUMP_ON_MANUAL") {
    Serial.print("Command tidak dikenali: ");
    Serial.println(command);
    return;
  }

  runWatering(potIndex, duration);
}

// =============================
// MQTT Reconnect
// =============================
void reconnectMqtt() {
  while (!mqttClient.connected()) {
    Serial.print("Mencoba koneksi MQTT...");

    bool connected = false;
    if (strlen(MQTT_USERNAME) > 0) {
      connected = mqttClient.connect(
        mqttClientId.c_str(),
        MQTT_USERNAME,
        MQTT_PASSWORD
      );
    } else {
      connected = mqttClient.connect(mqttClientId.c_str());
    }

    if (connected) {
      Serial.println("connected");

      String subscribeTopic = topicForAllPots(deviceMacClean);
      mqttClient.subscribe(subscribeTopic.c_str());

      Serial.print("Subscribe ke topic: ");
      Serial.println(subscribeTopic);
    } else {
      Serial.print("gagal, rc=");
      Serial.print(mqttClient.state());
      Serial.println(" coba lagi dalam 5 detik");
      delay(5000);
    }
  }
}

// =============================
// WiFi Connect
// =============================
void connectWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.setSleep(false);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print("Menghubungkan WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.println("WiFi connected");
  Serial.print("IP: ");
  Serial.println(WiFi.localIP());
}

// =============================
// Arduino Setup
// =============================
void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(RELAY_POT_1, OUTPUT);
  pinMode(RELAY_POT_2, OUTPUT);
  pinMode(RELAY_POT_3, OUTPUT);
  relayOffAll();

  connectWiFi();

  deviceMacRaw = WiFi.macAddress();
  deviceMacClean = macWithoutColon(deviceMacRaw);
  mqttClientId = deviceMacClean;

  Serial.print("MAC raw   : ");
  Serial.println(deviceMacRaw);
  Serial.print("MAC clean : ");
  Serial.println(deviceMacClean);
  Serial.print("Client ID : ");
  Serial.println(mqttClientId);

  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setBufferSize(512);
  mqttClient.setKeepAlive(30);
  mqttClient.setSocketTimeout(5);
}

// =============================
// Arduino Loop
// =============================
void loop() {
  if (WiFi.status() != WL_CONNECTED) {
    connectWiFi();
  }

  if (!mqttClient.connected()) {
    reconnectMqtt();
  }

  mqttClient.loop();
}
```

## 7. Penjelasan Logika

### 7.1 Subscribe topic

ESP32 subscribe ke:

```text
device/command/<cleanAddress>/pot/+
```

Contoh:

```text
device/command/24A0C4827D64/pot/+
```

Artinya ESP32 akan menerima command untuk pot 1, 2, dan 3 dari device miliknya sendiri.

### 7.2 Parsing payload

Saat pesan masuk, callback akan:

1. mengubah payload byte menjadi string
2. parsing JSON memakai ArduinoJson
3. membaca `pot_index`
4. membaca `duration`
5. mengecek command `PUMP_ON_MANUAL`
6. menjalankan relay sesuai `pot_index`

### 7.3 Kontrol relay

Switch-case dipakai untuk memetakan `pot_index` ke GPIO:

- `1` -> GPIO 12
- `2` -> GPIO 13
- `3` -> GPIO 14

Jika `pot_index` di luar range 1-3, command diabaikan.

## 8. Catatan Integrasi dengan Backend

Backend sekarang mengirim command dalam bentuk:

```text
device/command/{cleanAddress}/pot/{potIndex}
```

dan payload JSON berisi:

```json
{
  "email": "...",
  "address": "...",
  "command": "PUMP_ON_MANUAL",
  "duration": 5,
  "pot_index": 1
}
```

Jadi ESP32 cukup:

- subscribe ke topic wildcard pot
- parse payload JSON
- jalankan relay sesuai `pot_index`

## 9. Saran Implementasi Hardware

- Pastikan relay 3 channel memakai sumber daya yang stabil.
- Jika relay aktif LOW, gunakan `RELAY_ON = LOW`.
- Jika relay aktif HIGH, ubah ke `RELAY_ON = HIGH`.
- Gunakan delay siram secukupnya agar tidak memblok terlalu lama.
- Untuk produksi, pertimbangkan mengganti `delay()` dengan state machine non-blocking.

## 10. Checklist Uji Coba

1. Sambungkan ESP32 ke WiFi.
2. Jalankan RabbitMQ dengan port `1883` aktif.
3. Pastikan topic yang disubscribe cocok dengan MAC device.
4. Kirim request `POST /api/sensor-data/command`.
5. Pastikan `pot_index` sesuai dengan relay yang mau diaktifkan.
6. Lihat Serial Monitor ESP32.
7. Pastikan relay yang sesuai menyala dan mati sesuai durasi.

## 11. Ringkasan

Untuk fitur siram manual per pot, ESP32 perlu:

- client ID berbasis MAC asli
- subscribe ke topic MQTT style dengan slash
- parsing JSON payload
- mapping `pot_index` ke GPIO relay
- menjalankan relay sesuai durasi siram

Dengan konfigurasi ini, ESP32 bisa menerima command manual secara real-time dari RabbitMQ MQTT tanpa HTTP polling.
