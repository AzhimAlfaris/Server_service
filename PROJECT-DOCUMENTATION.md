# Verdant Flow Service Server Documentation

Dokumen ini menjelaskan kondisi project saat ini berdasarkan source code yang benar-benar ada di repository. Stack utamanya adalah Spring Boot, Eureka, API Gateway, MySQL, RabbitMQ, JWT, SMTP Gmail, Prometheus/Grafana, dan ELK.

Project ini terdiri dari enam service utama:

- `eureka-server`
- `gateway-service`
- `microcontroller-service`
- `application-service`
- `user-service`
- `security-service`

Repository ini juga memuat:

- `docker-compose.yml`
- `init/`
- `elk-stack/`
- `prometheus.yml`
- `Kode ESP.txt`
- `Testing.txt`

## 1. Gambaran Umum

Arsitektur project saat ini bekerja seperti ini:

- `eureka-server` menjadi service registry.
- `gateway-service` menjadi entry point HTTP utama di port `8080`.
- `security-service` menangani register, login, cek email, reset password, dan JWT.
- `user-service` menyimpan user dan konfigurasi `device_settings`.
- `microcontroller-service` menerima data sensor dari ESP32, menyimpan ke MySQL, dan menjawab request sensor via RabbitMQ.
- `application-service` menjadi facade untuk dashboard web, pengiriman email notifikasi, dan command manual watering.

Catatan penting:

- `application-service` tidak punya database sendiri.
- `microcontroller-service` dan `user-service` masing-masing memakai database MySQL terpisah.
- Manual watering sekarang dikirim lewat RabbitMQ MQTT, bukan lewat HTTP bridge lama.
- ESP32 pada sketch saat ini mengirim sensor reading langsung ke `microcontroller-service` di port `8081`, sedangkan lookup `device-settings` dilakukan lewat `gateway-service` di port `8080`.

## 2. Teknologi

Versi dan stack yang dipakai di `pom.xml` dan konfigurasi project:

- Java 17
- Spring Boot 4.1.0
- Spring Cloud 2025.1.2
- Spring WebMVC
- Spring WebFlux
- Spring Data JPA
- Spring AMQP
- Spring Security
- Spring Mail
- JWT (`jjwt`)
- Lombok
- Actuator
- Micrometer Prometheus
- Logstash Logback Encoder

Komponen discovery dan routing:

- `eureka-server` memakai `@EnableEurekaServer`
- `gateway-service` memakai Spring Cloud Gateway berbasis WebFlux
- `microcontroller-service`, `application-service`, `user-service`, dan `security-service` terdaftar ke Eureka sebagai discovery client

## 3. Port dan Container

### Service utama

| Component | Direct Port | Notes |
| --- | ---: | --- |
| `eureka-server` | `8761` | Service registry |
| `gateway-service` | `8080` | HTTP entry point utama |
| `microcontroller-service` | `8081` | Sensor ingestion, query, device settings public |
| `application-service` | `8082` | Dashboard facade, email, manual command |
| `user-service` | `8083` | User and device settings persistence |
| `security-service` | `8084` | Register, login, reset password, JWT |

### Infrastruktur

| Component | Docker / Host Port | Notes |
| --- | ---: | --- |
| `verdant-userDb` | `3307:3306` | MySQL untuk `user-service` |
| `verdant-microcontrollerDb` | `3308:3306` | MySQL untuk `microcontroller-service` |
| RabbitMQ AMQP | `5672` | Queue utama service-to-service |
| RabbitMQ MQTT | `1883` | Dipakai ESP32 untuk subscribe command |
| RabbitMQ Management | `15672` | UI RabbitMQ |
| Prometheus | `9090` | Metrics scraping |
| Grafana | `3000` | Dashboard metrics |
| Elasticsearch | `9200` | Penyimpanan log |
| Logstash TCP inputs | `5000-5005` | Satu port per service |
| Kibana | `5601` | Visualisasi log |

Catatan container:

- Di dalam Docker, service lain tidak memakai `localhost` untuk akses antar-container.
- `user-service` memakai host `verdant-userDb`.
- `microcontroller-service` memakai host `verdant-microcontrollerDb`.
- Semua service berjalan di network `verdant-network`.

