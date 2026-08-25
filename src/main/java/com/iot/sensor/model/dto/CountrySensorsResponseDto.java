package com.iot.sensor.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Sensores agrupados por país → estado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountrySensorsResponseDto {

    private String country;

    private List<RegionSensorsResponseDto> regions;
}
