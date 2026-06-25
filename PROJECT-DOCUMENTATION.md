# Verdant Flow Service Server Documentation

Dokumen ini menjelaskan kondisi terbaru project backend monitoring sensor berbasis Spring Boot, MySQL, RabbitMQ, JWT, serta stack observability Prometheus/Grafana dan ELK.

Project ini sekarang terdiri dari empat service:

- `microcontroller-service`
- `application-service`
- `user-service`
- `security-service`

Selain itu, repository ini juga memuat:

- `docker-compose.yml` untuk menjalankan seluruh stack
- `init/` untuk inisialisasi database
- `prometheus.yml` untuk scraping metrics
- `elk-stack/` untuk konfigurasi Elasticsearch, Logstash, dan Kibana

## 1. Ringkasan Cepat

### Build dan test

```powershell
cd microcontroller-service
.\mvnw.cmd test
```

```powershell
cd application-service
.\mvnw.cmd test
```

```powershell
cd user-service
.\mvnw.cmd test
```

```powershell
cd security-service
.\mvnw.cmd test
```

### Build jar

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

### Jalankan seluruh stack Docker

```powershell
docker compose up -d --build
```

Jika ingin reset database container:

```powershell
docker compose down -v
docker compose up -d --build
```

## 2. Gambaran Umum

Project ini adalah backend untuk monitoring sensor berbasis ESP32.

Alur utamanya sekarang:

1. User mendaftar dan login melalui `security-service`.
2. `security-service` menyimpan user ke `user-service` dan menghasilkan JWT.
3. `application-service` menerima request `latest` dan `history` menggunakan Bearer token di header HTTP.
4. `application-service` memvalidasi token tersebut dan mengambil email dari token.
5. `application-service` meneruskan request data sensor ke `microcontroller-service` melalui RabbitMQ.
6. `microcontroller-service` menyimpan data sensor ke MySQL.
7. Saat `microcontroller-service` menerima data baru, email pada body request divalidasi dulu ke `user-service`.
8. Jika valid, `microcontroller-service` mem-publish event notifikasi ke RabbitMQ.
9. `application-service` menerima event tersebut dan mengirim email ke alamat user yang ada di payload.
10. Jika user lupa password, client dapat mengecek email lewat `security-service`, lalu melakukan reset password melalui `security-service`.
11. `security-service` memvalidasi input reset password, meng-hash password baru, lalu meneruskan update ke `user-service`.
12. Semua service mengirim log ke Logstash, lalu ke Elasticsearch, dan ditampilkan di Kibana.
13. Semua service juga mengekspor metrics ke Prometheus, lalu bisa dilihat di Grafana.

## 3. Arsitektur

### 3.1 `microcontroller-service`

Tanggung jawab:

- menerima data sensor dari ESP32
- menyimpan data ke MySQL
- memvalidasi email request POST ke `user-service`
- menyediakan endpoint `latest` dan `history` untuk debug/internal
- mengirim event notifikasi ke RabbitMQ setelah insert berhasil
- mengirim log aplikasi ke Logstash
- mengekspor metrics ke Prometheus

### 3.2 `application-service`

Tanggung jawab:

- menyediakan endpoint `latest` dan `history` untuk aplikasi
- mengambil email dari Bearer token
- memvalidasi token JWT secara lokal
- mengirim request data `LATEST` atau `HISTORY` melalui RabbitMQ
- menerima event notifikasi dari RabbitMQ
- mengirim email otomatis ke user
- tidak memakai database sendiri
- mengirim log aplikasi ke Logstash
- mengekspor metrics ke Prometheus

### 3.3 `user-service`

Tanggung jawab:

- menyimpan data user
- memastikan email unik
- menyediakan endpoint cek eksistensi email
- menyediakan endpoint ambil user berdasarkan email
- menyediakan endpoint update password dari `security-service`
- menyediakan log request ke Logstash
- mengekspor metrics ke Prometheus

### 3.4 `security-service`

Tanggung jawab:

