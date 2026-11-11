package com.sidis.maintenanceservice.infrastructure.messaging;

import com.sidis.maintenanceservice.domain.MaintenanceRecordRedis;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceRecordRedisProjector {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceRecordRedisProjector.class);
    private final MaintenanceRecordQueryRepository maintenanceQueryRepository;

    public MaintenanceRecordRedisProjector(MaintenanceRecordQueryRepository maintenanceQueryRepository) {
        this.maintenanceQueryRepository = maintenanceQueryRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.MAINTENANCE_SYNC_QUEUE)
    public void syncAircraftToRedis(MaintenanceRecordSyncEvent event) {
        log.info("Sync event received for maintenance record: {}", event.recordId());

        MaintenanceRecordRedis recordRedis = maintenanceQueryRepository.findById(event.recordId())
                .orElseGet(MaintenanceRecordRedis::new);

        recordRedis.setAircraftRegistration(event.aircraftRegistration());
        recordRedis.setStatus(MaintenanceStatus.valueOf(String.valueOf(event.status())));
        recordRedis.setMaintenanceTemplateId(event.maintenanceTemplateId());
        recordRedis.setTechniciansUsername(event.techniciansUsername());
        recordRedis.setSupervisorUsername(event.supervisorUsername());
        recordRedis.setDescription(event.description());
        recordRedis.setStartDate(event.startDate());
        recordRedis.setEndDate(event.endDate());
        recordRedis.setUsedParts(event.usedParts());
        recordRedis.setDurationHours(event.durationHours());
        recordRedis.setDoneList(event.doneList());


        maintenanceQueryRepository.save(recordRedis);

        log.info("Redis cache updated successfully for maintenance record: {}", event.recordId());
    }
}