## 4. Konvensi Data

Project ini memakai konvensi berikut di hampir semua service:

- Email dinormalisasi ke huruf kecil.
- MAC address atau `address` dinormalisasi ke huruf besar.
- `soil_types` harus berisi tepat 3 item.
- `pot_index` dimulai dari `1`.
- `duration` pada command manual dihitung dalam detik.
- `history` sensor dibatasi per pot, bukan total seluruh reading.
- `soil_types[0]` merepresentasikan pot 1, `soil_types[1]` pot 2, dan `soil_types[2]` pot 3.

## 5. Tanggung Jawab Setiap Service

### `eureka-server`

- Menjadi service registry untuk semua service lain.
- Berjalan di port `8761`.
- Memakai `@EnableEurekaServer`.
- Tidak mendaftarkan dirinya sendiri ke Eureka.
- Mematikan self-preservation mode pada environment ini.

### `gateway-service`

- Menjadi pintu masuk HTTP utama di port `8080`.
- Merutekan request ke service tujuan dengan `lb://` lewat Eureka.
- Tidak menyimpan database sendiri.
- Berjalan sebagai Spring Cloud Gateway berbasis WebFlux.
- Mendaftarkan dirinya ke Eureka agar terlihat di registry.
- Mengirim log ke Logstash dan metrics ke Prometheus.

Route gateway yang aktif:

| Route | Target |
| --- | --- |
| `/auth/**` | `security-service` |
| `/api/auth/**` | `security-service` dengan `StripPrefix=1` |
| `/api/users/**` | `user-service` |
| `/api/sensor-data/**` | `application-service` |
| `/api/sensor-readings/**` | `microcontroller-service` |
| `/api/device-settings/**` | `microcontroller-service` |

### `microcontroller-service`

- Menerima data sensor dari ESP32.
- Menyimpan sensor reading ke MySQL.
- Memvalidasi address dan owner email ke `user-service`.
- Menyediakan endpoint `latest`, `history`, dan `device-settings` public.
- Menjawab request sensor via RabbitMQ request-reply.
- Mengirim notification event ke RabbitMQ setelah insert berhasil.
- Mengirim log ke Logstash dan metrics ke Prometheus.

### `application-service`

- Menyediakan endpoint `latest` dan `history` untuk dashboard.
- Menyediakan endpoint `device-settings` untuk dashboard.
- Menyediakan endpoint `command` untuk manual watering.
- Mengambil email dari Bearer token.
- Memvalidasi JWT secara lokal.
- Mengirim request sensor ke `microcontroller-service` melalui RabbitMQ.
- Menerima event notifikasi dan mengirim email otomatis.
- Tidak memakai database sendiri.

### `user-service`

- Menyimpan data user.
- Memastikan email user unik.
- Menyimpan dan membaca `device_settings`.
- Memastikan `address` unik untuk satu device setting.
- Menjadi backend internal untuk service lain.
- Mengirim log ke Logstash dan metrics ke Prometheus.

### `security-service`

- Register user ke `user-service`.
- Login user dan menghasilkan JWT.
- Mengecek apakah email sudah terdaftar.
- Reset password.
- Meng-hash password dengan BCrypt sebelum update ke `user-service`.
- Mengirim log ke Logstash dan metrics ke Prometheus.

## 6. Fitur Utama dan Alur Kerja

Bagian ini menjelaskan alur kerja fitur-fitur utama secara end-to-end, dari request yang dikirim client sampai service yang memprosesnya.

### 6.1 Register akun

Tujuan fitur ini adalah membuat akun baru untuk dipakai login ke dashboard.

Alur kerja:

1. Client mengirim request `POST /auth/register` ke `gateway-service` atau langsung ke `security-service`.
2. `gateway-service` meneruskan request ke `security-service`.
3. `security-service` memvalidasi format email dan password.
4. `security-service` meng-hash password dengan BCrypt.
5. `security-service` memanggil `user-service POST /api/users` dengan payload `email` dan password yang sudah di-hash.
6. `user-service` menyimpan data ke tabel `users`.
7. `security-service` mengembalikan response sukses ke client.

