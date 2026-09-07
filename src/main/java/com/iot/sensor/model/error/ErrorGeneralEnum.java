package com.iot.sensor.model.error;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorGeneralEnum {

    ERROR_DEVICE_NOT_FOUND("200001", "Dispositivo no registrado", 401),
    ERROR_DEVICE_INACTIVE("200002", "Dispositivo desactivado", 403),
    ERROR_TIMESTAMP_INVALID("200003", "Timestamp fuera de rango (>10 min)", 400),
    ERROR_SEQUENCE_REPLAY("200004", "Sequence no es mayor al último (replay)", 409),
    ERROR_SIGNATURE_INVALID("200005", "Firma HMAC inválida", 401),
    ERROR_DEVICE_EXISTS("200006", "Dispositivo ya existe", 409),
    ERROR_NOT_FOUND("200007", "No encontrado", 404),
    ERROR_NO_MEASUREMENTS("200008", "Sin mediciones", 404),
    ERROR_VALIDATION("200009", "Error de validación en request body", 400),
    ERROR_INTERNAL("200010", "Error interno del servidor", 500),
    ERROR_DEVICE_ID_FORMAT("200011", "Formato de device_id inválido", 400);

    private final String errorCode;
    private final String message;
    private final int httpStatus;
}
