# Verdant Flow Service Server Documentation

Dokumen ini menjelaskan kondisi terbaru project backend monitoring sensor berbasis Spring Boot, MySQL, RabbitMQ, dan ELK stack.

Project ini terdiri dari dua service utama:

- `microcontroller-service`
- `application-service`

Selain itu, repository ini juga memuat:

- `docker-compose.yml` untuk menjalankan seluruh stack
- `init/microcontroller_service_db.sql` untuk inisialisasi database
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

### Jalankan lokal

```powershell
cd microcontroller-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd application-service
.\mvnw.cmd spring-boot:run
```

### Jalankan seluruh stack Docker

```powershell
cd microcontroller-service
.\mvnw.cmd -DskipTests package

cd ..\application-service
.\mvnw.cmd -DskipTests package

cd ..
docker compose up -d
```

Jika ingin reset database container:

```powershell
docker compose down -v
cd microcontroller-service
.\mvnw.cmd -DskipTests package

cd ..\application-service
.\mvnw.cmd -DskipTests package

cd ..
docker compose up -d
```

## 2. Gambaran Umum

Project ini adalah backend untuk monitoring sensor berbasis ESP32.

Alur utamanya:

1. ESP32 mengirim data sensor ke `microcontroller-service`.
2. `microcontroller-service` menyimpan data ke MySQL.
3. Setelah data tersimpan, service ini mem-publish event notifikasi ke RabbitMQ.
4. `application-service` menerima event tersebut dan mengirim email ke alamat user pada data sensor.
5. Saat aplikasi meminta data terbaru atau riwayat sensor, `application-service` mengirim request ke RabbitMQ dan meneruskan hasilnya ke caller.
6. Seluruh log aplikasi dikirim ke Logstash, diteruskan ke Elasticsearch, dan ditampilkan di Kibana.

Untuk alur Docker saat ini, file `.jar` dibangun terlebih dahulu di masing-masing service. Docker kemudian hanya menyalin JAR tersebut ke image runtime berbasis JRE, sehingga proses `docker compose up -d` tidak lagi melakukan compile Java di dalam container.

## 3. Arsitektur

### 3.1 `microcontroller-service`

Tanggung jawab:

- menerima data sensor dari ESP32
- menyimpan data ke MySQL
- menyediakan endpoint `latest` dan `history` untuk debug langsung
- mengirim event notifikasi ke RabbitMQ setelah insert berhasil
- mengirim log aplikasi ke Logstash

### 3.2 `application-service`

Tanggung jawab:

- menyediakan endpoint untuk aplikasi
- mengirim request data `LATEST` atau `HISTORY` melalui RabbitMQ
- menerima event notifikasi dari RabbitMQ
- mengirim email otomatis ke user
- tidak memakai database sendiri
- mengirim log aplikasi ke Logstash

### 3.3 Komponen Infrastruktur

- MySQL untuk `microcontroller-service`
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
- Lombok
- Jackson Databind

### 4.2 Observability

Kedua service sudah mengaktifkan:

- `spring-boot-starter-actuator`
- `micrometer-registry-prometheus`
- logging ke Logstash melalui `logback-spring.xml`

Endpoint yang diekspos:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

## 5. Logging dan ELK

### 5.1 Alur log

Log dari kedua service dikirim ke Logstash lewat TCP appender:

- `microcontroller-service` -> Logstash port `5000`
- `application-service` -> Logstash port `5001`

Logstash kemudian meneruskan data ke Elasticsearch, dan Kibana membaca index dari Elasticsearch.

### 5.2 File konfigurasi

- `application-service/src/main/resources/logback-spring.xml`
- `microcontroller-service/src/main/resources/logback-spring.xml`
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

## 6. Struktur Project

### 6.1 `application-service`

- `controller`
  - `SensorDataController`
- `service`
  - `SensorDataClientService`
  - `SensorEmailListener`
  - `SensorEmailService`
