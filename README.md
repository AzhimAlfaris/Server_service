# Verdant Flow - Service Server

Dokumentasi lengkap project ada di [PROJECT-DOCUMENTATION.md](./PROJECT-DOCUMENTATION.md).

Ringkasnya:
- `microcontroller-service` menyimpan data sensor ke MySQL.
- `application-service` mengambil data sensor lewat RabbitMQ saat aplikasi request.
- `microcontroller-service` juga mengirim notifikasi email ke user setiap ada data baru masuk.