- register user ke `user-service`
- login user dengan password yang di-hash
- menyediakan bridge untuk cek eksistensi email ke `user-service`
- menyediakan reset password flow
- membuat JWT token
- mengembalikan token untuk dipakai `application-service`
- menyediakan log request ke Logstash
- mengekspor metrics ke Prometheus

### 3.5 Komponen Infrastruktur

- MySQL untuk `microcontroller-service` dan `user-service`
- RabbitMQ sebagai message broker
- Gmail SMTP untuk email notifikasi
- Elasticsearch, Logstash, dan Kibana untuk observability log
- Prometheus dan Grafana untuk metrics monitoring

## 4. Teknologi

### 4.1 Stack utama

- Java 17
- Spring Boot 4.1.0
- Spring Web
- Spring AMQP
- Spring Mail
- Spring Data JPA
- Spring Security
- JWT
- Lombok
- Jackson Databind

### 4.2 Observability

Semua service sudah mengaktifkan:

- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- logging ke Logstash melalui `logback-spring.xml`

Endpoint actuator yang diekspos:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

## 5. Logging dan ELK

### 5.1 Alur log

Log dari semua service dikirim ke Logstash lewat TCP appender:

- `microcontroller-service` -> Logstash port `5000`
- `application-service` -> Logstash port `5001`
- `user-service` -> Logstash port `5002`
- `security-service` -> Logstash port `5003`

Logstash kemudian meneruskan data ke Elasticsearch, dan Kibana membaca index dari Elasticsearch.

### 5.2 File konfigurasi

- `application-service/src/main/resources/logback-spring.xml`
- `microcontroller-service/src/main/resources/logback-spring.xml`
- `user-service/src/main/resources/logback-spring.xml`
- `security-service/src/main/resources/logback-spring.xml`
- `elk-stack/logstash/pipeline/logstash.conf`

### 5.3 Perilaku log saat ini

Project saat ini sudah mencatat:

- request HTTP masuk
- request ke service layer
- response sukses
- error dan exception
- event startup dan shutdown container

Index log yang dipakai oleh Logstash saat ini:

- `microcontroller-service-logs-YYYY.MM.dd`
- `application-service-logs-YYYY.MM.dd`
- `user-service-logs-YYYY.MM.dd`
- `security-service-logs-YYYY.MM.dd`

## 6. Struktur Project

### 6.1 `application-service`

- `controller`
  - `SensorDataController`
- `service`
  - `SensorDataClientService`
  - `SensorEmailListener`
  - `SensorEmailService`
  - `JwtService`
- `config`
  - `RabbitMQConfig`
  - `HttpRequestLoggingFilter`
- `dto`
  - `SensorQueryRequest`
  - `SensorQueryResponse`
  - `SensorDeviceResponse`
  - `PotReadingResponse`
  - `PotReadingRequest`
  - `SensorReadingResponse`
- `exception`
  - `GlobalExceptionHandler`
  - `ResourceNotFoundException`

### 6.2 `microcontroller-service`

- `controller`
  - `SensorReadingController`
- `service`
  - `SensorReadingService`
  - `SensorRequestListener`
- `config`
  - `RabbitMQConfig`
  - `HttpRequestLoggingFilter`
- `client`
  - `UserServiceClient`
- `repository`
  - `SensorReadingRepository`
- `model`
  - `SensorReading`
  - `PotDetail`
- `dto`
  - `SensorReadingRequest`
  - `SensorReadingResponse`
  - `SensorQueryRequest`
  - `SensorQueryResponse`
  - `SensorDeviceResponse`
  - `PotReadingRequest`
  - `PotReadingResponse`
- `exception`
  - `GlobalExceptionHandler`
  - `ResourceNotFoundException`

### 6.3 `user-service`

- `controller`
  - `UserController`
- `service`
  - `UserService`
- `config`
  - `HttpRequestLoggingFilter`
- `repository`
  - `UserRepository`
- `model`
  - `User`
- `data`
  - `UserRequest`
- `handler`
  - `GlobalExceptionHandler`

### 6.4 `security-service`

- `controller`
  - `AuthController`
- `service`
  - `AuthService`
  - `JwtService`
- `config`
  - `SecurityConfig`
  - `WebClientConfig`
  - `HttpRequestLoggingFilter`