- `config`
  - `RabbitMQConfig`
  - `HttpRequestLoggingFilter`
- `dto`
  - `SensorQueryRequest`
  - `SensorQueryResponse`
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

## 7. Konfigurasi Runtime

### 7.1 Port default

- `microcontroller-service` -> `8081`
- `application-service` -> `8082`

### 7.2 `application-service/src/main/resources/application.properties`

Nilai penting yang dipakai:

- `spring.application.name=application-service`
- `server.port=8082`
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

- `SensorEmailListener` aktif jika `spring.mail.host` tersedia.
- Jika `MAIL_PASSWORD` tidak diisi dengan Gmail App Password, email tidak akan terkirim.

### 7.3 `microcontroller-service/src/main/resources/application.properties`

Nilai penting yang dipakai:

- `spring.application.name=microcontroller-service`
- `server.port=8081`
- `spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME_MICRO:microcontroller_service_db}`
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

## 8. Alur Data

### 8.1 Request data dari aplikasi

1. Aplikasi memanggil endpoint `application-service`.
2. `SensorDataClientService` mengubah request menjadi JSON `SensorQueryRequest`.
3. Payload dikirim ke exchange `sensor.exchange` dengan routing key `sensor.request`.
4. `SensorRequestListener` di `microcontroller-service` menerima payload lewat queue `sensor.request.queue`.
5. Service membaca data dari database.
6. Response dikembalikan sebagai `SensorQueryResponse`.
7. `application-service` meneruskan response ke caller.

### 8.2 Notifikasi email

1. ESP32 mengirim data ke `microcontroller-service`.
2. `SensorReadingService` menyimpan data ke MySQL.
3. Setelah insert berhasil, service mem-publish payload ke routing key `sensor.notification`.
4. `SensorEmailListener` di `application-service` menerima payload dari queue `sensor.notification.queue`.
5. `SensorEmailService` mengirim email ke alamat yang ada di data sensor.

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

### 10.1 Database `microcontroller_service_db`

Database hanya dipakai oleh `microcontroller-service`.

Tabel utama:

- `sensor_readings`

Kolom inti:

- `id`
- `microcontroller_id`
- `email`
- `sensor_value`
- `moisture_percent`
- `soil_condition`
- `action`
- `pump_duration`
- `timestamp_sensor`
- `created_at`

### 10.2 Skema SQL inti

```sql
CREATE TABLE sensor_readings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    microcontroller_id VARCHAR(100) NOT NULL,
    moisture_percent VARCHAR(255) NOT NULL,
    pump_duration VARCHAR(255) NOT NULL,
    sensor_value VARCHAR(255) NOT NULL,
    soil_condition VARCHAR(255) NOT NULL,
    timestamp_sensor VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);
```

File `init/microcontroller_service_db.sql` digunakan untuk inisialisasi tabel saat container MySQL pertama kali dibuat.

### 10.3 `application-service`

Service ini tidak memakai database.

## 11. Endpoint API

## 11.1 `microcontroller-service`

Base URL:

```text
http://localhost:8081
```

### POST `/api/sensor-readings`

Menyimpan data sensor baru.

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

Response:

- HTTP `201 Created`
- body `SensorReadingResponse`

### GET `/api/sensor-readings/latest/{microcontrollerId}`

Mengambil data terbaru untuk satu `microcontrollerId`.

Response:

- HTTP `200 OK`
- body `SensorReadingResponse`

### GET `/api/sensor-readings/history/{microcontrollerId}?limit=10`

Mengambil riwayat data sensor.

Response:

- HTTP `200 OK`
- body `List<SensorReadingResponse>`

## 11.2 `application-service`

Base URL:

```text
http://localhost:8082
```

### GET `/api/sensor-data/latest/{microcontrollerId}`

Meminta data terbaru dari `microcontroller-service`.

Response body:

```json
{
  "status": "success",
  "message": "Data sensor terbaru berhasil diambil",
  "microcontrollerId": "ESP32-001",
  "readings": []
}
```

