package com.iot.sensor.model.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.iot.sensor.model.entity.converter.ReadingsListConverter;
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
@DynamoDBTable(tableName = "iot_measurements")
public class Measurement {

    @DynamoDBHashKey(attributeName = "device_id")
    private String deviceId;

    @DynamoDBRangeKey(attributeName = "timestamp")
    @DynamoDBIndexRangeKey(attributeName = "timestamp", globalSecondaryIndexName = "region-timestamp-index")
    private Long timestamp;

    @DynamoDBAttribute(attributeName = "readings")
    @DynamoDBTypeConverted(converter = ReadingsListConverter.class)
    private List<Reading> readings;

    @DynamoDBAttribute(attributeName = "sequence")
    private Long sequence;

    @DynamoDBAttribute(attributeName = "country")
    private String country;

    @DynamoDBAttribute(attributeName = "state")
    private String state;

    @DynamoDBIndexHashKey(attributeName = "country_state", globalSecondaryIndexName = "region-timestamp-index")
    private String countryState;

    /**
     * Una lectura individual de un sensor.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reading {
        private String sensorId;
        private String type;
        private String unit;
        private Double value;
        private Integer rawValue;
    }
}
