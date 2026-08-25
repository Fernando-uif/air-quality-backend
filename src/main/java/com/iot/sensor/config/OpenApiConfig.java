package com.iot.sensor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IoT Sensor Network API")
                        .version("2.0.0")
                        .description("Backend para red de sensores Raspberry Pi Pico W. "
                                + "Autenticación HMAC-SHA256, protección anti-replay con sequence, "
                                + "almacenamiento en DynamoDB, datos agrupados por país/estado."));
    }
}