### GET `/api/sensor-data/history/{microcontrollerId}?limit=10`

Meminta riwayat data sensor dari `microcontroller-service`.

Response body:

```json
{
  "status": "success",
  "message": "Riwayat data sensor berhasil diambil",
  "microcontrollerId": "ESP32-001",
  "readings": []
}
```

Catatan:

- `latest` mengembalikan 1 item di `readings`.
- `history` mengembalikan daftar data hingga batas `limit`.
- Jika data tidak ditemukan, service mengembalikan error dengan status not found.

## 12. Docker Compose Terbaru

`docker-compose.yml` saat ini menjalankan service berikut:

- `verdant-mysql`
- `rabbitmq`
- `microcontroller-service`
- `application-service`
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
- Prometheus -> `9090`
- Grafana -> `3000`
- Elasticsearch -> `9200`
- Logstash TCP -> `5000` dan `5001`
- Kibana -> `5601`

### 12.2 Network

Semua container menggunakan network:

- `verdant-network`

### 12.3 Environment variable di Docker

#### `microcontroller-service`

- `DB_HOST=verdant-mysql`
- `DB_PORT=3306`
- `DB_NAME_MICRO=microcontroller_service_db`
- `DB_USERNAME=root`
- `DB_PASSWORD=root`
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

### 12.4 Catatan penting Docker

- Di dalam container, `localhost` tidak dipakai untuk koneksi antarservice.
- Gunakan nama service container seperti `verdant-mysql`, `rabbitmq`, dan `verdant-logstash`.
- `docker-compose.yml` juga memasang healthcheck untuk MySQL dan RabbitMQ.
- Dockerfile untuk kedua service Java sekarang menggunakan image runtime JRE dan menyalin file `.jar` dari folder `target/`.
- Proses build Java dijalankan di luar Docker memakai Maven Wrapper.

## 13. Monitoring

### 13.1 Prometheus

`prometheus.yml` saat ini scraping:

- `microcontroller-service:8081/actuator/prometheus`
- `application-service:8082/actuator/prometheus`

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

Log aplikasi yang dikirim oleh kedua service akan muncul di Kibana setelah masuk ke Elasticsearch melalui Logstash.

#### Index pattern Kibana

Buat dua data view atau index pattern berikut:

- `microcontroller-service-logs-*`
- `application-service-logs-*`

Langkah singkat:

1. Buka Kibana di `http://localhost:5601`.
2. Masuk ke menu **Stack Management**.
3. Pilih **Index Patterns** atau **Data Views**.
4. Buat data view `microcontroller-service-logs-*`.
5. Buat data view `application-service-logs-*`.
6. Gunakan field waktu `@timestamp` jika diminta.
7. Simpan keduanya.

### 13.4 Actuator

Kedua service mengekspos metrics ke:

- `http://localhost:8081/actuator/health`
- `http://localhost:8081/actuator/info`
- `http://localhost:8081/actuator/prometheus`
- `http://localhost:8082/actuator/health`
- `http://localhost:8082/actuator/info`
- `http://localhost:8082/actuator/prometheus`

## 14. Catatan Testing

Jika ingin validasi cepat setelah menjalankan service:

1. POST data ke `microcontroller-service`.
2. GET `latest` dan `history` dari `microcontroller-service`.
3. GET `latest` dan `history` dari `application-service`.
4. Cek `actuator/health`.
5. Cek `actuator/prometheus`.
6. Cek log di Kibana untuk memastikan request aplikasi juga masuk ke Elasticsearch.

## 15. Ringkasan Singkat

Project ini terdiri dari `microcontroller-service` untuk menerima dan menyimpan data sensor ESP32, serta `application-service` untuk mengambil data sensor lewat RabbitMQ dan mengirim email otomatis saat data baru masuk.

Database hanya dipakai oleh `microcontroller-service`, sementara observability sudah tersedia lewat Actuator, Prometheus, Grafana, dan ELK stack.
