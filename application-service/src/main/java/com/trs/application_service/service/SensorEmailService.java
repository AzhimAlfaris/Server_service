package com.trs.application_service.service;

import com.trs.application_service.dto.SensorReadingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@RequiredArgsConstructor
public class SensorEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:anasrudi048@gmail.com}")
    private String fromEmail;

    public void sendSensorUpdate(SensorReadingResponse reading) {
        if (reading.email() == null || reading.email().isBlank()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(reading.email());
        message.setSubject("Sensor update dari " + reading.address());

        StringBuilder body = new StringBuilder();
        body.append("""
                Halo,

                Berikut data sensor terbaru dari perangkat %s:
                - Email tujuan: %s
                - Created at: %s
                """.formatted(
                reading.address(),
                reading.email(),
                reading.createdAt()));

        if (reading.pots() != null && !reading.pots().isEmpty()) {
            body.append("\nRincian pot:\n");
            for (var pot : reading.pots()) {
                body.append("""
                        - Pot %s
                          Sensor value: %s
                          Moisture percent: %s
                          Soil condition: %s
                          Action: %s
                          Pump duration: %s
                          Timestamp sensor: %s
                        """.formatted(
                        pot.potIndex(),
                        pot.sensorValue(),
                        pot.moisturePercent(),
                        pot.soilCondition(),
                        pot.action(),
                        pot.pumpDuration(),
                        pot.timestampSensor()));
            }
        }

        body.append("""

                Pesan ini dikirim otomatis dari microcontroller-service.
                """);
        message.setText(body.toString());

        mailSender.send(message);
    }
}
