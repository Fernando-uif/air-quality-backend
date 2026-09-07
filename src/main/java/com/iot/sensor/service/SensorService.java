package com.iot.sensor.service;

import com.iot.sensor.model.dto.*;
import com.iot.sensor.model.entity.Device;
import com.iot.sensor.model.entity.Measurement;
import com.iot.sensor.model.error.ErrorGeneralEnum;
import com.iot.sensor.model.error.GeneralException;
import com.iot.sensor.repository.DeviceRepository;
import com.iot.sensor.repository.MeasurementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.iot.sensor.utils.SafeUtil.safe;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorService {

    private final DeviceRepository deviceRepository;
    private final MeasurementRepository measurementRepository;
    private final HmacService hmacService;
    private final MqttPublisherService mqttPublisherService;

    @org.springframework.beans.factory.annotation.Value("${security.registration-key}")
    private String registrationKey;

    // Formato esperado de device_id: PICO-{codigoPais 2-3 letras}-{numero}. Ej: PICO-MX-1
    private static final java.util.regex.Pattern DEVICE_ID_PATTERN =
            java.util.regex.Pattern.compile("^PICO-[A-Z]{2,3}-\\d+$");

    /** Señal de fallo enviada por el micro: value==0 && raw_value==0 (sin flag nuevo ni cambio de firma). */
    private static boolean isSensorFault(MeasurementRequestDto.ReadingDto r) {
        return r.getValue() != null && r.getValue() == 0.0
                && r.getRawValue() != null && r.getRawValue() == 0;
    }

    // ============================================================
    // POST /measurements
    // ============================================================

    /**
     * Recibe medición del Pico W con múltiples readings.
     * Flujo: verificar device → activo → timestamp → sequence → HMAC → guardar
     */
    public ApiResponseDto receiveMeasurement(MeasurementRequestDto request) {
        // 1. Buscar dispositivo
        Device device = deviceRepository.findById(request.getDeviceId())
                .orElseThrow(() -> new GeneralException(ErrorGeneralEnum.ERROR_DEVICE_NOT_FOUND));

        // 2. ¿Activo?
        if (!Boolean.TRUE.equals(device.getActive())) {
            throw new GeneralException(ErrorGeneralEnum.ERROR_DEVICE_INACTIVE);
        }

        // 3. Timestamp
        if (!hmacService.verifyTimestamp(request.getTimestamp())) {
            throw new GeneralException(ErrorGeneralEnum.ERROR_TIMESTAMP_INVALID);
        }

        // 4. Sequence (anti-replay)
        Long lastSeq = safe(device.getLastSequence());
        if (!hmacService.verifySequence(request.getSequence(), lastSeq)) {
            throw new GeneralException(ErrorGeneralEnum.ERROR_SEQUENCE_REPLAY,
                    String.format("Sequence %d no es mayor a %d (replay?)", request.getSequence(), lastSeq));
        }

        // 5. HMAC — firma sobre readings concatenados (payload COMPLETO recibido, incluidos fallos 0/0)
        if (!hmacService.verifySignatureMulti(
                device.getSecretKey(),
                request.getDeviceId(),
                request.getReadings(),
                request.getTimestamp(),
                request.getSequence(),
                request.getSignature())) {
            throw new GeneralException(ErrorGeneralEnum.ERROR_SIGNATURE_INVALID);
        }

        long now = Instant.now().getEpochSecond();

        // 6. Config de sensores del device (mapa por id) para type/unit y estado
        Map<String, Device.SensorConfig> sensorMap = new HashMap<>();
        if (device.getSensors() != null) {
            device.getSensors().forEach(s -> sensorMap.put(s.getId(), s));
        }

        // 6.1 Separar readings válidos de los que llegan en fallo (señal 0/0 puesta por el micro)
        //     La detección física (rango, 10 muestras, DHT11) es del MICRO; el backend solo lee la señal.
        List<Measurement.Reading> validReadings = new ArrayList<>();
        boolean deviceStateChanged = false;

        for (MeasurementRequestDto.ReadingDto r : request.getReadings()) {
            Device.SensorConfig cfg = sensorMap.get(r.getSensorId());
            boolean fault = isSensorFault(r);

            // Actualizar estado por sensor con transiciones (protección de fallos consecutivos)
            if (cfg != null) {
                cfg.setLastSeen(now);
                String prev = cfg.getStatus() == null ? "ok" : cfg.getStatus();
                String next = fault ? "error" : "ok";
                if (!prev.equals(next)) {
                    cfg.setStatus(next);
                    cfg.setSince(now);
                    deviceStateChanged = true;
                    log.info("Sensor {} de {} transición {}→{}", r.getSensorId(),
                            request.getDeviceId(), prev, next);
                }
            }

            if (!fault) {
                validReadings.add(Measurement.Reading.builder()
                        .sensorId(r.getSensorId())
                        .type(cfg != null ? cfg.getType() : "unknown")
                        .unit(cfg != null ? cfg.getUnit() : "?")
                        .value(r.getValue())
                        .rawValue(r.getRawValue())
                        .build());
            }
        }

        // 7. Persistir estado del device solo si hubo transición (evita escrituras redundantes)
        if (deviceStateChanged) {
            deviceRepository.save(device);
        }

        // 7.1 Si TODOS los sensores están en fallo → no hay dato útil: no se guarda medición
        if (validReadings.isEmpty()) {
            log.info("Medición NO guardada (todos los sensores en fallo): device={}, seq={}",
                    request.getDeviceId(), request.getSequence());
            return ApiResponseDto.builder()
                    .success(true)
                    .message("Sin sensores válidos: solo se actualizó el estado")
                    .data(Map.of(
                            "device_id", request.getDeviceId(),
                            "sensors", 0,
                            "sequence", request.getSequence()
                    ))
                    .build();
        }

        // 8. Guardar medición SOLO con readings válidos (histórico limpio, sin ceros de fallo)
        Measurement measurement = Measurement.builder()
                .deviceId(request.getDeviceId())
                .timestamp(request.getTimestamp())
                .readings(validReadings)
                .sequence(request.getSequence())
                .country(safe(device.getCountry()))
                .state(safe(device.getState()))
                .countryState(String.format("%s#%s", safe(device.getCountry()), safe(device.getState())))
                .build();

        measurementRepository.save(measurement);

        // Actualizar sequence
        deviceRepository.updateLastSequence(request.getDeviceId(), request.getSequence());

        log.info("Medición registrada: device={}, sensors={}, seq={}",
                request.getDeviceId(), validReadings.size(), request.getSequence());

        return ApiResponseDto.builder()
                .success(true)
                .message("Medición registrada")
                .data(Map.of(
                        "device_id", request.getDeviceId(),
                        "sensors", validReadings.size(),
                        "sequence", request.getSequence()
                ))
                .build();
    }

    // ============================================================
    // AUTO-REGISTRO via MQTT (devices/{id}/register)
    // ============================================================

    /**
     * Auto-registro de un dispositivo via MQTT.
     * El Pico envía su config completa firmada con su secret_key.
     * El backend valida la firma y lo registra si no existe.
     *
     * La secret_key debe estar pre-configurada en application.properties
     * o ser conocida por ambas partes (pre-shared key).
     */
    public void autoRegisterDevice(DeviceAutoRegisterDto request) {
        // 1. Validar formato de device_id (PICO-XX-N). NO se valida timestamp en el registro:
        //    el micro arranca con reloj basura y necesita registrarse para recibir la hora (ACK).
        if (request.getDeviceId() == null || !DEVICE_ID_PATTERN.matcher(request.getDeviceId()).matches()) {
            throw new GeneralException(ErrorGeneralEnum.ERROR_DEVICE_ID_FORMAT);
        }

        long now = Instant.now().getEpochSecond();

        // Buscar si ya existe
        Optional<Device> existing = deviceRepository.findById(request.getDeviceId());
        if (existing.isPresent()) {
            // Ya registrado — verificar firma con la key que ya tiene
            Device device = existing.get();

            if (!hmacService.verifyAutoRegisterSignature(device.getSecretKey(),
                    request.getDeviceId(), request.getTimestamp(), request.getSignature())) {
                throw new GeneralException(ErrorGeneralEnum.ERROR_SIGNATURE_INVALID);
            }

            // Actualizar info (ubicación, sensores pueden haber cambiado).
            // Preservar estado de sensores existente cuando el id coincide.
            Map<String, Device.SensorConfig> prevStatus = new HashMap<>();
            if (device.getSensors() != null) {
                device.getSensors().forEach(s -> prevStatus.put(s.getId(), s));
            }

            device.setCountry(request.getCountry());
            device.setState(request.getState());
            device.setMunicipality(request.getMunicipality());
            device.setLatitude(request.getLatitude());
            device.setLongitude(request.getLongitude());
            device.setSensors(request.getSensors().stream()
                    .map(s -> {
                        Device.SensorConfig old = prevStatus.get(s.getId());
                        return Device.SensorConfig.builder()
                                .id(s.getId()).type(s.getType()).unit(s.getUnit())
                                .adc(s.getAdc()).gpio(s.getGpio()).label(s.getLabel())
                                .status(old != null && old.getStatus() != null ? old.getStatus() : "ok")
                                .since(old != null && old.getSince() != null ? old.getSince() : now)
                                .lastSeen(old != null ? old.getLastSeen() : now)
                                .build();
                    })
                    .collect(Collectors.toList()));
            device.setActive(true);
            deviceRepository.save(device);

            log.info("Dispositivo re-registrado via MQTT: {}", request.getDeviceId());
            mqttPublisherService.sendRegisterAck(request.getDeviceId());
            return;
        }

        // 3. Nuevo dispositivo — validar firma con la REGISTRATION_KEY (shared secret del sistema)
        if (!hmacService.verifyAutoRegisterSignature(registrationKey,
                request.getDeviceId(), request.getTimestamp(), request.getSignature())) {
            throw new GeneralException(ErrorGeneralEnum.ERROR_SIGNATURE_INVALID);
        }

        // 4. Registrar (inicializar estado de cada sensor en "ok")
        List<Device.SensorConfig> sensors = request.getSensors().stream()
                .map(s -> Device.SensorConfig.builder()
                        .id(s.getId()).type(s.getType()).unit(s.getUnit())
                        .adc(s.getAdc()).gpio(s.getGpio()).label(s.getLabel())
                        .status("ok").since(now).lastSeen(now)
                        .build())
                .collect(Collectors.toList());

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .country(request.getCountry())
                .state(request.getState())
                .municipality(safe(request.getMunicipality()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .sensors(sensors)
                .secretKey(registrationKey)
                .active(true)
                .lastSequence(0L)
                .registeredAt(now)
                .build();

        deviceRepository.save(device);
        log.info("Dispositivo auto-registrado via MQTT: {} con {} sensores",
                request.getDeviceId(), sensors.size());
        mqttPublisherService.sendRegisterAck(request.getDeviceId());
    }

    // ============================================================
    // POST /devices
    // ============================================================

    public ApiResponseDto registerDevice(DeviceRegisterRequestDto request) {
        // Verificar si ya existe
        if (deviceRepository.findById(request.getDeviceId()).isPresent()) {
            throw new GeneralException(ErrorGeneralEnum.ERROR_DEVICE_EXISTS);
        }

        // Mapear sensores del DTO a la entidad
        List<Device.SensorConfig> sensors = request.getSensors().stream()
                .map(s -> Device.SensorConfig.builder()
                        .id(s.getId())
                        .type(s.getType())
                        .unit(s.getUnit())
                        .adc(s.getAdc())
                        .gpio(s.getGpio())
                        .label(s.getLabel())
                        .build())
                .collect(Collectors.toList());

        Device device = Device.builder()
                .deviceId(request.getDeviceId())
                .country(request.getCountry())
                .state(request.getState())
                .municipality(safe(request.getMunicipality()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .sensors(sensors)
                .secretKey(request.getSecretKey())
                .active(true)
                .lastSequence(0L)
                .registeredAt(Instant.now().getEpochSecond())
                .build();

        deviceRepository.save(device);

        log.info("Dispositivo registrado: {} con {} sensores", request.getDeviceId(), sensors.size());

        return ApiResponseDto.builder()
                .success(true)
                .message("Dispositivo registrado")
                .data(Map.of("device_id", request.getDeviceId(), "sensors", sensors.size()))
                .build();
    }

    // ============================================================
    // GET /devices/{deviceId}
    // ============================================================

    public DeviceResponseDto getDevice(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new GeneralException(ErrorGeneralEnum.ERROR_NOT_FOUND));
        return DeviceResponseDto.builder()
                .deviceId(device.getDeviceId())
                .country(safe(device.getCountry()))
                .state(safe(device.getState()))
                .municipality(safe(device.getMunicipality()))
                .latitude(safe(device.getLatitude()))
                .longitude(safe(device.getLongitude()))
                .sensors(device.getSensors())
                .active(safe(device.getActive()))
                .lastSequence(safe(device.getLastSequence()))
                .build();
    }

    // ============================================================
    // DELETE /devices/{deviceId}
    // ============================================================

    public ApiResponseDto deleteDevice(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new GeneralException(ErrorGeneralEnum.ERROR_NOT_FOUND));

        deviceRepository.delete(device);

        log.info("Dispositivo eliminado: {}", deviceId);

        return ApiResponseDto.builder()
                .success(true)
                .message("Dispositivo eliminado")
                .data(Map.of("device_id", deviceId))
                .build();
    }

    // ============================================================
    // GET /devices/{deviceId}/measurements (histórico)
    // ============================================================

    public MeasurementHistoryResponseDto getMeasurementHistory(String deviceId, Long startTime, Long endTime, int limit) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new GeneralException(ErrorGeneralEnum.ERROR_NOT_FOUND));

        long now = Instant.now().getEpochSecond();
        long effectiveStart = (startTime != null) ? startTime : now - 86400;
        long effectiveEnd = (endTime != null) ? endTime : now;

        List<Measurement> measurements = measurementRepository.findByDeviceIdAndTimeRange(
                deviceId, effectiveStart, effectiveEnd, limit);

        List<MeasurementHistoryResponseDto.MeasurementDataDto> data = measurements.stream()
                .map(m -> MeasurementHistoryResponseDto.MeasurementDataDto.builder()
                        .readings(m.getReadings())
                        .timestamp(m.getTimestamp())
                        .sequence(m.getSequence())
                        .build())
                .collect(Collectors.toList());

        return MeasurementHistoryResponseDto.builder()
                .deviceId(deviceId)
                .sensors(device.getSensors())
                .totalResults(data.size())
                .limit(limit)
                .startTime(effectiveStart)
                .endTime(effectiveEnd)
                .measurements(data)
                .build();
    }

    // ============================================================
    // GET /sensors (agrupado por país → estado)
    // ============================================================

    public List<CountrySensorsResponseDto> getSensorsGrouped(int page, int size, int maxMeasurements) {
        List<Device> devices = deviceRepository.findAll();

        Map<String, Map<String, List<MeasurementResponseDto>>> grouped = new LinkedHashMap<>();

        for (Device device : devices) {
            if (!Boolean.TRUE.equals(device.getActive())) {
                continue;
            }

            Optional<Measurement> latestOpt = measurementRepository.findLatestByDeviceId(device.getDeviceId());
            if (latestOpt.isEmpty()) {
                continue;
            }

            Measurement latest = latestOpt.get();
            String country = device.getCountry() != null ? device.getCountry() : "Unknown";
            String state = device.getState() != null ? device.getState() : "Unknown";

            MeasurementResponseDto response = MeasurementResponseDto.builder()
                    .deviceId(latest.getDeviceId())
                    .readings(latest.getReadings())
                    .timestamp(latest.getTimestamp())
                    .sequence(latest.getSequence())
                    .sensors(device.getSensors())
                    .latitude(device.getLatitude())
                    .longitude(device.getLongitude())
                    .country(country)
                    .state(state)
                    .municipality(device.getMunicipality())
                    .build();

            grouped.computeIfAbsent(country, k -> new LinkedHashMap<>())
                    .computeIfAbsent(state, k -> new ArrayList<>())
                    .add(response);
        }

        List<CountrySensorsResponseDto> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<MeasurementResponseDto>>> countryEntry : grouped.entrySet()) {
            List<RegionSensorsResponseDto> regions = new ArrayList<>();
            for (Map.Entry<String, List<MeasurementResponseDto>> stateEntry : countryEntry.getValue().entrySet()) {
                List<MeasurementResponseDto> limited = stateEntry.getValue().stream()
                        .limit(maxMeasurements)
                        .collect(Collectors.toList());
                regions.add(RegionSensorsResponseDto.builder()
                        .state(stateEntry.getKey())
                        .sensorCount(stateEntry.getValue().size())
                        .measurements(limited)
                        .build());
            }
            result.add(CountrySensorsResponseDto.builder()
                    .country(countryEntry.getKey())
                    .regions(regions)
                    .build());
        }

        int fromIndex = page * size;
        if (fromIndex >= result.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + size, result.size());

        return result.subList(fromIndex, toIndex);
    }
}