- `client`
  - `UserServiceClient`
- `data`
  - `AuthRequest`
  - `AuthResponse`
  - `UserRequest`
  - `UserResponse`
- `error`
  - `GlobalExceptionHandler`

## 7. Konfigurasi Runtime

### 7.1 Port default

- `microcontroller-service` -> `8081`
- `application-service` -> `8082`
- `user-service` -> `8083`
- `security-service` -> `8084`

### 7.2 `application-service/src/main/resources/application.properties`

Nilai penting yang dipakai:

- `spring.application.name=application-service`
- `server.port=8082`
- `jwt.secret=...`
- `spring.rabbitmq.host=${RABBITMQ_HOST:localhost}`
- `spring.rabbitmq.port=${RABBITMQ_PORT:5672}`
- `spring.rabbitmq.username=${RABBITMQ_USERNAME:user}`
- `spring.rabbitmq.password=${RABBITMQ_PASSWORD:password}`
- `app.rabbitmq.exchange=sensor.exchange`
- `app.rabbitmq.queue=sensor.request.queue`
- `app.rabbitmq.routing-key=sensor.request`
- `app.rabbitmq.notification-queue=sensor.notification.queue`
- `app.rabbitmq.notification-routing-key=sensor.notification`
- `app.mail.from=${MAIL_FROM:anasrudi048@gmail.com}`
- `spring.mail.host=${MAIL_HOST:smtp.gmail.com}`
- `spring.mail.port=${MAIL_PORT:587}`
- `spring.mail.username=${MAIL_USERNAME:anasrudi048@gmail.com}`
- `spring.mail.password=${MAIL_PASSWORD:...}`
- `management.endpoints.web.exposure.include=health,info,prometheus`
- `management.metrics.tags.application=${spring.application.name}`

Catatan:

- `application-service` membaca Bearer token dari header `Authorization`.
- Email untuk request `latest/history` diambil dari token, bukan dari path URL.

### 7.3 `microcontroller-service/src/main/resources/application.properties`

Nilai penting yang dipakai:

