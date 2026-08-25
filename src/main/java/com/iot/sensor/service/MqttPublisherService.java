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
