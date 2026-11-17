package com.sidis.maintenanceservice.infrastructure.messaging;

import com.sidis.maintenanceservice.domain.MaintenanceHoursReportRedis;
import com.sidis.maintenanceservice.domain.MaintenanceRecordRedis;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.infrastructure.MaintenanceHoursRepository;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceRecordRedisProjector {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceRecordRedisProjector.class);
    private final MaintenanceRecordQueryRepository maintenanceQueryRepository;
    private final MaintenanceHoursRepository reportRedisRepository;

    public MaintenanceRecordRedisProjector(MaintenanceRecordQueryRepository maintenanceQueryRepository,
                                           MaintenanceHoursRepository reportRedisRepository) {
        this.maintenanceQueryRepository = maintenanceQueryRepository;
        this.reportRedisRepository = reportRedisRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.MAINTENANCE_SYNC_QUEUE)
    public void syncAircraftToRedis(MaintenanceRecordSyncEvent event) {
        log.info("Sync event received for maintenance record: {}", event.recordId());

        MaintenanceRecordRedis recordRedis = maintenanceQueryRepository.findById(event.recordId())
                .orElseGet(MaintenanceRecordRedis::new);

        if (event.aircraftRegistration() != null) {
            Double oldDuration = recordRedis.getDurationHours() != null ? recordRedis.getDurationHours() : 0.0;
            Double newDuration = event.durationHours() != null ? event.durationHours() : 0.0;

            Double durationDelta = newDuration - oldDuration;

            if (durationDelta != 0.0) {
                MaintenanceHoursReportRedis report = reportRedisRepository.findById(event.aircraftRegistration())
                        .orElse(new MaintenanceHoursReportRedis(event.aircraftRegistration(), 0.0));

                report.setTotalMaintenanceHours(report.getTotalMaintenanceHours() + durationDelta);
                reportRedisRepository.save(report);
                log.info("Updated total maintenance hours for aircraft {} by delta: {}", event.aircraftRegistration(), durationDelta);
            }
        }

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
