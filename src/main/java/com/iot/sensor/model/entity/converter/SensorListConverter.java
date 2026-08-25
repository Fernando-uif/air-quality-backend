package com.iot.sensor.model.entity.converter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.sensor.model.entity.Device.SensorConfig;

import java.util.Collections;
import java.util.List;

/**
 * Convierte List<SensorConfig> ↔ String (JSON) para almacenar en DynamoDB.
 */
public class SensorListConverter implements DynamoDBTypeConverter<String, List<SensorConfig>> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convert(List<SensorConfig> sensors) {
        if (sensors == null || sensors.isEmpty()) {
            return "[]";
        }
        try {
            return mapper.writeValueAsString(sensors);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public List<SensorConfig> unconvert(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
