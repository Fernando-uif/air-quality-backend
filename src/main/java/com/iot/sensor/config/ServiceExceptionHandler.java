package com.iot.sensor.config;

import com.iot.sensor.model.error.ErrorGeneralEnum;
import com.iot.sensor.model.error.ErrorMessage;
import com.iot.sensor.model.error.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class ServiceExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            errors.put(fieldName, error.getDefaultMessage());
        });

        ErrorGeneralEnum error = ErrorGeneralEnum.ERROR_VALIDATION;
        return new ResponseEntity<>(
                ErrorMessage.builder()
                        .message(error.getMessage())
                        .developerMessage(String.format("Validation errors: %s", errors))
                        .httpStatus(error.getHttpStatus())
                        .errorCode(error.getErrorCode())
                        .build(),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {GeneralException.class})
    public ResponseEntity<ErrorMessage> handleGeneralException(GeneralException ex) {
        log.info("GeneralException: {}", ex.getErrorMessage().getDeveloperMessage());
        return new ResponseEntity<>(
                ex.getErrorMessage(),
                HttpStatus.valueOf(ex.getErrorMessage().getHttpStatus()));
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<ErrorMessage> handleGenericException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ErrorGeneralEnum error = ErrorGeneralEnum.ERROR_INTERNAL;
        return new ResponseEntity<>(
                ErrorMessage.builder()
                        .message(error.getMessage())
                        .developerMessage("Exception: " + ex.getMessage())
                        .httpStatus(error.getHttpStatus())
                        .errorCode(error.getErrorCode())
                        .build(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
