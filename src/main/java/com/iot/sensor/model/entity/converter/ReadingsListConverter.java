package com.iot.sensor.model.entity.converter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.sensor.model.entity.Measurement.Reading;

import java.util.Collections;
import java.util.List;

/**
 * Convierte List<Reading> ↔ String (JSON) para almacenar en DynamoDB.
 */
public class ReadingsListConverter implements DynamoDBTypeConverter<String, List<Reading>> {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convert(List<Reading> readings) {
        if (readings == null || readings.isEmpty()) {
            return "[]";
        }
        try {
            return mapper.writeValueAsString(readings);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    @Override
    public List<Reading> unconvert(String json) {
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