- `spring.application.name=microcontroller-service`
- `server.port=8081`
- `userservice.base-url=${USERSERVICE_BASE_URL:http://localhost:8083}`
- `spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME_MICRO:microcontroller-service-db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta`
- `spring.datasource.username=${DB_USERNAME:root}`
- `spring.datasource.password=${DB_PASSWORD:root}`
- `spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver`
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.jpa.show-sql=true`
- `spring.jpa.properties.hibernate.format_sql=true`
- `spring.rabbitmq.host=${RABBITMQ_HOST:localhost}`
- `spring.rabbitmq.port=${RABBITMQ_PORT:5672}`
- `spring.rabbitmq.username=${RABBITMQ_USERNAME:user}`
- `spring.rabbitmq.password=${RABBITMQ_PASSWORD:password}`
- `app.rabbitmq.exchange=sensor.exchange`
- `app.rabbitmq.queue=sensor.request.queue`
- `app.rabbitmq.routing-key=sensor.request`
- `app.rabbitmq.notification-routing-key=sensor.notification`
- `management.endpoints.web.exposure.include=health,info,prometheus`
- `management.metrics.tags.application=${spring.application.name}`

### 7.4 `user-service/src/main/resources/application.properties`

Nilai penting yang dipakai:

- `spring.application.name=user-service`
- `server.port=8083`
- `spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME_USER:user_service_db}?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta`
- `spring.datasource.username=${DB_USERNAME:root}`
- `spring.datasource.password=${DB_PASSWORD:root}`
- `spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver`
- `spring.jpa.hibernate.ddl-auto=update`
- `spring.jpa.show-sql=true`
- `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect`
- `management.endpoints.web.exposure.include=health,info,prometheus`
- `management.metrics.tags.application=${spring.application.name}`

### 7.5 `security-service/src/main/resources/application.properties`

Nilai penting yang dipakai:

- `spring.application.name=security-service`
- `server.port=8084`
- `userservice.base-url=${USERSERVICE_BASE_URL:http://localhost:8083}`
- `jwt.secret=...`
- `jwt.expiration=86400000`
- `logstash.host=${LOGSTASH_HOST:localhost}`
- `management.endpoints.web.exposure.include=health,info,prometheus`
- `management.metrics.tags.application=${spring.application.name}`

## 8. Alur Data

### 8.1 Request data sensor dari aplikasi

1. Client mengirim request ke `application-service`.
2. Client menyertakan `Authorization: Bearer <token>` pada header HTTP.
3. `application-service` memvalidasi token JWT dan mengambil email dari token.
4. `SensorDataClientService` mengubah request menjadi JSON `SensorQueryRequest`.
5. Payload dikirim ke exchange `sensor.exchange` dengan routing key `sensor.request`.
6. `SensorRequestListener` di `microcontroller-service` menerima payload lewat queue `sensor.request.queue`.
7. Service membaca data dari database.
8. Response dikembalikan sebagai `SensorQueryResponse`.
9. `application-service` meneruskan response ke caller.

### 8.2 Notifikasi email

1. ESP32 mengirim data ke `microcontroller-service`.
2. `SensorReadingService` memvalidasi email ke `user-service`.
3. Jika email valid, data sensor disimpan ke MySQL.
4. Setelah insert berhasil, service mem-publish payload ke routing key `sensor.notification`.
5. `SensorEmailListener` di `application-service` menerima payload dari queue `sensor.notification.queue`.
6. `SensorEmailService` mengirim email ke alamat yang ada di data sensor.

### 8.3 Auth dan token

1. Client melakukan register ke `security-service`.
2. `security-service` meneruskan data user ke `user-service`.
3. User dibuat dengan email unik dan password yang sudah di-hash.
4. Client melakukan login ke `security-service`.
5. `security-service` mengambil user dari `user-service`, memvalidasi password, lalu mengembalikan JWT.
6. JWT dipakai client saat memanggil endpoint `latest/history` di `application-service`.
7. Jika client lupa password, client dapat memanggil `GET /auth/exists` untuk cek email dan `PUT /auth/reset-password` untuk memperbarui password.
8. `security-service` akan meneruskan password baru yang sudah di-encode ke `user-service` melalui endpoint internal `PUT /api/users/update-password`.

## 9. RabbitMQ Design

### 9.1 Exchange

- `sensor.exchange`

### 9.2 Queue

- `sensor.request.queue`
- `sensor.notification.queue`

### 9.3 Routing key

- `sensor.request`
- `sensor.notification`

### 9.4 Perilaku penting

- Exchange yang dipakai adalah `DirectExchange`.
- Request history memakai `requestType=HISTORY` dan `limit`.
- Request latest memakai `requestType=LATEST`.
- Jika request type tidak ada, listener di `microcontroller-service` default ke `LATEST`.
- Jika response RabbitMQ kosong, `application-service` melempar `ResourceNotFoundException`.

## 10. Database

### 10.1 Database `microcontroller-service-db`

Database ini dipakai oleh `microcontroller-service`.

Struktur terbaru memakai relasi one-to-many:

- `sensor_readings`
- `pot_details`

Skema SQL inti:

```sql
CREATE TABLE sensor_readings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    address VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE pot_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reading_id BIGINT NOT NULL,
    pot_index INT NOT NULL,
    sensor_value VARCHAR(255) NOT NULL,
    moisture_percent VARCHAR(255) NOT NULL,
    soil_condition VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    pump_duration VARCHAR(255) NOT NULL,
    timestamp_sensor VARCHAR(255) NOT NULL,
    FOREIGN KEY (reading_id) REFERENCES sensor_readings(id) ON DELETE CASCADE
);
```

### 10.2 Database `user_service_db`

Database ini dipakai oleh `user-service`.

Struktur inti:

- `users`

Kolom utama:

- `id`
- `email`
- `password`

Email dibuat unik melalui constraint di entity dan validasi service.

### 10.3 Catatan database Docker

Di Docker saat ini, `microcontroller-service` dan `user-service` memakai satu container MySQL yang sama, yaitu `verdant-mysql`, dengan dua database:

- `microcontroller-service-db`
- `user_service_db`

File inisialisasi yang dipasang:

- `init/microcontroller-service-db.sql`
- `init/user_service_db.sql`

### 10.4 Catatan database lokal

Untuk menjalankan `user-service` secara lokal di Windows/XAMPP, konfigurasi default masih memakai:

- host `localhost`
- username `root`
- password `root`

Jika Anda memakai XAMPP lokal, pastikan database `user_service_db` sudah tersedia.

## 11. Endpoint API

## 11.1 `microcontroller-service`

Base URL:

```text
http://localhost:8081
```

### POST `/api/sensor-readings`

Menyimpan data sensor baru.

Request body:

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

Response:

- HTTP `201 Created`
- body `SensorReadingResponse`

Catatan:

- `address` pada body divalidasi ke `user-service` melalui tabel `device_settings`.
- Jika `address` tidak ditemukan, request ditolak dengan `400 Bad Request`.
- Jika `email` pada body tidak cocok dengan owner `address`, request ditolak dengan `403 Forbidden`.

Contoh response sukses:

```json
{
  "id": 1,
  "address": "24:0A:C4:82:7D:64",
  "email": "user@example.com",
  "createdAt": "2026-06-14T17:30:00",
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

### GET `/api/sensor-readings/latest/{email}`

Mengambil data terbaru untuk semua perangkat yang dimiliki email tersebut.

Response:

- HTTP `200 OK`
- body `SensorQueryResponse`

### GET `/api/sensor-readings/history/{email}?limit=5`

Mengambil riwayat data sensor untuk email tersebut.

Response:

- HTTP `200 OK`
- body `SensorQueryResponse`

### GET `/api/device-settings/{address}`

Endpoint publik untuk ESP32 yang mengambil konfigurasi perangkat berdasarkan MAC address.

Response:

- HTTP `200 OK`
- body:

```json
{
  "email": "user@example.com",
  "soil_type": "Clay"
}
```

Jika address tidak terdaftar:

- HTTP `404 Not Found`

## 11.2 `application-service`

Base URL:

```text
http://localhost:8082
```

### GET `/api/sensor-data/latest`

Meminta data terbaru dari `microcontroller-service`.

Header:

- `Authorization: Bearer <token>`

Response body:

```json
{
  "status": "success",
  "message": "Data sensor terbaru dari semua perangkat berhasil diambil",
  "email": "user@gmail.com",
  "devices": []
}
```

### GET `/api/sensor-data/history?limit=5`

Meminta riwayat data sensor dari `microcontroller-service`.

Header:

- `Authorization: Bearer <token>`

Response body:

```json
{
  "status": "success",
  "message": "Riwayat data sensor dari semua perangkat berhasil diambil",
  "email": "user@gmail.com",
  "devices": []
}
```

Catatan:

- `application-service` tidak lagi memakai email di path URL untuk `latest/history`.
- Email diambil dari token JWT yang divalidasi di service ini.

### POST atau PUT `/api/sensor-data/device-settings`

Menyimpan atau memperbarui konfigurasi device untuk user yang sedang login.

Header:

- `Authorization: Bearer <token>`

Request body:

```json
{
  "address": "24:0A:C4:82:7D:64",
  "soil_type": "Clay"
}
```

Behavior:

- Email diambil dari JWT authentication context.
- `application-service` meneruskan `email`, `address`, dan `soil_type` ke `user-service`.
- `address` yang sama hanya boleh dimiliki satu email.

Response contoh:

```json
{
  "id": 1,
  "email": "user@example.com",
  "address": "24:0A:C4:82:7D:64",
  "soil_type": "Clay"
}
```

## 11.3 `user-service`

Base URL:

```text
http://localhost:8083
```

### POST `/api/users`

Membuat user baru.

Request body:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

### GET `/api/users/exists?email=user@example.com`

Mengecek apakah email sudah terdaftar.

Response:

```json
true
```

### PUT `/api/users/update-password`

Memperbarui password user berdasarkan email.

Request body:

```json
{
  "email": "user@example.com",
  "password": "hashed-or-updated-password"
}
```

Catatan:

- Endpoint ini dipakai internal oleh `security-service`.
- Password yang dikirim ke endpoint ini sudah dalam bentuk hasil encode dari `security-service`.

### GET `/api/users/{email}`

Mengambil data user berdasarkan email.

Response:

- HTTP `200 OK` bila ditemukan
- HTTP `404 Not Found` bila tidak ditemukan

### PUT `/api/users/device-settings`

Menyimpan atau memperbarui device settings untuk owner address tertentu.

Request body:

```json
{
  "email": "user@example.com",
  "address": "24:0A:C4:82:7D:64",
  "soil_type": "Clay"
}
```

Response:

```json
{
  "id": 1,
  "email": "user@example.com",
  "address": "24:0A:C4:82:7D:64",
  "soil_type": "Clay"
}
```

### GET `/api/users/device-settings/{address}`

Mengambil konfigurasi device dan email owner berdasarkan MAC address.

Response:

```json
{
  "id": 1,
  "email": "user@example.com",
  "address": "24:0A:C4:82:7D:64",
  "soil_type": "Clay"
}
```

Catatan:

- `address` di tabel `device_settings` memiliki unique constraint.
- Satu MAC address hanya boleh terikat ke satu email.

## 11.4 `security-service`

Base URL:

```text
http://localhost:8084
```

### POST `/auth/register`

Mendaftarkan user baru melalui `user-service`.

Request body:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Response:

- HTTP `201 Created`
- body string `User registered successfully`

### POST `/auth/login`

Login user dan menghasilkan token JWT.

Request body:

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Response:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "email": "user@example.com"
}
```

### GET `/auth/exists?email=user@example.com`

Mengecek apakah email ada di `user-service`.

Response:

```json
true
```

### PUT `/auth/reset-password`

Memvalidasi password baru, meng-hash password, lalu meneruskan update password ke `user-service`.

Request body:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "passwordAgain": "password123"
}
```

Response:

- HTTP `200 OK`
- body string `Password berhasil direset`

Catatan:

- Endpoint ini ditujukan untuk client.
- `security-service` akan menolak request jika `password` dan `passwordAgain` berbeda.
- Jika email tidak ditemukan, response akan `404 Not Found`.

## 12. Docker Compose Terbaru

`docker-compose.yml` saat ini menjalankan service berikut:

- `verdant-mysql`
- `rabbitmq`
- `microcontroller-service`
- `application-service`
- `user-service`
- `security-service`
- `prometheus`
- `grafana`
- `elasticsearch`
- `logstash`
- `kibana`

### 12.1 Port di Docker

- MySQL -> `3306`
- RabbitMQ AMQP -> `5672`
- RabbitMQ UI -> `15672`
- `microcontroller-service` -> `8081`
- `application-service` -> `8082`
- `user-service` -> `8083`
- `security-service` -> `8084`
- Prometheus -> `9090`
- Grafana -> `3000`
- Elasticsearch -> `9200`
- Logstash TCP -> `5000`, `5001`, `5002`, `5003`
- Kibana -> `5601`

### 12.2 Network

Semua container menggunakan network:

- `verdant-network`

### 12.3 Environment variable di Docker

#### `microcontroller-service`

- `DB_HOST=verdant-mysql`
- `DB_PORT=3306`
- `DB_NAME_MICRO=microcontroller-service-db`
- `DB_USERNAME=root`
- `DB_PASSWORD=root`
- `USERSERVICE_BASE_URL=http://user-service:8083`
- `RABBITMQ_HOST=rabbitmq`
- `RABBITMQ_PORT=5672`
- `RABBITMQ_USERNAME=user`
- `RABBITMQ_PASSWORD=password`
- `LOGSTASH_HOST=verdant-logstash`

