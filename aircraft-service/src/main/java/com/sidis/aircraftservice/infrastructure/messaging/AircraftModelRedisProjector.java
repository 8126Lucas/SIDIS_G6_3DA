package com.sidis.aircraftservice.infrastructure.messaging;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class AircraftModelRedisProjector {
    private static final Logger log = LoggerFactory.getLogger(AircraftModelRedisProjector.class);
    private final AircraftModelQueryRepository aircraftModelQueryRepository;

    public AircraftModelRedisProjector(AircraftModelQueryRepository aircraftModelQueryRepository) {
        this.aircraftModelQueryRepository = aircraftModelQueryRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.MODEL_SYNC_QUEUE)
    public void syncAircraftToRedis(AircraftModelSyncEvent event) {
        log.info("Sync event received for aircraft model: {}", event.modelName());

        AircraftModelRedis aircraftModelRedis = aircraftModelQueryRepository.findByModelName(event.modelName())
                .orElseGet(AircraftModelRedis::new);

        aircraftModelRedis.changeModelName(event.modelName());
        aircraftModelRedis.changeManufacturer(event.manufacturer());
        aircraftModelRedis.changeAircraftModelSpecs(event.aircraftModelSpecs());
        aircraftModelRedis.changeModelImage(event.modelImage());

        aircraftModelQueryRepository.save(aircraftModelRedis);

        log.info("Redis cache updated successfully for aircraft model: {}", event.modelName());
    }
}
