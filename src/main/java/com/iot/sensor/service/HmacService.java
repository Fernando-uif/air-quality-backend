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
        StringBuilder sb = new StringBuilder();
        sb.append(deviceId);
        for (MeasurementRequestDto.ReadingDto r : readings) {
            sb.append(":").append(r.getValue().toString());
        }
        sb.append(":").append(timestamp);
        sb.append(":").append(sequence);

        String message = sb.toString();

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
            log.error("Error verifying multi HMAC: {}", e.getMessage());
            return false;
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
