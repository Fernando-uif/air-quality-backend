package com.iot.sensor.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.sensor.model.dto.DeviceAutoRegisterDto;
import com.iot.sensor.model.dto.MeasurementRequestDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.springframework.stereotype.Service;

/**
 * Escucha mensajes MQTT de los dispositivos Pico W.
 *
 * Topics:
 *   - devices/+/telemetry  → mediciones
 *   - devices/+/register   → auto-registro de nuevos dispositivos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MqttSubscriberService {

    private final MqttClient mqttClient;
    private final SensorService sensorService;
    private final ObjectMapper objectMapper;

    private static final String TELEMETRY_TOPIC = "devices/+/telemetry";
    private static final String REGISTER_TOPIC = "devices/+/register";

    @PostConstruct
    public void subscribe() {
        try {
            // Suscribir telemetry
            MqttSubscription telemetrySub = new MqttSubscription(TELEMETRY_TOPIC, 1);
            mqttClient.subscribe(
                    new MqttSubscription[]{telemetrySub},
                    new IMqttMessageListener[]{this::onTelemetry}
            );
            log.info("MQTT subscrito a: {}", TELEMETRY_TOPIC);

            // Suscribir register
            MqttSubscription registerSub = new MqttSubscription(REGISTER_TOPIC, 1);
            mqttClient.subscribe(
                    new MqttSubscription[]{registerSub},
                    new IMqttMessageListener[]{this::onRegister}
            );
            log.info("MQTT subscrito a: {}", REGISTER_TOPIC);

        } catch (Exception e) {
            log.error("Error al subscribirse a MQTT: {}", e.getMessage(), e);
        }
    }

    private void onTelemetry(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            log.debug("MQTT telemetry en {}: {}", topic, payload);

            MeasurementRequestDto request = objectMapper.readValue(payload, MeasurementRequestDto.class);
            sensorService.receiveMeasurement(request);

            log.info("MQTT medición procesada: device={}, seq={}", request.getDeviceId(), request.getSequence());

        } catch (Exception e) {
            log.error("Error procesando telemetry de {}: {}", topic, e.getMessage());
        }
    }

    private void onRegister(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            log.info("MQTT registro recibido en {}: {}", topic, payload);

            DeviceAutoRegisterDto request = objectMapper.readValue(payload, DeviceAutoRegisterDto.class);
            sensorService.autoRegisterDevice(request);

        } catch (Exception e) {
            log.error("Error procesando registro de {}: {}", topic, e.getMessage());
        }
    }
}