#### `application-service`

- `RABBITMQ_HOST=rabbitmq`
- `RABBITMQ_PORT=5672`
- `RABBITMQ_USERNAME=user`
- `RABBITMQ_PASSWORD=password`
- `MAIL_HOST=smtp.gmail.com`
- `MAIL_PORT=587`
- `MAIL_USERNAME=anasrudi048@gmail.com`
- `MAIL_PASSWORD=App Password Gmail`
- `MAIL_FROM=anasrudi048@gmail.com`
- `LOGSTASH_HOST=verdant-logstash`

#### `user-service`

- `DB_HOST=verdant-mysql`
- `DB_PORT=3306`
- `DB_NAME_USER=user_service_db`
- `DB_USERNAME=root`
- `DB_PASSWORD=root`
- `LOGSTASH_HOST=verdant-logstash`

#### `security-service`

- `USERSERVICE_BASE_URL=http://user-service:8083`
- `JWT_SECRET=...`
- `JWT_EXPIRATION=86400000`
- `LOGSTASH_HOST=verdant-logstash`

### 12.4 Catatan penting Docker

- Di dalam container, `localhost` tidak dipakai untuk koneksi antarservice.
- Gunakan nama service container seperti `verdant-mysql`, `rabbitmq`, `user-service`, dan `verdant-logstash`.
- `docker-compose.yml` memasang healthcheck untuk MySQL dan RabbitMQ.
- Dockerfile untuk semua service Java menggunakan image runtime JRE dan menyalin file `.jar` dari folder `target/`.
- Proses build Java dijalankan di luar Docker memakai Maven Wrapper, lalu container dijalankan dari artifact yang sudah jadi.