Catatan:

- Duplicate email akan ditolak oleh `user-service` dengan status `409 Conflict`.
- Password minimal 6 karakter.
- Flow ini dipakai oleh aplikasi mobile/web untuk onboarding user baru.

### 6.2 Login dan JWT

Tujuan fitur ini adalah menghasilkan token login yang dipakai pada request aplikasi.

Alur kerja:

1. Client mengirim request `POST /auth/login` dengan `email` dan `password`.
2. `security-service` mengambil data user dari `user-service GET /api/users/{email}`.
3. `security-service` membandingkan password input dengan password hash milik user.
4. Jika valid, `JwtService` membuat JWT dengan subject berupa email user.
5. `security-service` mengembalikan `AuthResponse` berisi token, tipe token, dan email.
6. Token dipakai client pada header `Authorization: Bearer <token>` saat memanggil `application-service`.

Catatan:

- `jwt.secret` di `security-service` dan `application-service` harus sama.
- JWT pada project ini berlaku sebagai identitas user untuk fitur sensor data dan device settings.

### 6.3 Cek akun

Tujuan fitur ini adalah mengecek apakah email sudah terdaftar di sistem.

Alur kerja:

1. Client mengirim request `GET /auth/exists?email=...`.
2. `security-service` meneruskan pengecekan ke `user-service GET /api/users/exists`.
3. `user-service` mengembalikan `true` jika email sudah ada, atau `false` jika belum terdaftar.
4. `security-service` meneruskan hasil tersebut ke client.

Fungsi ini dipakai untuk:

- validasi form register,
- validasi alur lupa password,
- pengecekan akun sebelum reset credential.

### 6.4 Ganti password

Tujuan fitur ini adalah memperbarui password user secara aman.

Alur kerja:

1. Client mengirim request `PUT /auth/reset-password` dengan `email`, `password`, dan `passwordAgain`.
2. `security-service` memeriksa apakah `password` dan `passwordAgain` sama.
3. `security-service` memastikan email memang terdaftar lewat `user-service`.
4. Password baru di-hash dengan BCrypt.
5. `security-service` memanggil `user-service PUT /api/users/update-password`.
6. `user-service` menyimpan password baru ke tabel `users`.
7. `security-service` mengembalikan response sukses.

Catatan:

- Jika email tidak ditemukan, response menjadi `404 Not Found`.
- Jika dua password tidak sama, response menjadi `400 Bad Request`.
- Endpoint ini adalah flow reset password yang dipakai client.

### 6.5 Mendaftarkan MAC address ESP32 ke akun

Fitur ini dipakai untuk menautkan perangkat ESP32 ke akun user.

Alur kerja dari sisi dashboard:

1. User login ke aplikasi dan mengirim Bearer token.
2. Client mengirim request `POST` atau `PUT /api/sensor-data/device-settings` ke `application-service`.
3. `application-service` mengambil email user dari JWT.
4. `application-service` membentuk `DeviceSettingsUpsertRequest` berisi email, address, dan `soil_types`.
5. `application-service` memanggil `user-service PUT /api/users/device-settings`.
6. `user-service` menyimpan binding email dan MAC address ke tabel `device_settings`.

Alur kerja dari sisi ESP32:

1. ESP32 menyambung ke Wi-Fi.
2. ESP32 mengambil MAC address lokal.
3. ESP32 memanggil `GET /api/device-settings/{address}` lewat `gateway-service`.
4. `gateway-service` meneruskan request ke `microcontroller-service`.
5. `microcontroller-service` mengambil data device settings dari `user-service`.
6. ESP32 menerima `email` owner dan `soil_types`, lalu menyimpannya untuk dipakai saat mengirim sensor reading.

Catatan:

- `soil_types` harus tepat 3 item karena perangkat ini dirancang untuk 3 pot.
- `device-settings` versi dashboard mengembalikan daftar milik user.
- `device-settings` versi ESP32 mengembalikan email owner dan `soil_types` untuk satu address.

### 6.6 Pengiriman data sensor dari ESP32

Fitur ini adalah jalur utama dari device ke backend untuk data kelembaban dan status pompa.

