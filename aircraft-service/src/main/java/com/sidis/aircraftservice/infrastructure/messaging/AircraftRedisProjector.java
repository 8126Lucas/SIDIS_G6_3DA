package com.sidis.aircraftservice.infrastructure.messaging;

import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AircraftRedisProjector {
    private static final Logger log = LoggerFactory.getLogger(AircraftRedisProjector.class);
    private final AircraftQueryRepository aircraftQueryRepository;

    public AircraftRedisProjector(AircraftQueryRepository aircraftQueryRepository) {
        this.aircraftQueryRepository = aircraftQueryRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.SYNC_QUEUE)
    public void syncAircraftToRedis(AircraftSyncEvent event) {
        log.info("Sync event received for aircraft: {}", event.registrationNumber());

        AircraftRedis aircraftRedis = aircraftQueryRepository.findById(event.registrationNumber())
                .orElseGet(AircraftRedis::new);

        aircraftRedis.setRegistrationNumber(event.registrationNumber());
        aircraftRedis.setModelName(event.modelName());
        aircraftRedis.setStatus(AircraftAvailability.valueOf(event.status()));
        aircraftRedis.setManufacturingDate(event.manufacturingDate());
        aircraftRedis.setTotalOperationalHours(event.totalOperationalHours());
        aircraftRedis.setTotalFlightHours(event.totalFlightHours());
        aircraftRedis.setMeanRange(event.meanRange());
        aircraftRedis.setFeatures(event.features());

        aircraftQueryRepository.save(aircraftRedis);

        log.info("Redis cache updated successfully for aircraft: {}", event.registrationNumber());
    }
}