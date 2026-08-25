package com.iot.sensor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Payload del Pico W con readings de múltiples sensores.
 * No incluye ubicación — el backend la resuelve por device_id.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeasurementRequestDto {

    @NotBlank(message = "device_id es requerido")
    @JsonProperty("device_id")
    private String deviceId;

    @NotEmpty(message = "readings es requerido")
    @Valid
    private List<ReadingDto> readings;

    @NotNull(message = "timestamp es requerido")
    private Long timestamp;

    @NotNull(message = "sequence es requerido")
    private Long sequence;

    @NotBlank(message = "signature es requerido")
    private String signature;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReadingDto {

        @NotBlank(message = "sensor_id es requerido")
        @JsonProperty("sensor_id")
        private String sensorId;

        @NotNull(message = "value es requerido")
        private Double value;

        @NotNull(message = "raw_value es requerido")
        @JsonProperty("raw_value")
        private Integer rawValue;
    }
}
