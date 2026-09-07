package com.iot.sensor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Publica comandos MQTT a los dispositivos Pico W.
 *
 * Topic: devices/{device_id}/command
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MqttPublisherService {

    private final MqttClient mqttClient;
    private final ObjectMapper objectMapper;

    private static final String COMMAND_TOPIC_TEMPLATE = "devices/%s/command";
    private static final String REGISTER_ACK_TOPIC_TEMPLATE = "devices/%s/register/ack";

    /**
     * Envía comando de medición inmediata a un dispositivo.
     */
    public void requestMeasurement(String deviceId) {
        String topic = String.format(COMMAND_TOPIC_TEMPLATE, deviceId);

        Map<String, String> command = Map.of(
                "action", "measure_now",
                "requested_at", String.valueOf(System.currentTimeMillis() / 1000)
        );

        publish(topic, command);
        log.info("MQTT comando enviado a {}: measure_now", deviceId);
    }

    /**
     * Responde al registro del dispositivo con la hora del servidor (calibración de reloj).
     * Topic: devices/{device_id}/register/ack
     * El micro usa server_time para calcular su offset (no depende de NTP ni RTC).
     */
    public void sendRegisterAck(String deviceId) {
        String topic = String.format(REGISTER_ACK_TOPIC_TEMPLATE, deviceId);

        Map<String, Object> ack = Map.of(
                "action", "time_sync",
                "server_time", java.time.Instant.now().getEpochSecond()
        );

        publish(topic, ack);
        log.info("MQTT ACK de registro enviado a {}: time_sync", deviceId);
    }

    /**
     * Publica un mensaje JSON en un topic.
     */
    public void publish(String topic, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            MqttMessage message = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            message.setRetained(false);
            mqttClient.publish(topic, message);
        } catch (Exception e) {
            log.error("Error publicando MQTT en {}: {}", topic, e.getMessage());
        }
    }
}
