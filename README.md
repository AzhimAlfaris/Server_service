# Verdant Flow Service Server

Backend microservices Spring Boot untuk monitoring tanaman berbasis ESP32. Project ini memakai Eureka, API Gateway, MySQL, RabbitMQ MQTT, JWT, Gmail SMTP, Prometheus/Grafana, dan ELK.

## Fitur Utama

- Service discovery dan routing request lewat `eureka-server` dan `gateway-service`.
- Register, login, cek akun, ganti password, dan JWT.
- Mendaftarkan MAC address ESP32 ke akun pengguna.
- Mengirim data kelembaban dan status pompa dari ESP32 ke backend.
- Mengambil data sensor terbaru dan riwayat sensor untuk dashboard.
- Mengirim command manual watering dari aplikasi ke ESP32 lewat RabbitMQ MQTT.
- Monitoring service, metrics, dan log terpusat.

## Alur Fitur

### Register akun

- Endpoint utama: `POST /auth/register`
- Disarankan lewat gateway: `http://localhost:8080/auth/register`
- Flow:
  1. Client mengirim `email` dan `password`.
  2. `gateway-service` meneruskan request ke `security-service`.
  3. `security-service` melakukan validasi dan meng-hash password.
  4. `security-service` memanggil `user-service POST /api/users`.
  5. `user-service` menyimpan user ke MySQL.
  6. `security-service` mengembalikan response sukses.

Contoh body:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

### Login dan JWT

- Endpoint utama: `POST /auth/login`
- Disarankan lewat gateway: `http://localhost:8080/auth/login`
- Flow:
  1. Client mengirim `email` dan `password`.
  2. `security-service` mengambil data user dari `user-service`.
  3. Password diverifikasi dengan BCrypt.
  4. Jika valid, `security-service` membuat JWT.
  5. Token dikirim kembali untuk dipakai di header `Authorization: Bearer ...`.

Contoh body:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

### Cek akun

- Endpoint utama: `GET /auth/exists?email=...`
- Disarankan lewat gateway: `http://localhost:8080/auth/exists?email=user@example.com`
- Flow:
  1. Client mengirim email yang ingin dicek.
  2. `security-service` meneruskan request ke `user-service`.
  3. `user-service` mengembalikan `true` atau `false`.

Fitur ini berguna untuk validasi form register dan lupa password.

### Ganti password

- Endpoint utama: `PUT /auth/reset-password`
- Disarankan lewat gateway: `http://localhost:8080/auth/reset-password`
- Flow:
  1. Client mengirim `email`, `password`, dan `passwordAgain`.
  2. `security-service` memeriksa kecocokan dua password tersebut.
  3. `security-service` mengecek email ke `user-service`.
  4. Password baru di-hash lalu dikirim ke `user-service`.
  5. `user-service` menyimpan password baru.

Contoh body:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "passwordAgain": "password123"
}
```

### Mendaftarkan MAC address ESP32

- Endpoint utama untuk dashboard: `POST` atau `PUT /api/sensor-data/device-settings`
- Disarankan lewat gateway: `http://localhost:8080/api/sensor-data/device-settings`
- Flow:
  1. User login dan mengirim request dengan Bearer token.
  2. `application-service` mengambil email dari JWT.
  3. Client mengirim `address` dan `soil_types`.
  4. `application-service` meneruskan data ke `user-service`.
  5. `user-service` menyimpan binding `email`, `address`, dan `soil_types`.
  6. ESP32 mengambil data publik dari `GET /api/device-settings/{address}` untuk mendapatkan email owner dan konfigurasi tanah.

