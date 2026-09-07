package com.iot.sensor.model.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.iot.sensor.model.entity.converter.SensorListConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBTable(tableName = "iot_devices")
public class Device {

    @DynamoDBHashKey(attributeName = "device_id")
    private String deviceId;

    @DynamoDBAttribute(attributeName = "country")
    private String country;

    @DynamoDBAttribute(attributeName = "state")
    private String state;

    @DynamoDBAttribute(attributeName = "municipality")
    private String municipality;

    @DynamoDBAttribute(attributeName = "latitude")
    private Double latitude;

    @DynamoDBAttribute(attributeName = "longitude")
    private Double longitude;

    @DynamoDBAttribute(attributeName = "sensors")
    @DynamoDBTypeConverted(converter = SensorListConverter.class)
    private List<SensorConfig> sensors;

    @DynamoDBAttribute(attributeName = "secret_key")
    private String secretKey;

    @DynamoDBAttribute(attributeName = "active")
    private Boolean active;

    @DynamoDBAttribute(attributeName = "last_sequence")
    private Long lastSequence;

    @DynamoDBAttribute(attributeName = "registered_at")
    private Long registeredAt;

    /**
     * Configuración de un sensor conectado al dispositivo.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorConfig {
        private String id;
        private String type;
        private String unit;
        private Integer adc;
        private Integer gpio;
        private String label;
        // Estado del sensor (robustez): "ok" | "error".
        // Se persiste dentro del JSON del atributo "sensors" (sin cambio de schema DynamoDB).
        private String status;
        private Long since;    // epoch (s) desde que está en el estado actual
        private Long lastSeen; // epoch (s) de la última vez que se recibió dato de este sensor
    }
}
