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
 * Respuesta del histórico de mediciones de un dispositivo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementHistoryResponseDto {

    @JsonProperty("device_id")
    private String deviceId;

    private List<Device.SensorConfig> sensors;

    @JsonProperty("total_results")
    private Integer totalResults;

    private Integer limit;

    @JsonProperty("start_time")
    private Long startTime;

    @JsonProperty("end_time")
    private Long endTime;

    private List<MeasurementDataDto> measurements;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MeasurementDataDto {

        private List<Measurement.Reading> readings;

        private Long timestamp;

        private Long sequence;
    }
}
