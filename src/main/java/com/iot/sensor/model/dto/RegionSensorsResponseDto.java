package com.iot.sensor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Sensores de una región con su última medición.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionSensorsResponseDto {

    private String state;

    @JsonProperty("sensor_count")
    private Integer sensorCount;

    private List<MeasurementResponseDto> measurements;
}