Alur kerja:

1. ESP32 membaca sensor satu per satu untuk 3 pot.
2. ESP32 membentuk payload `SensorReadingRequest` berisi `address`, `email`, dan daftar `pots`.
3. ESP32 mengirim request `POST /api/sensor-readings` ke `microcontroller-service`.
4. `microcontroller-service` menormalisasi `address` dan `email`.
5. `microcontroller-service` memvalidasi address ke `user-service`.
6. `microcontroller-service` memastikan email pada body cocok dengan pemilik address.
7. Data disimpan ke `sensor_readings` dan `pot_details` di MySQL.
8. Setelah insert sukses, `microcontroller-service` mem-publish event notifikasi ke RabbitMQ.
9. `application-service` menerima event tersebut melalui `SensorEmailListener`.
10. `SensorEmailService` mengirim email otomatis ke alamat user yang ada di payload.

Catatan:

- Jika address tidak terdaftar, request ditolak dengan `400 Bad Request`.
- Jika email tidak cocok dengan owner address, request ditolak dengan `403 Forbidden`.
- Flow ini dipakai untuk sinkronisasi data kelembaban dari ESP32 ke aplikasi dan email notifikasi.

### 6.7 Mengambil latest dan history sensor

Fitur ini dipakai dashboard untuk membaca data sensor user berdasarkan JWT.

Alur kerja:

1. Client login dan mendapatkan JWT dari `security-service`.
2. Client memanggil `GET /api/sensor-data/latest` atau `GET /api/sensor-data/history?limit=5`.
3. `application-service` mengambil email user dari JWT.
4. `application-service` membentuk `SensorQueryRequest` dengan `requestType` `LATEST` atau `HISTORY`.
5. Request dikirim ke RabbitMQ queue `sensor.request.queue`.
6. `microcontroller-service` menerima request melalui `SensorRequestListener`.
7. Untuk mode `LATEST`, service mengambil data terbaru per address milik email tersebut.
8. Untuk mode `HISTORY`, service mengambil riwayat per pot dengan batas `limit`.
9. Response JSON dikirim balik ke `application-service`.
10. `application-service` meneruskan hasilnya ke client.

Catatan:

- Email tidak diambil dari path URL pada `application-service`, tetapi dari JWT.
- `history` dibatasi per pot, bukan total seluruh record.
- Endpoint direct di `microcontroller-service` tetap tersedia untuk debugging jika diperlukan.

### 6.8 Command manual watering

Fitur ini dipakai untuk menyalakan pompa secara manual dari aplikasi.

Alur kerja:

1. Client mengirim request `POST /api/sensor-data/command`.
2. Request berisi `address`, `command`, `duration`, dan `pot_index`.
3. `application-service` mengambil email user dari JWT.
4. `application-service` memvalidasi bahwa address tersebut milik email user melalui `user-service`.
5. `application-service` menormalisasi MAC address menjadi format tanpa tanda `:`.
6. `DeviceCommandPublisherService` membuat routing key `device.command.<cleanMac>.pot.<potIndex>`.
7. Payload command dipublish ke RabbitMQ topic exchange `amq.topic`.
8. ESP32 yang subscribe ke topik `device/command/<cleanMac>/pot/+` menerima command tersebut.
9. `mqttCallback` pada sketch ESP32 memproses command `PUMP_ON_MANUAL`.
10. ESP32 membuka solenoid selama durasi yang dikirim, lalu menutupnya kembali.

Catatan:

- Jika address tidak terdaftar, response menjadi `400 Bad Request`.
- Jika address bukan milik email JWT, response menjadi `403 Forbidden`.
- `pot_index` minimal 1.
- Policy LVQ pada queue MQTT memastikan command terbaru yang paling relevan tetap tersisa jika device offline.

### 6.9 Monitoring, logging, dan service discovery

Fitur ini menjaga service mudah dipantau dan mudah dilacak saat terjadi error.

Alur kerja:

