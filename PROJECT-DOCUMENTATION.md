# Verdant Flow Service Server Documentation

Dokumen ini menjelaskan arsitektur, alur data, konfigurasi database, RabbitMQ, email, dan contoh rencana Docker untuk dua service Spring Boot berikut:

- `application-service`
- `microcontroller-service`

## 1. Gambaran Umum

Project ini adalah backend untuk sistem monitoring sensor berbasis ESP32.

Alurnya dibagi menjadi dua:

1. Aplikasi meminta data sensor ke `application-service`.
2. Saat data sensor baru masuk ke `microcontroller-service`, service tersebut menyimpan data ke database dan mengirim notifikasi email otomatis ke user.

RabbitMQ dipakai untuk komunikasi antarservice.

## 2. Arsitektur

### 2.1 Service

- `microcontroller-service`
  - Menerima data sensor dari ESP32
  - Menyimpan data ke MySQL
  - Menyediakan endpoint history/latest untuk debug langsung
  - Mengirim event notifikasi ke RabbitMQ setelah data baru tersimpan

- `application-service`
  - Menyediakan endpoint untuk aplikasi
  - Mengirim request data latest/history ke `microcontroller-service` melalui RabbitMQ
  - Menerima event notifikasi dari RabbitMQ
  - Mengirim email ke alamat user yang ada di data sensor

### 2.2 Komponen Infrastruktur

- MySQL untuk database masing-masing service
- RabbitMQ sebagai message broker
- Gmail SMTP untuk pengiriman email otomatis

## 3. Alur Data

### 3.1 Alur Request Data oleh Aplikasi

1. Aplikasi memanggil endpoint `application-service`.
2. `application-service` membuat request JSON berisi:
   - `microcontrollerId`
   - `requestType` = `LATEST` atau `HISTORY`
   - `limit` jika history
3. Request dikirim ke RabbitMQ.
4. `microcontroller-service` menerima request lewat `@RabbitListener`.
5. `microcontroller-service` mengambil data dari database.
6. Response dikirim kembali melalui RabbitMQ.
7. `application-service` menerima response dan meneruskannya ke aplikasi.

### 3.2 Alur Notifikasi Email

1. ESP32 mengirim data ke `microcontroller-service`.
2. `microcontroller-service` menyimpan data ke database.
3. Setelah data tersimpan, service ini mem-publish event notifikasi ke RabbitMQ.
4. `application-service` menerima event tersebut.
5. `application-service` mengirim email ke alamat user yang ada pada data sensor.

Perbedaan penting:

- Request data ke aplikasi: hanya saat aplikasi meminta data.
- Email notifikasi: otomatis setiap ada data baru yang berhasil masuk ke `microcontroller-service`.

## 4. Teknologi yang Dipakai

- Java 17
- Spring Boot 4.1.0
- Spring Web
- Spring Data JPA
- Spring AMQP
- Spring Mail
- Lombok
- MySQL Connector
- Jackson Databind
- H2 untuk test

## 5. Struktur Project

### 5.1 `application-service`

- `controller`
  - `SensorDataController`
- `service`
  - `SensorDataClientService`
  - `SensorEmailListener`
  - `SensorEmailService`
- `config`
  - `RabbitMQConfig`
- `dto`
  - `SensorQueryRequest`
  - `SensorQueryResponse`
  - `SensorReadingResponse`
- `exception`
  - `GlobalExceptionHandler`
  - `ResourceNotFoundException`

### 5.2 `microcontroller-service`

- `controller`
  - `SensorReadingController`
- `service`
  - `SensorReadingService`
  - `SensorRequestListener`
- `config`
  - `RabbitMQConfig`
- `repository`
  - `SensorReadingRepository`
- `model`
  - `SensorReading`
- `dto`
  - `SensorReadingRequest`
  - `SensorReadingResponse`
  - `SensorQueryRequest`
  - `SensorQueryResponse`
- `exception`
  - `GlobalExceptionHandler`
  - `ResourceNotFoundException`

## 6. Database Design

## 6.1 Database `microcontroller_service_db`

Tabel: `sensor_readings`

Kolom:

- `id` - primary key, auto increment
- `microcontroller_id` - ID unik perangkat
- `email` - email user tujuan notifikasi
- `sensor_value` - nilai mentah sensor
- `moisture_percent` - kelembaban dalam persen
- `soil_condition` - status kondisi tanah
- `action` - tindakan sistem/pompa
- `pump_duration` - durasi aktif pompa
- `timestamp_sensor` - waktu dari sisi ESP32
- `created_at` - waktu data masuk ke server

Contoh SQL jika ingin membuat manual:

