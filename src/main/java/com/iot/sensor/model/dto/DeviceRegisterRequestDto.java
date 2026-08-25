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
 * Registro de un nuevo dispositivo IoT con sus sensores.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegisterRequestDto {

    @NotBlank(message = "device_id es requerido")
    @JsonProperty("device_id")
    private String deviceId;

    @NotBlank(message = "country es requerido")
    private String country;

    @NotBlank(message = "state es requerido")
    private String state;

    @Builder.Default
    private String municipality = "";

    @NotNull(message = "latitude es requerido")
    private Double latitude;

    @NotNull(message = "longitude es requerido")
    private Double longitude;

    @NotEmpty(message = "sensors es requerido (al menos 1 sensor)")
    @Valid
    private List<SensorDto> sensors;

    @NotBlank(message = "secret_key es requerido")
    @JsonProperty("secret_key")
    private String secretKey;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorDto {

        @NotBlank(message = "sensor id es requerido")
        private String id;

        @NotBlank(message = "sensor type es requerido")
        private String type;

        @NotBlank(message = "sensor unit es requerido")
        private String unit;

        @NotNull(message = "sensor adc es requerido")
        private Integer adc;

        @NotNull(message = "sensor gpio es requerido")
        private Integer gpio;

        @Builder.Default
        private String label = "";
    }
}
