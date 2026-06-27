# RabbitMQ MQTT LVQ Policy

Gunakan policy ini untuk subscription queue MQTT ESP32.

- Pattern: `^mqtt-subscription-`
- Queue type: `queues`
- Arguments:
  - `x-max-length = 1`
  - `x-overflow = drop-head`

Tujuan:

- Jika ESP32 offline dan beberapa command manual masuk, RabbitMQ hanya menyimpan command terbaru.
- Saat device reconnect, queue subscription MQTT tetap membawa pesan paling baru.

Catatan:

- Queue subscription MQTT dibuat oleh RabbitMQ MQTT plugin.
- Policy ini berlaku untuk queue dinamis yang cocok dengan pattern `mqtt-subscription-`.