```sql
CREATE TABLE sensor_readings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    microcontroller_id VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    sensor_value VARCHAR(255) NOT NULL,
    moisture_percent VARCHAR(255) NOT NULL,
    soil_condition VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    pump_duration VARCHAR(255) NOT NULL,
    timestamp_sensor VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 6.2 Database `application_service_db`

Saat ini `application-service` tidak lagi menyimpan user table.

Jadi database ini secara praktis dipakai untuk struktur Hibernate dan kebutuhan internal service, tetapi fitur utama project berada di `microcontroller_service_db`.

Jika Anda ingin, database ini bisa tetap dibiarkan kosong, atau Anda bisa biarkan Hibernate membuat tabel pendukung jika nanti ada kebutuhan tambahan.

## 7. RabbitMQ Design

RabbitMQ yang dipakai:

- Host: `localhost` saat local run
- Port: `5672`
- Management UI: `http://localhost:15672`

Credential default dari container yang pernah Anda pakai:

- Username: `user`
- Password: `password`

### 7.1 Exchange

- `sensor.exchange`

### 7.2 Queue

- `sensor.request.queue`
  - dipakai untuk request data dari `application-service` ke `microcontroller-service`

- `sensor.notification.queue`
  - dipakai untuk notifikasi email dari `microcontroller-service` ke `application-service`

### 7.3 Routing Key

- `sensor.request`
- `sensor.notification`

## 8. Endpoint API

## 8.1 `microcontroller-service`

Base URL:

```text
http://localhost:8081
```

### POST `/api/sensor-readings`

Simpan data sensor baru.

Contoh request:

```json
{
  "microcontrollerId": "ESP32-001",
  "email": "user@example.com",
  "sensorValue": "742",
  "moisturePercent": "68",
  "soilCondition": "Normal",
  "action": "Pump ON",
  "pumpDuration": "5 seconds",
  "timestampSensor": "2026-06-14 17:30:00"
}
```

### GET `/api/sensor-readings/latest/{microcontrollerId}`

Mengambil data terbaru berdasarkan `microcontrollerId`.

Contoh:

```text
http://localhost:8081/api/sensor-readings/latest/ESP32-001
```

### GET `/api/sensor-readings/history/{microcontrollerId}?limit=10`

Mengambil riwayat data.

Contoh:

```text
http://localhost:8081/api/sensor-readings/history/ESP32-001?limit=10
```

## 8.2 `application-service`

Base URL:

```text
http://localhost:8082
```

### GET `/api/sensor-data/latest/{microcontrollerId}`

Meminta data terbaru dari `microcontroller-service`.

Contoh:

```text
http://localhost:8082/api/sensor-data/latest/ESP32-001
```

### GET `/api/sensor-data/history/{microcontrollerId}?limit=10`

Meminta riwayat data dari `microcontroller-service`.

Contoh:

```text
http://localhost:8082/api/sensor-data/history/ESP32-001?limit=10
```

## 9. Konfigurasi Lokal Tanpa Docker

### 9.1 MySQL via XAMPP

Jika Anda masih memakai XAMPP default:

- Jalankan Apache dan MySQL dari XAMPP Control Panel
- Buka phpMyAdmin
- Buat database:
  - `microcontroller_service_db`
  - `application_service_db`

Karena `spring.jpa.hibernate.ddl-auto=update`, tabel akan dibuat otomatis saat service dijalankan.

### 9.2 RabbitMQ

Container RabbitMQ yang pernah Anda jalankan:

```bash
docker run --detach --hostname my-rabbit --name some-rabbit --env RABBITMQ_DEFAULT_USER=user --env RABBITMQ_DEFAULT_PASS=password --publish 15672:15672 --publish 5672:5672 rabbitmq:management
```

### 9.3 Jalankan Service

Di folder masing-masing service:

```bash
./mvnw spring-boot:run
```

atau di Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## 10. Konfigurasi untuk Docker

Bagian ini penting karena di dalam container, `localhost` berarti container itu sendiri, bukan host machine.

Artinya:

- kalau database ada di container MySQL, pakai host `mysql`
- kalau RabbitMQ ada di container RabbitMQ, pakai host `rabbitmq`
- kalau service jalan di container, jangan hardcode `localhost` untuk koneksi antarcontainer

### 10.1 Environment Variable yang Dipakai

Saya sudah ubah konfigurasi project supaya bisa dibaca dari environment variable.

#### `application-service`

- `DB_HOST`
- `DB_PORT`
- `DB_NAME_APPLICATION`
- `DB_USERNAME`
- `DB_PASSWORD`
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`

#### `microcontroller-service`

- `DB_HOST`
- `DB_PORT`
- `DB_NAME_MICRO`
- `DB_USERNAME`
- `DB_PASSWORD`
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`

### 10.2 Contoh `docker-compose.yml`

Contoh berikut bisa Anda jadikan acuan:

