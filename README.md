# Verdant Flow Service Server

Backend microservices Spring Boot untuk monitoring sensor ESP32, dengan MySQL, RabbitMQ, email notifikasi, serta monitoring Prometheus dan Grafana.

## Ringkasan

- `microcontroller-service` menerima data sensor, menyimpannya ke MySQL, lalu mem-publish event notifikasi ke RabbitMQ.
- `application-service` mengambil data sensor dari `microcontroller-service` berdasarkan email pengguna saat aplikasi meminta data.
- `application-service` juga menerima event notifikasi untuk mengirim email ke user.
- Manual watering command sekarang dipush lewat RabbitMQ MQTT ke ESP32, bukan lewat polling HTTP bridge.
- `application-service` tidak memakai database sendiri.
- `docker-compose.yml` terbaru juga menjalankan `prometheus` dan `grafana`.

Dokumentasi teknis lengkap ada di [`PROJECT-DOCUMENTATION.md`](./PROJECT-DOCUMENTATION.md).

## Alur Singkat

1. ESP32 mengirim data ke `microcontroller-service`.
2. `microcontroller-service` menyimpan data ke database MySQL.
3. Setelah data tersimpan, service ini mengirim event ke RabbitMQ.
4. `application-service` menerima event tersebut dan mengirim email.
5. Saat aplikasi meminta data, `application-service` mengirim request ke RabbitMQ dan meneruskan hasilnya ke aplikasi.

## Komponen Utama

- `microcontroller-service/`
- `application-service/`
- `init/microcontroller-service-db.sql`
- `docker-compose.yml`
- `prometheus.yml`

## Jalankan Lokal

Jalankan masing-masing service dari foldernya:

```powershell
cd microcontroller-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd application-service
.\mvnw.cmd spring-boot:run
```

## Testing Cepat

### microcontroller-service

```http
POST http://localhost:8081/api/sensor-readings
GET http://localhost:8081/api/sensor-readings/latest/decalyps@gmail.com
GET http://localhost:8081/api/sensor-readings/history/decalyps@gmail.com?limit=5
```

### application-service

```http
GET http://localhost:8082/api/sensor-data/latest/decalyps@gmail.com
GET http://localhost:8082/api/sensor-data/history/decalyps@gmail.com?limit=5
```

## Jalankan dengan Docker

Pastikan file `.jar` sudah dibuat untuk kedua service:

```powershell
cd microcontroller-service
.\mvnw.cmd -DskipTests package

cd ..\application-service
.\mvnw.cmd -DskipTests package

cd ..
docker compose up -d
```

Setelah file `.jar` sudah ada di masing-masing folder `target/`, `docker compose up -d` hanya akan membangun image ringan dari JRE dan menyalin artefak `.jar` yang sudah jadi.

### Setup Kibana Index Pattern

Setelah stack Docker berjalan dan Logstash mulai menerima log, buat index pattern berikut di Kibana:

- `microcontroller-service-logs-*`
- `application-service-logs-*`

Langkahnya:

1. Buka Kibana di `http://localhost:5601`.
2. Masuk ke menu **Stack Management**.
3. Pilih **Index Patterns** atau **Data Views**.
4. Buat data view untuk `microcontroller-service-logs-*`.
5. Buat data view untuk `application-service-logs-*`.
6. Pilih field waktu `@timestamp` jika Kibana meminta time field.
7. Simpan keduanya.

Index pattern ini sesuai dengan konfigurasi Logstash yang menulis log ke index berbentuk:

- `microcontroller-service-logs-YYYY.MM.dd`
- `application-service-logs-YYYY.MM.dd`

### Port yang Dipakai

- `microcontroller-service` -> `http://localhost:8081`
- `application-service` -> `http://localhost:8082`
- RabbitMQ UI -> `http://localhost:15672`
- RabbitMQ AMQP -> `5672`
- RabbitMQ MQTT -> `1883`
- MySQL -> `3306`
- Prometheus -> `http://localhost:9090`
- Grafana -> `http://localhost:3000`

## Monitoring

- Setiap service mengekspos `actuator/health`, `actuator/info`, dan `actuator/prometheus`.
- `prometheus.yml` sudah diarahkan ke:
  - `microcontroller-service:8081/actuator/prometheus`
  - `application-service:8082/actuator/prometheus`
- Log aplikasi dikirim ke Logstash dan tersedia di Kibana setelah index pattern dibuat.

## Catatan

- `application-service` tidak memiliki database sendiri.
- `MAIL_PASSWORD` harus diisi dengan Gmail App Password.
- `init/microcontroller-service-db.sql` dipakai untuk inisialisasi tabel `sensor_readings` dan `pot_details`.
