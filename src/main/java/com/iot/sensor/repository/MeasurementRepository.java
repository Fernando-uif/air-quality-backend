package com.iot.sensor.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.iot.sensor.model.entity.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MeasurementRepository {

    private final DynamoDBMapper dynamoDBMapper;

    /**
     * Guarda una medición.
     */
    public Measurement save(Measurement measurement) {
        dynamoDBMapper.save(measurement);
        return measurement;
    }

    /**
     * Última medición de un dispositivo (Query con Limit=1 descendente).
     */
    public Optional<Measurement> findLatestByDeviceId(String deviceId) {
        DynamoDBQueryExpression<Measurement> queryExpression = new DynamoDBQueryExpression<Measurement>()
                .withHashKeyValues(Measurement.builder().deviceId(deviceId).build())
                .withScanIndexForward(false)
                .withLimit(1);

        List<Measurement> results = dynamoDBMapper.queryPage(Measurement.class, queryExpression).getResults();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    /**
     * Histórico de mediciones de un dispositivo en un rango de tiempo.
     * Query: PK = deviceId, SK between startTime and endTime, orden descendente.
     */
    public List<Measurement> findByDeviceIdAndTimeRange(String deviceId, Long startTime, Long endTime, int limit) {
        DynamoDBQueryExpression<Measurement> queryExpression = new DynamoDBQueryExpression<Measurement>()
                .withHashKeyValues(Measurement.builder().deviceId(deviceId).build())
                .withRangeKeyCondition("timestamp", new Condition()
                        .withComparisonOperator(ComparisonOperator.BETWEEN)
                        .withAttributeValueList(
                                new AttributeValue().withN(startTime.toString()),
                                new AttributeValue().withN(endTime.toString())
                        ))
                .withScanIndexForward(false)
                .withLimit(limit);

        return dynamoDBMapper.queryPage(Measurement.class, queryExpression).getResults();
    }

}