## 13. Monitoring

### 13.1 Prometheus

`prometheus.yml` saat ini scraping:

- `microcontroller-service:8081/actuator/prometheus`
- `application-service:8082/actuator/prometheus`
- `user-service:8083/actuator/prometheus`
- `security-service:8084/actuator/prometheus`

### 13.2 Grafana

Grafana tersedia di:

```text
http://localhost:3000
```

### 13.3 Kibana

Kibana tersedia di:

```text
http://localhost:5601
```

Log aplikasi yang dikirim oleh semua service akan muncul di Kibana setelah masuk ke Elasticsearch melalui Logstash.

#### Data view Kibana

Buat empat data view atau index pattern berikut:

- `microcontroller-service-logs-*`
- `application-service-logs-*`
- `user-service-logs-*`
- `security-service-logs-*`

Langkah singkat:

1. Buka Kibana di `http://localhost:5601`.
2. Masuk ke menu **Stack Management**.
3. Pilih **Data Views**.
4. Buat data view sesuai nama index di atas.
5. Gunakan field waktu `@timestamp` jika diminta.
6. Simpan.

### 13.4 Actuator

Semua service mengekspos metrics ke:

- `http://localhost:8081/actuator/health`
- `http://localhost:8081/actuator/info`
- `http://localhost:8081/actuator/prometheus`
- `http://localhost:8082/actuator/health`
- `http://localhost:8082/actuator/info`
- `http://localhost:8082/actuator/prometheus`
- `http://localhost:8083/actuator/health`
- `http://localhost:8083/actuator/info`
- `http://localhost:8083/actuator/prometheus`
- `http://localhost:8084/actuator/health`
- `http://localhost:8084/actuator/info`
- `http://localhost:8084/actuator/prometheus`

## 14. Catatan Testing

Jika ingin validasi cepat setelah menjalankan service:

1. Register user lewat `security-service`.
2. Login user lewat `security-service` dan ambil token.
3. POST data ke `microcontroller-service`.
4. GET `latest` dan `history` dari `microcontroller-service`.
5. GET `latest` dan `history` dari `application-service` dengan Bearer token.
6. Cek `actuator/health`.
7. Cek `actuator/prometheus`.
8. Cek log di Kibana untuk memastikan request aplikasi masuk ke Elasticsearch.

## 15. Ringkasan Singkat

Project ini sekarang terdiri dari:

- `microcontroller-service` untuk menerima dan menyimpan data sensor ESP32
- `application-service` untuk mengambil data sensor lewat RabbitMQ dan mengirim email otomatis
- `user-service` untuk menyimpan user dan validasi email
- `security-service` untuk register/login dan penerbitan JWT token

Database dipakai oleh `microcontroller-service` dan `user-service`, sementara observability sudah tersedia lewat Actuator, Prometheus, Grafana, dan ELK stack.
