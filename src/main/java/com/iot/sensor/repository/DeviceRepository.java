package com.iot.sensor.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.iot.sensor.model.entity.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DeviceRepository {

    private final DynamoDBMapper dynamoDBMapper;

    /**
     * Obtiene un dispositivo por su ID.
     */
    public Optional<Device> findById(String deviceId) {
        return Optional.ofNullable(dynamoDBMapper.load(Device.class, deviceId));
    }

    /**
     * Obtiene todos los dispositivos (scan completo con paginación).
     */
    public List<Device> findAll() {
        return dynamoDBMapper.scan(Device.class, new DynamoDBScanExpression());
    }

    /**
     * Registra un nuevo dispositivo.
     */
    public Device save(Device device) {
        dynamoDBMapper.save(device);
        return device;
    }

    /**
     * Actualiza el last_sequence de un dispositivo.
     */
    public void updateLastSequence(String deviceId, Long sequence) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        if (device != null) {
            device.setLastSequence(sequence);
            dynamoDBMapper.save(device);
        }
    }

    /**
     * Elimina un dispositivo.
     */
    public void delete(Device device) {
        dynamoDBMapper.delete(device);
    }
}
