package com.iot.sensor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iot.sensor.model.entity.Device;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponseDto {

    @JsonProperty("device_id")
    private String deviceId;

    private String country;

    private String state;

    private String municipality;

    private Double latitude;

    private Double longitude;

    private List<Device.SensorConfig> sensors;

    private Boolean active;

    @JsonProperty("last_sequence")
    private Long lastSequence;
}