Contoh body:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "soil_types": ["Kering", "Sedang", "Basah"]
}
```

### Kirim data sensor dari ESP32

- Endpoint utama: `POST /api/sensor-readings`
- Flow:
  1. ESP32 membaca sensor kelembaban per pot.
  2. ESP32 mengirim JSON `address`, `email`, dan daftar `pots` ke `microcontroller-service`.
  3. `microcontroller-service` memvalidasi address dan kepemilikan email ke `user-service`.
  4. Data disimpan ke MySQL.
  5. Setelah sukses, service ini mengirim event notifikasi ke RabbitMQ.
  6. `application-service` menerima event tersebut dan mengirim email ke user.

Contoh body:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "email": "user@example.com",
  "pots": [
    {
      "potIndex": 1,
      "sensorValue": "742",
      "moisturePercent": "68",
      "soilCondition": "Normal",
      "action": "Pump ON",
      "pumpDuration": "5 seconds",
      "timestampSensor": "2026-06-14 17:30:00"
    }
  ]
}
```

### Ambil data latest dan history

- Endpoint utama:
  - `GET /api/sensor-data/latest`
  - `GET /api/sensor-data/history?limit=5`
- Disarankan lewat gateway:
  - `http://localhost:8080/api/sensor-data/latest`
  - `http://localhost:8080/api/sensor-data/history?limit=5`
- Flow:
  1. Client login lalu mengirim Bearer token.
  2. `application-service` mengambil email dari JWT.
  3. `application-service` mengirim request ke RabbitMQ.
  4. `microcontroller-service` membaca request tersebut dan mengambil data dari MySQL.
  5. Response dikirim kembali ke `application-service`.
  6. `application-service` meneruskan hasilnya ke client.

### Manual watering command

- Endpoint utama: `POST /api/sensor-data/command`
- Disarankan lewat gateway: `http://localhost:8080/api/sensor-data/command`
- Flow:
  1. Client mengirim `address`, `command`, `duration`, dan `pot_index`.
  2. `application-service` mengambil email dari JWT dan memvalidasi ownership address.
  3. Jika valid, service ini membuat routing key MQTT-style.
  4. Command dipublish ke RabbitMQ topic exchange `amq.topic`.
  5. ESP32 subscribe ke topik MQTT dan mengeksekusi pompa per pot.

Contoh body:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "command": "PUMP_ON_MANUAL",
  "duration": 5,
  "pot_index": 1
}
```

### Monitoring dan logging

- Semua service mengekspos `actuator/health`, `actuator/info`, dan `actuator/prometheus`.
- Prometheus mengambil metrics dari semua service.
- Grafana menampilkan dashboard metrics.
- Log setiap service dikirim ke Logstash lalu disimpan ke Elasticsearch.
- Kibana dipakai untuk membaca log dengan data view per service.

## Akses Penting

- Gateway: `http://localhost:8080`
- Eureka: `http://localhost:8761`
- `microcontroller-service`: `http://localhost:8081`
- `application-service`: `http://localhost:8082`
- `user-service`: `http://localhost:8083`
- `security-service`: `http://localhost:8084`

## Jalankan Lokal

Jalankan masing-masing service dari foldernya dengan:

```powershell
.\mvnw.cmd spring-boot:run
```

Urutan yang aman:

1. `eureka-server`
2. `gateway-service`
3. `user-service`
4. `security-service`
5. `microcontroller-service`
6. `application-service`

## Jalankan dengan Docker

Build dulu artifact `.jar` jika belum ada, lalu jalankan:

```powershell
docker compose up -d --build
```

## Port yang Dipakai

- `gateway-service` -> `8080`
- `eureka-server` -> `8761`
- `microcontroller-service` -> `8081`
- `application-service` -> `8082`
- `user-service` -> `8083`
- `security-service` -> `8084`
- RabbitMQ UI -> `15672`
- RabbitMQ AMQP -> `5672`
- RabbitMQ MQTT -> `1883`
- Prometheus -> `9090`
- Grafana -> `3000`
- Kibana -> `5601`

## Catatan

- `application-service` tidak memiliki database sendiri.
- `user-service` menyimpan user dan `device_settings`.
- `microcontroller-service` menyimpan sensor reading ke MySQL.
- Manual watering sudah memakai RabbitMQ MQTT, bukan polling HTTP.
- Dokumentasi teknis lengkap ada di [`PROJECT-DOCUMENTATION.md`](./PROJECT-DOCUMENTATION.md).
