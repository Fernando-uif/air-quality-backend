package com.iot.sensor.model.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorMessage implements Serializable {

    @JsonProperty("http_status")
    private int httpStatus;

    @JsonProperty("error_code")
    private String errorCode;

    private String message;

    @JsonProperty("developer_message")
    private String developerMessage;
}