1. Semua service aplikasi mendaftarkan dirinya ke `eureka-server` saat startup.
2. `gateway-service` memakai Eureka untuk routing `lb://` ke service tujuan.
3. Semua service mengekspos `actuator/health`, `actuator/info`, dan `actuator/prometheus`.
4. Prometheus mengambil metrics dari semua service.
5. Grafana membaca metrics dari Prometheus untuk dashboard.
6. Log setiap service dikirim ke Logstash lewat TCP appender.
7. Logstash meneruskan log ke Elasticsearch.
8. Kibana dipakai untuk membuat data view dan membaca index log per service.

Port Logstash yang dipakai:

- `5000` untuk `microcontroller-service`
- `5001` untuk `application-service`
- `5002` untuk `user-service`
- `5003` untuk `security-service`
- `5004` untuk `eureka-server`
- `5005` untuk `gateway-service`

Catatan:

- Index log yang terbentuk memakai pola `*-logs-YYYY.MM.dd`.
- `gateway-service` dan `eureka-server` juga dipantau oleh Prometheus dan dapat dilihat di Grafana.
- `docker-compose.yml` menggunakan healthcheck agar service startup lebih stabil.

## 7. RabbitMQ Design

### Exchange dan queue

- `sensor.exchange` adalah `DirectExchange`.
- `sensor.request.queue` dipakai `microcontroller-service` untuk request sensor.
- `sensor.notification.queue` dipakai `application-service` untuk notification event.
- `amq.topic` dipakai untuk publish manual command ke ESP32.

### Routing key

- `sensor.request`
- `sensor.notification`
- `device.command.<cleanMac>.pot.<potIndex>`

### Bentuk command manual

Contoh:

```text
device.command.240AC4827D64.pot.1
```

Aturan format:

- `<cleanMac>` adalah MAC address tanpa tanda `:`.
- `potIndex` dimulai dari `1`.
- Payload JSON tetap membawa `email`, `address`, `command`, `duration`, dan `pot_index`.

### MQTT bridge dan LVQ

- ESP32 subscribe ke topik MQTT seperti `device/command/<cleanMac>/pot/+`.
- RabbitMQ MQTT plugin menjembatani topik MQTT ke routing key AMQP.
- `init/rabbitmq-start.sh` mengaktifkan plugin `rabbitmq_mqtt`.
- Script tersebut juga memasang policy `mqtt-lvq` untuk queue yang cocok dengan pattern `^mqtt-subscription-`.
- Policy ini menggunakan `x-max-length = 1` dan `x-overflow = drop-head`.
- Efeknya, jika device offline dan command menumpuk, hanya command terbaru yang tersisa.

### Request-reply sensor query

- `application-service` mengirim JSON request ke `sensor.exchange` dengan routing key `sensor.request`.
- `microcontroller-service` mengambil request dari `sensor.request.queue`.
- Listener mengembalikan JSON response sebagai string.

## 8. Database

### `microcontroller-service-db`

Tabel utama:

- `sensor_readings`
- `pot_details`

`sensor_readings` berisi:

- `id`
- `address`
- `email`
- `created_at`

`pot_details` berisi:

- `id`
- `reading_id`
- `pot_index`
- `sensor_value`
- `moisture_percent`
- `soil_condition`
- `action`
- `pump_duration`
- `timestamp_sensor`

Relasi:

- Satu `sensor_readings` punya banyak `pot_details`.
- `pot_details.reading_id` adalah foreign key ke `sensor_readings.id`.

### `user_service_db`

Tabel utama:

- `users`
- `device_settings`

`users` berisi:

- `id`
- `email`
- `password`

`device_settings` berisi:

- `id`
- `email`
- `address`
- `soil_type`
- `soil_types`

Catatan penting:

- `email` pada `users` bersifat unique.
- `address` pada `device_settings` bersifat unique.
- `soil_types` disimpan sebagai JSON string.
- `soil_type` dipakai sebagai nilai ringkas atau legacy field dari item pertama `soil_types`.
- `user-service` memakai `ddl-auto=update`, jadi schema runtime bisa ditambah oleh Hibernate walaupun file SQL awal masih baseline dump.

## 9. API Reference

### 9.1 `gateway-service`

Base URL:

```text
http://localhost:8080
```

Gateway meneruskan request ke route berikut:

| Route | Target |
| --- | --- |
| `/auth/**` | `security-service` |
| `/api/auth/**` | `security-service` |
| `/api/users/**` | `user-service` |
| `/api/sensor-data/**` | `application-service` |
| `/api/sensor-readings/**` | `microcontroller-service` |
| `/api/device-settings/**` | `microcontroller-service` |

Catatan:

- Semua service tetap bisa diakses langsung lewat port masing-masing.
- `gateway-service` hanya routing, bukan storage.

### 9.2 `microcontroller-service`

Base URL:

```text
http://localhost:8081
```

#### POST `/api/sensor-readings`

Menyimpan sensor reading baru dari ESP32.

Contoh request:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "email": "decalyps@gmail.com",
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

Catatan:

- `address`, `email`, dan `pots` wajib diisi.
- `address` yang tidak terdaftar akan ditolak dengan `400 Bad Request`.
- Jika email tidak cocok dengan pemilik address, request ditolak dengan `403 Forbidden`.
- Response sukses memakai HTTP `201 Created`.

#### GET `/api/sensor-readings/latest/{email}`

Mengambil data terbaru untuk semua device milik email tersebut.

Catatan:

- Response berbentuk `SensorQueryResponse`.
- Satu device response mewakili satu `address`.
- Setiap device berisi `pots` lengkap dari reading terbaru.

#### GET `/api/sensor-readings/history/{email}?limit=5`

Mengambil history sensor untuk semua device milik email tersebut.

Catatan:

- `limit` berlaku per pot, bukan total seluruh data.
- Response berbentuk `SensorQueryResponse`.
- History yang dikembalikan hanya menyimpan `potIndex`, `moisturePercent`, dan `soilCondition`.
- Field lain seperti `sensorValue`, `action`, `pumpDuration`, dan `timestampSensor` di history bernilai `null`.

#### GET `/api/device-settings/{address}`

Mengambil device settings publik untuk ESP32.

Contoh response:

```json
{
  "email": "decalyps@gmail.com",
  "soil_types": ["Kering", "Sedang", "Basah"]
}
```

Catatan:

- Endpoint ini tidak mengembalikan `id` atau `address`.
- Dipakai ESP32 untuk mengambil owner email dan soil types.

### 9.3 `application-service`

Base URL:

```text
http://localhost:8082
```

Semua endpoint `application-service` memakai Bearer token di header `Authorization`.

#### GET `/api/sensor-data/latest`

Mengambil data sensor terbaru berdasarkan email dari JWT.

#### GET `/api/sensor-data/history?limit=5`

Mengambil history data sensor berdasarkan email dari JWT.

Catatan:

- Email tidak diambil dari path URL.
- `limit` default `5`.

#### GET `/api/sensor-data/device-settings`

Mengambil seluruh device settings milik user yang sedang login.

Contoh response:

```json
{
  "status": "success",
  "message": "Daftar device settings berhasil diambil",
  "email": "decalyps@gmail.com",
  "devices": [
    {
      "address": "24:0A:C4:82:7D:64",
      "soil_types": ["Kering", "Sedang", "Basah"]
    }
  ]
}
```

#### POST atau PUT `/api/sensor-data/device-settings`

Menyimpan atau memperbarui device settings.

Contoh request:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "soil_types": ["Kering", "Sedang", "Basah"]
}
```

Catatan:

- `soil_types` harus tepat 3 item.
- Email diambil dari JWT.
- Response mengikuti `DeviceSettingsResponse` dengan `id`, `email`, `address`, dan `soil_types`.

#### POST `/api/sensor-data/command`

Mengirim command manual watering ke device tertentu dan pot tertentu.

Contoh request:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "command": "PUMP_ON_MANUAL",
  "duration": 5,
  "pot_index": 1
}
```

Contoh response:

```json
{
  "status": "success",
  "message": "Perintah manual watering berhasil dikirim",
  "email": "decalyps@gmail.com",
  "address": "24:0A:C4:82:7D:64",
  "routingKey": "device.command.240AC4827D64.pot.1"
}
```

Catatan:

