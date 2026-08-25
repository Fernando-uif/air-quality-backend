package com.iot.sensor.model.error;

import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException {

    private final ErrorMessage errorMessage;

    public GeneralException(ErrorGeneralEnum errorEnum) {
        super(errorEnum.getMessage());
        this.errorMessage = ErrorMessage.builder()
                .httpStatus(errorEnum.getHttpStatus())
                .developerMessage(errorEnum.getMessage())
                .errorCode(errorEnum.getErrorCode())
                .message(errorEnum.getMessage())
                .build();
    }

    public GeneralException(ErrorGeneralEnum errorEnum, String developerMessage) {
        super(errorEnum.getMessage());
        this.errorMessage = ErrorMessage.builder()
                .httpStatus(errorEnum.getHttpStatus())
                .developerMessage(developerMessage)
                .errorCode(errorEnum.getErrorCode())
                .message(errorEnum.getMessage())
                .build();
    }
}
