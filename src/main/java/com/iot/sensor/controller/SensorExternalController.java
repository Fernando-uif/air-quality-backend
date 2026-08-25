package com.iot.sensor.controller;

import com.iot.sensor.model.dto.ApiResponseDto;
import com.iot.sensor.model.dto.MeasurementRequestDto;
import com.iot.sensor.service.MqttPublisherService;
import com.iot.sensor.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador externo — endpoints de comunicación con los dispositivos Pico W.
 * Recibe mediciones y envía comandos MQTT.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "External - Dispositivos", description = "Endpoints de comunicación con los Pico W")
public class SensorExternalController {

    private final SensorService sensorService;
    private final MqttPublisherService mqttPublisherService;

    @Operation(summary = "Recibe medición del Pico W (HTTP fallback)",
            description = "Valida HMAC-SHA256, timestamp (±10min), sequence (anti-replay) y guarda en DynamoDB")
    @PostMapping("/measurements")
    public ResponseEntity<ApiResponseDto> receiveMeasurement(@Valid @RequestBody MeasurementRequestDto request) {
        return new ResponseEntity<>(sensorService.receiveMeasurement(request), HttpStatus.OK);
    }

    @Operation(summary = "Request immediate measurement from device via MQTT",
            description = "Sends a command to the device to measure and report immediately")
    @PostMapping("/devices/{deviceId}/request-measurement")
    public ResponseEntity<ApiResponseDto> requestMeasurement(@PathVariable String deviceId) {
        // Verificar que el dispositivo existe
        sensorService.getDevice(deviceId);

        // Publicar comando MQTT
        mqttPublisherService.requestMeasurement(deviceId);

        return ResponseEntity.ok(ApiResponseDto.builder()
                .success(true)
                .message("Comando enviado al dispositivo via MQTT")
                .data(Map.of("device_id", deviceId, "action", "measure_now"))
                .build());
    }
}