- `duration` dihitung dalam detik.
- `pot_index` minimal `1`.
- Jika address tidak terdaftar, response menjadi `400 Bad Request`.
- Jika address bukan milik email JWT, response menjadi `403 Forbidden`.
- Command manual tidak lewat `microcontroller-service` lagi.

### 9.4 `user-service`

Base URL:

```text
http://localhost:8083
```

#### POST `/api/users`

Membuat user baru.

Catatan:

- Request body memakai `email` dan `password`.
- Service ini tidak meng-hash password.
- Response adalah entity `User` yang berisi `id`, `email`, dan `password`.
- Duplicate email akan menghasilkan `409 Conflict`.

#### PUT `/api/users/update-password`

Memperbarui password user.

Catatan:

- Password yang dikirim ke endpoint ini sudah di-hash oleh `security-service`.
- Jika user tidak ditemukan, response `404 Not Found`.

#### GET `/api/users/exists?email=user@example.com`

Mengecek apakah email sudah terdaftar.

Response:

```json
true
```

#### GET `/api/users/{email}`

Mengambil data user berdasarkan email.

#### PUT `/api/users/device-settings`

Menyimpan atau memperbarui device settings.

Contoh request:

```json
{
  "email": "decalyps@gmail.com",
  "address": "24:0A:C4:82:7D:64",
  "soil_types": ["Kering", "Sedang", "Basah"]
}
```

#### GET `/api/users/device-settings/{address}`

Mengambil satu device settings berdasarkan address.

#### GET `/api/users/device-settings?email=user@example.com`

Mengambil semua device settings milik email tertentu.

Catatan:

- Endpoint `user-service` adalah backend internal untuk service lain.
- Untuk kebutuhan user-facing, gunakan `security-service` dan `application-service` melalui gateway.

### 9.5 `security-service`

Base URL:

```text
http://localhost:8084
```

#### POST `/auth/register`

Mendaftarkan user baru melalui `user-service`.

Contoh request:

```json
{
  "email": "decalyps@gmail.com",
  "password": "password123"
}
```

Catatan:

- Password akan di-hash dengan BCrypt sebelum dikirim ke `user-service`.
- Response sukses adalah string `User registered successfully`.

#### POST `/auth/login`

Login user dan menghasilkan JWT.

