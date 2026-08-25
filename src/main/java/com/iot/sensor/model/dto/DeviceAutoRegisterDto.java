package com.iot.sensor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Payload de auto-registro enviado por el Pico W via MQTT.
 * Topic: devices/{device_id}/register
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceAutoRegisterDto {

    @JsonProperty("device_id")
    private String deviceId;

    private String country;

    private String state;

    private String municipality;

    private Double latitude;

    private Double longitude;

    private List<SensorDto> sensors;

    private Long timestamp;

    private String signature;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SensorDto {
        private String id;
        private String type;
        private String unit;
        private Integer adc;
        private Integer gpio;
        private String label;
    }
}