```yaml
services:
  mysql:
    image: mysql:8.4
    container_name: verdant-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: application_service_db
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  rabbitmq:
    image: rabbitmq:management
    container_name: verdant-rabbitmq
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: user
      RABBITMQ_DEFAULT_PASS: password
    ports:
      - "5672:5672"
      - "15672:15672"

  microcontroller-service:
    build:
      context: ./microcontroller-service
    container_name: microcontroller-service
    restart: unless-stopped
    depends_on:
      - mysql
      - rabbitmq
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME_MICRO: microcontroller_service_db
      DB_USERNAME: root
      DB_PASSWORD: root
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: user
      RABBITMQ_PASSWORD: password
    ports:
      - "8081:8081"

  application-service:
    build:
      context: ./application-service
    container_name: application-service
    restart: unless-stopped
    depends_on:
      - mysql
      - rabbitmq
      - microcontroller-service
    environment:
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME_APPLICATION: application_service_db
      DB_USERNAME: root
      DB_PASSWORD: root
      RABBITMQ_HOST: rabbitmq
      RABBITMQ_PORT: 5672
      RABBITMQ_USERNAME: user
      RABBITMQ_PASSWORD: password
      MAIL_HOST: smtp.gmail.com
      MAIL_PORT: 587
      MAIL_USERNAME: anasrudi048@gmail.com
      MAIL_PASSWORD: isi_app_password_di_sini
      MAIL_FROM: anasrudi048@gmail.com
    ports:
      - "8082:8082"

volumes:
  mysql_data:
```

### 10.3 Kenapa `localhost` Tidak Dipakai di Docker

Kalau service berjalan di container, maka:

- `localhost:3306` berarti MySQL di container service itu sendiri
- `localhost:5672` berarti RabbitMQ di container service itu sendiri

Jadi di Docker, host harus memakai nama service:

- `mysql`
- `rabbitmq`

### 10.4 Catatan MySQL di Docker

Kalau Anda memakai MySQL di Docker:

- Nama database otomatis dibuat dari `MYSQL_DATABASE`
- Jika ingin dua database, Anda bisa:
  - buat satu container MySQL lalu jalankan SQL init untuk membuat dua database
  - atau pakai satu database saja dan atur ulang nama DB sesuai kebutuhan

Contoh SQL init:

```sql
CREATE DATABASE IF NOT EXISTS application_service_db;
CREATE DATABASE IF NOT EXISTS microcontroller_service_db;
```

Kalau pakai init script, script itu biasanya diletakkan di folder init container MySQL.

## 11. Contoh Dockerfile

### 11.1 `application-service/Dockerfile`

```dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/application-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 11.2 `microcontroller-service/Dockerfile`

```dockerfile
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY target/microcontroller-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Sebelum `docker compose up`, build dulu:

```bash
./mvnw clean package
```

## 12. Konfigurasi Gmail

Untuk email otomatis:

- Pengirim: `anasrudi048@gmail.com`
- SMTP host: `smtp.gmail.com`
- SMTP port: `587`

Hal yang harus diisi manual:

- `spring.mail.password` di [`application-service/src/main/resources/application.properties`](./application-service/src/main/resources/application.properties)

Gunakan **App Password Gmail**, bukan password akun biasa.

## 13. Catatan Penting Tentang Database

Karena project menggunakan:

```properties
spring.jpa.hibernate.ddl-auto=update
```

maka Hibernate akan:

- membuat tabel jika belum ada
- menyesuaikan kolom yang bisa di-update

Namun, jika tabel lama sudah terlanjur ada, perubahan kolom kadang perlu disesuaikan manual lewat SQL.

### Contoh masalah umum

- Kolom `created_at` tidak muncul
  - biasanya karena tabel sudah terlanjur dibuat sebelumnya
  - solusi: drop tabel dan biarkan Hibernate membuat ulang, atau gunakan `ALTER TABLE`

## 14. Contoh Data

### 14.1 POST ke `microcontroller-service`

```json
{
  "microcontrollerId": "ESP32-001",
  "email": "user@example.com",
  "sensorValue": "742",
  "moisturePercent": "68",
  "soilCondition": "Normal",
  "action": "Pump ON",
  "pumpDuration": "5 seconds",
  "timestampSensor": "2026-06-14 17:30:00"
}
```

### 14.2 Request dari `application-service`

Latest:

```text
GET http://localhost:8082/api/sensor-data/latest/ESP32-001
```

History:

```text
GET http://localhost:8082/api/sensor-data/history/ESP32-001?limit=10
```

## 15. Testing Status

Kedua service sudah pernah diuji dengan Maven test dan build-nya lolos saat konfigurasi terakhir saya cek.

## 16. Ringkasan Singkat untuk Diskusi dengan AI Lain

Kalimat ringkas yang bisa Anda pakai:

> Project ini terdiri dari `microcontroller-service` untuk menerima dan menyimpan data sensor ESP32, serta `application-service` untuk mengambil data sensor lewat RabbitMQ dan mengirim email otomatis saat data baru masuk. Database utama adalah MySQL, RabbitMQ dipakai untuk request/response dan notifikasi, dan saat masuk ke Docker semua host `localhost` harus diganti environment variable atau nama service container seperti `mysql` dan `rabbitmq`.