Contoh response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "email": "decalyps@gmail.com"
}
```

#### GET `/auth/exists?email=user@example.com`

Mengecek apakah email ada di `user-service`.

#### PUT `/auth/reset-password`

Mereset password user.

Contoh request:

```json
{
  "email": "decalyps@gmail.com",
  "password": "password123",
  "passwordAgain": "password123"
}
```

Catatan:

- `password` dan `passwordAgain` harus sama.
- Password minimal 6 karakter.
- Password baru di-hash sebelum update ke `user-service`.
- Response sukses adalah string `Password berhasil direset`.

## 10. ESP32 dan MQTT

File referensi device saat ini ada di `Kode ESP.txt`.

Hal penting dari sketch tersebut:

- `MQTT_HOST` diset ke `192.168.1.15`.
- `MQTT_PORT` diset ke `1883`.
- `MQTT_USERNAME` dan `MQTT_PASSWORD` kosong pada konfigurasi default.
- Client ID MQTT memakai MAC address tanpa tanda titik dua.
- Device subscribe ke topik `device/command/<cleanMac>/pot/+`.
- Command MQTT yang dikenali sketch saat ini adalah `PUMP_ON_MANUAL`.
- `duration` dibaca dalam detik lalu diubah ke milidetik di ESP32.
- `pot_index` valid untuk 1 sampai 3.

Alur network dari sketch:

1. ESP32 menyambung ke Wi-Fi.
2. ESP32 memanggil `GET /api/device-settings/{mac}` lewat gateway di `8080`.
3. ESP32 menyimpan email owner dari response itu.
4. ESP32 mengirim sensor reading ke `POST /api/sensor-readings` di `8081`.
5. ESP32 mendengarkan command manual lewat MQTT pada port `1883`.

Format sensor payload dari sketch:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "email": "decalyps@gmail.com",
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

Catatan:

- `address` yang dipakai di HTTP adalah MAC asli dengan tanda `:`.
- Routing key command di RabbitMQ memakai MAC tanpa `:`.
- Karena device punya 3 kanal, `soil_types` di database dan UI harus selalu 3 item.

## 11. Observability

### Actuator

Semua service mengekspos endpoint berikut:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

### Logstash

Semua service mengirim log JSON ke Logstash lewat TCP appender.

Port input Logstash:

- `microcontroller-service` -> `5000`
- `application-service` -> `5001`
- `user-service` -> `5002`
- `security-service` -> `5003`
- `eureka-server` -> `5004`
- `gateway-service` -> `5005`

Index Elasticsearch yang dihasilkan:

- `microcontroller-service-logs-YYYY.MM.dd`
- `application-service-logs-YYYY.MM.dd`
- `user-service-logs-YYYY.MM.dd`
- `security-service-logs-YYYY.MM.dd`
- `eureka-server-logs-YYYY.MM.dd`
- `gateway-service-logs-YYYY.MM.dd`

### Prometheus dan Grafana

`prometheus.yml` saat ini scraping:

- `microcontroller-service:8081/actuator/prometheus`
- `application-service:8082/actuator/prometheus`
- `user-service:8083/actuator/prometheus`
- `security-service:8084/actuator/prometheus`
- `eureka-server:8761/actuator/prometheus`
- `gateway-service:8080/actuator/prometheus`

Grafana memakai Prometheus sebagai data source.

### Kibana

Buat data view berikut di Kibana:

- `microcontroller-service-logs-*`
- `application-service-logs-*`
- `user-service-logs-*`
- `security-service-logs-*`
- `eureka-server-logs-*`
- `gateway-service-logs-*`

## 12. Build, Run, dan Testing

### Build jar

Jalankan di masing-masing service:

```powershell
cd eureka-server
.\mvnw.cmd -DskipTests package
```

```powershell
cd gateway-service
.\mvnw.cmd -DskipTests package
```

```powershell
cd microcontroller-service
.\mvnw.cmd -DskipTests package
```

```powershell
cd application-service
.\mvnw.cmd -DskipTests package
```

```powershell
cd user-service
.\mvnw.cmd -DskipTests package
```

```powershell
cd security-service
.\mvnw.cmd -DskipTests package
```

### Jalankan lokal

```powershell
cd eureka-server
.\mvnw.cmd spring-boot:run
```

```powershell
cd gateway-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd microcontroller-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd application-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd user-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd security-service
.\mvnw.cmd spring-boot:run
```

### Jalankan Docker stack

```powershell
docker compose up -d --build
```

### Catatan testing

- Test profile menggunakan H2 in-memory database.
- Listener RabbitMQ pada test profile dimatikan.
- Jika ingin smoke test cepat, urutannya adalah register, login, simpan device settings, kirim sensor reading, lalu cek latest/history.

## 13. Catatan Penting

- `application-service` dan `security-service` harus memakai `jwt.secret` yang sama.
- `user-service` tidak meng-hash password sendiri.
- `history` sensor dibatasi per pot, bukan total seluruh record.
- `soil_types` harus tepat 3 item karena project ini memang dirancang untuk 3 pot per device.
- `device-settings` publik untuk ESP32 dan `device-settings` dashboard adalah dua jalur yang berbeda.
- `gateway-service` adalah entry point untuk client, tetapi sketch ESP32 saat ini masih mengirim sensor reading langsung ke `microcontroller-service`.
- `init/user_service_db.sql` adalah baseline schema. Runtime schema bisa bertambah karena `ddl-auto=update`.

## 14. Ringkasan Singkat

Project ini saat ini terdiri dari:

- `eureka-server` untuk service registry
- `gateway-service` untuk entry point HTTP utama di port `8080`
- `microcontroller-service` untuk menerima dan menyimpan data sensor ESP32
- `application-service` untuk dashboard, email notifikasi, dan command manual watering
- `user-service` untuk user dan device settings persistence
- `security-service` untuk register, login, reset password, dan JWT

Database dipisah menjadi dua MySQL container, observability sudah aktif lewat Actuator, Prometheus, Grafana, dan ELK, dan command manual watering sudah memakai RabbitMQ MQTT bridge.
