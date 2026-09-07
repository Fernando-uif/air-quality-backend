package com.iot.sensor.service;

import com.iot.sensor.model.dto.MeasurementRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

/**
 * Servicio de seguridad HMAC-SHA256.
 *
 * Replica la funcionalidad de security.py:
 * - Genera firma HMAC-SHA256 sobre "device_id:value:timestamp:sequence"
 * - Verifica firmas con timing-safe comparison
 * - Valida ventana de timestamp (±10 min)
 * - Valida sequence > last_sequence (anti-replay)
 */
@Service
@Slf4j
public class HmacService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${security.timestamp-tolerance}")
    private int timestampTolerance;

    /**
     * Genera firma HMAC-SHA256.
     * Mensaje: "device_id:value:timestamp:sequence"
     *
     * El formato de value debe coincidir exactamente con lo que genera MicroPython:
     * "{}".format(float) → e.g. "23.42", "0.3333", "2.38"
     * En Java, Double.toString() produce el mismo formato minimal ("23.42", no "23.420000").
     */
    public String generateSignature(String secretKey, String deviceId, Double value, Long timestamp, Long sequence) {
        // Usar Double.toString() que produce representación minimal igual que Python
        // Ejemplo: 23.42 → "23.42", 0.0 → "0.0"
        String valueStr = value.toString();

        String message = String.format("%s:%s:%d:%d", deviceId, valueStr, timestamp, sequence);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating HMAC signature: {}", e.getMessage());
            throw new RuntimeException("Error generating HMAC signature", e);
        }
    }

    /**
     * Verifica la firma HMAC-SHA256 usando timing-safe comparison.
     */
    public boolean verifySignature(String secretKey, String deviceId, Double value,
                                   Long timestamp, Long sequence, String receivedSignature) {
        String expected = generateSignature(secretKey, deviceId, value, timestamp, sequence);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                receivedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Verifica firma HMAC-SHA256 para múltiples readings.
     * Mensaje: "device_id:sensor1_value:sensor2_value:...:timestamp:sequence"
     * Los values se concatenan en orden del arreglo readings.
     */
    public boolean verifySignatureMulti(String secretKey, String deviceId,
                                        java.util.List<MeasurementRequestDto.ReadingDto> readings,
                                        Long timestamp, Long sequence, String receivedSignature) {
        // Construir mensaje: device_id:val1:val2:...:timestamp:sequence
        //
        // ROBUSTEZ DE FORMATO: el micro firma el valor como texto (str(value) en Python).
        // Según el tipo que use el dispositivo, un valor puede llegar como entero ("45", "0")
        // o como decimal ("45.0", "0.0"). Para NO acoplar el backend al formato del micro
        // (y soportar sensores nuevos que devuelvan enteros u otros tipos), se prueban las
        // representaciones candidatas de cada valor y se acepta si ALGUNA combinación valida.
        //
        // Se construye la lista de representaciones por reading y se recorren todas las
        // combinaciones (producto cartesiano). En la práctica son pocos sensores y pocas
        // variantes por valor, así que el costo es mínimo.
        java.util.List<java.util.List<String>> perReading = new java.util.ArrayList<>();
        for (MeasurementRequestDto.ReadingDto r : readings) {
            perReading.add(valueRepresentations(r.getValue()));
        }

        String suffix = ":" + timestamp + ":" + sequence;
        return matchesAnyCombination(secretKey, deviceId, perReading, 0, new StringBuilder(deviceId),
                suffix, receivedSignature);
    }

    /**
     * Representaciones textuales candidatas de un valor numérico, para tolerar el formato
     * que use el micro (entero vs decimal). Replica lo que produciría str() en Python.
     */
    private java.util.List<String> valueRepresentations(Double value) {
        java.util.List<String> reps = new java.util.ArrayList<>();
        if (value == null) {
            reps.add("null");   // por si el micro alguna vez firmara un nulo textual
            reps.add("None");   // str(None) en Python
            return reps;
        }
        // Representación por defecto de Double (ej. "45.0", "0.0", "23.4")
        reps.add(value.toString());
        // Si es un entero exacto, el micro pudo firmarlo como int ("45", "0")
        if (value == Math.floor(value) && !value.isInfinite()) {
            long asLong = value.longValue();
            String asInt = Long.toString(asLong);
            if (!reps.contains(asInt)) {
                reps.add(asInt);
            }
        }
        return reps;
    }

    /**
     * Recorre recursivamente el producto cartesiano de representaciones y valida el HMAC.
     * Devuelve true si alguna combinación produce la firma recibida.
     */
    private boolean matchesAnyCombination(String secretKey, String deviceId,
                                          java.util.List<java.util.List<String>> perReading,
                                          int idx, StringBuilder acc, String suffix,
                                          String receivedSignature) {
        if (idx == perReading.size()) {
            String message = acc.toString() + suffix;
            String expected = computeHmac(secretKey, message);
            if (expected == null) {
                return false;
            }
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8));
        }
        for (String rep : perReading.get(idx)) {
            int len = acc.length();
            acc.append(":").append(rep);
            if (matchesAnyCombination(secretKey, deviceId, perReading, idx + 1, acc, suffix,
                    receivedSignature)) {
                return true;
            }
            acc.setLength(len); // backtrack
        }
        return false;
    }

    /** Calcula el HMAC-SHA256 de un mensaje y lo devuelve en hex, o null si falla. */
    private String computeHmac(String secretKey, String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying multi HMAC: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Verifica que el timestamp esté dentro de la tolerancia (±10 min por defecto).
     */
    public boolean verifyTimestamp(Long timestamp) {
        long currentTime = Instant.now().getEpochSecond();
        long difference = Math.abs(currentTime - timestamp);
        return difference <= timestampTolerance;
    }

    /**
     * Verifica que el sequence sea mayor al último registrado.
     * Si lastSequence es 0 (dispositivo nuevo), acepta cualquier sequence.
     */
    public boolean verifySequence(Long receivedSequence, Long lastSequence) {
        if (lastSequence == null || lastSequence == 0) {
            return true;
        }
        return receivedSequence > lastSequence;
    }

    /**
     * Verifica firma de auto-registro.
     * Mensaje: "device_id:timestamp"
     */
    public boolean verifyAutoRegisterSignature(String secretKey, String deviceId, Long timestamp, String receivedSignature) {
        String message = deviceId + ":" + timestamp;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String expected = bytesToHex(hmacBytes);
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying auto-register HMAC: {}", e.getMessage());
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
