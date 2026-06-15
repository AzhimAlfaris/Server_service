# Verdant Flow Service Server

Backend microservices Spring Boot untuk monitoring sensor ESP32.

## Ringkasan

- `microcontroller-service` menerima data sensor, menyimpannya ke MySQL, lalu mem-publish event ke RabbitMQ.
- `application-service` mengambil data sensor dari `microcontroller-service` saat aplikasi meminta data.
- `application-service` juga menerima event notifikasi untuk mengirim email ke user.
- `application-service` tidak memakai database sendiri.

Dokumentasi teknis lengkap ada di [`PROJECT-DOCUMENTATION.md`](./PROJECT-DOCUMENTATION.md).

## Cara Kerja Singkat

1. ESP32 mengirim data ke `microcontroller-service`.
2. `microcontroller-service` menyimpan data ke database MySQL.
3. Setelah data tersimpan, service ini mengirim event ke RabbitMQ.
4. `application-service` menerima event tersebut untuk proses email.
5. Saat aplikasi meminta data, `application-service` mengirim request ke RabbitMQ dan meneruskan hasilnya ke aplikasi.

## Struktur Utama

- `microcontroller-service/`
- `application-service/`
- `init/microcontroller_service_db.sql`
- `docker-compose.yml`

## Cara Pull Dari GitHub

Repo GitHub:

[`AzhimAlfaris/Service_server`](https://github.com/AzhimAlfaris/Service_server)

Perintah pull:

```powershell
git clone https://github.com/AzhimAlfaris/Service_server.git
cd Service_server
git pull origin main
```

Kalau branch utama Anda bukan `main`, ganti nama branch sesuai repo Anda.

## Cara Menjalankan Lokal

Jalankan masing-masing service dari foldernya:

```powershell
cd microcontroller-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd application-service
.\mvnw.cmd spring-boot:run
```

## Cara Testing

### 1. POST data sensor ke microcontroller-service

```http
POST http://localhost:8081/api/sensor-readings
```

Contoh body:

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

### 2. GET data lewat application-service

```http
GET http://localhost:8082/api/sensor-data/latest/ESP32-001
GET http://localhost:8082/api/sensor-data/history/ESP32-001?limit=10
```

### 3. GET data langsung dari microcontroller-service

```http
GET http://localhost:8081/api/sensor-readings/latest/ESP32-001
GET http://localhost:8081/api/sensor-readings/history/ESP32-001?limit=10
```

## Cara Memasukkan ke Docker

Pastikan:

- Docker dan Docker Compose sudah terpasang
- file `init/microcontroller_service_db.sql` ikut ter-push ke GitHub
- `MAIL_PASSWORD` sudah diisi dengan Gmail App Password

Jalankan dari root project:

```powershell
docker compose up -d --build
```

Kalau ingin database di-init ulang:

```powershell
docker compose down -v
docker compose up -d --build
```

## Port yang Dipakai

- `microcontroller-service` -> `http://localhost:8081`
- `application-service` -> `http://localhost:8082`
- RabbitMQ UI -> `http://localhost:15672`
- RabbitMQ AMQP -> `5672`
- MySQL -> `3306`

## Catatan Penting

- `verdant-rabbitmq` adalah message broker utama untuk project ini.
- `some-rabbit` yang lama tidak diperlukan lagi jika Anda memakai `docker compose` ini.
- `application-service` tidak memiliki database sendiri.
- Password Gmail tidak ditulis langsung di kode produksi, isi manual pada `MAIL_PASSWORD`.

