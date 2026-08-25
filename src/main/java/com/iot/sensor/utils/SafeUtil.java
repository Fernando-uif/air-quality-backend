package com.iot.sensor.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utilidades generales reutilizables en todo el proyecto.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SafeUtil {

    public static String safe(String value) {
        return value != null ? value : "";
    }

    public static String safe(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static Double safe(Double value) {
        return value != null ? value : 0.0;
    }

    public static Long safe(Long value) {
        return value != null ? value : 0L;
    }

    public static Integer safe(Integer value) {
        return value != null ? value : 0;
    }

    public static Boolean safe(Boolean value) {
        return value != null ? value : false;
    }
}
