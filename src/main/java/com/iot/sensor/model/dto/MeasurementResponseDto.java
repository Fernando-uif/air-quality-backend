package com.iot.sensor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iot.sensor.model.entity.Device;
import com.iot.sensor.model.entity.Measurement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Medición con readings y datos del dispositivo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementResponseDto {

    @JsonProperty("device_id")
    private String deviceId;

    private List<Measurement.Reading> readings;

    private Long timestamp;

    private Long sequence;

    private List<Device.SensorConfig> sensors;

    private Double latitude;

    private Double longitude;

    private String country;

    private String state;

    private String municipality;
}
