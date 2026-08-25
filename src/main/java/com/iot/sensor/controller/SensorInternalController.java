package com.iot.sensor.controller;

import com.iot.sensor.model.dto.*;
import com.iot.sensor.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador interno — endpoints de administración y consulta.
 * Registrar dispositivos, listarlos, consultar mediciones agrupadas e históricas.
 * Solicitar medición bajo demanda via MQTT.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Internal - Administración", description = "Endpoints para gestión de dispositivos y consulta de datos")
public class SensorInternalController {

    private final SensorService sensorService;

    @Operation(summary = "Register new Device IoT")
    @PostMapping("/devices")
    public ResponseEntity<ApiResponseDto> create(@Valid @RequestBody DeviceRegisterRequestDto request) {
        return new ResponseEntity<>(sensorService.registerDevice(request), HttpStatus.CREATED);
    }

    @Operation(summary = "Get Specific Device")
    @GetMapping("/devices/{deviceId}")
    public ResponseEntity<DeviceResponseDto> getDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(sensorService.getDevice(deviceId));
    }

    @Operation(summary = "Get all sensors with their latest measurement grouped by country/state")
    @GetMapping("/sensors")
    public ResponseEntity<List<CountrySensorsResponseDto>> getSensorsGrouped(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1", name = "max_measurements") int maxMeasurements) {
        return ResponseEntity.ok(sensorService.getSensorsGrouped(page, size, Math.min(maxMeasurements, 100)));
    }

    @Operation(summary = "Delete a specific device")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponseDto> deleteDevice(@PathVariable String deviceId) {
        return ResponseEntity.ok(sensorService.deleteDevice(deviceId));
    }

    @Operation(summary = "Get measurement history for a device",
            description = "Returns measurements in a time range. Defaults to last 24h if no range specified.")
    @GetMapping("/devices/{deviceId}/measurements")
    public ResponseEntity<MeasurementHistoryResponseDto> getMeasurementHistory(
            @PathVariable String deviceId,
            @RequestParam(required = false, name = "start_time") Long startTime,
            @RequestParam(required = false, name = "end_time") Long endTime,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(sensorService.getMeasurementHistory(deviceId, startTime, endTime, Math.min(limit, 1000)));
    }
}
