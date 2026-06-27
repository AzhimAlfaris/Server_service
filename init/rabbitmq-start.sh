#!/bin/sh
set -eu

# Enable MQTT plugin before the broker starts.
rabbitmq-plugins enable --offline rabbitmq_mqtt

# Start broker in background so we can apply the LVQ policy once the node is ready.
rabbitmq-server &

until rabbitmq-diagnostics -q ping; do
  sleep 2
done

retry_count=0
until rabbitmqctl await_startup >/dev/null 2>&1; do
  retry_count=$((retry_count + 1))
  if [ "$retry_count" -ge 30 ]; then
    echo "RabbitMQ did not become ready in time" >&2
    exit 1
  fi
  sleep 2
done

# RabbitMQ policy syntax uses max-length/overflow; it enforces the desired LVQ behavior
# for MQTT subscription queues that match the pattern ^mqtt-subscription-.
retry_count=0
until rabbitmqctl set_policy mqtt-lvq '^mqtt-subscription-' '{"max-length":1,"overflow":"drop-head"}' --apply-to queues >/dev/null 2>&1; do
  retry_count=$((retry_count + 1))
  if [ "$retry_count" -ge 30 ]; then
    echo "RabbitMQ did not accept policy application in time" >&2
    exit 1
  fi
  sleep 2
done

echo "RabbitMQ MQTT plugin enabled and LVQ policy applied"

wait
