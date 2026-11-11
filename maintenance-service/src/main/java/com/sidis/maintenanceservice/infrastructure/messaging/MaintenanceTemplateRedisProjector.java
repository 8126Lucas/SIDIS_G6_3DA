package com.sidis.maintenanceservice.infrastructure.messaging;

import com.sidis.maintenanceservice.domain.*;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceTemplateRedisProjector {
    private static final Logger log = LoggerFactory.getLogger(MaintenanceTemplateRedisProjector.class);
    private final MaintenanceTemplateQueryRepository maintenanceQueryRepository;

    public MaintenanceTemplateRedisProjector(MaintenanceTemplateQueryRepository maintenanceQueryRepository) {
        this.maintenanceQueryRepository = maintenanceQueryRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.TEMPLATE_SYNC_QUEUE)
    public void syncAircraftToRedis(MaintenanceTemplateSyncEvent event) {
        log.info("Sync event received for maintenance template: {}", event.templateId());

        MaintenanceTemplateRedis templateRedis = maintenanceQueryRepository.findById(event.templateId())
                .orElseGet(MaintenanceTemplateRedis::new);

        templateRedis.setName(event.name());
        templateRedis.setOperation(MaintenanceType.valueOf(String.valueOf(event.operation())));
        templateRedis.setExpectedDuration(event.expectedDuration());
        templateRedis.setTemplateChecklist(event.templateChecklist());
        templateRedis.setAttribute(MaintenanceAttribute.valueOf(String.valueOf(event.attribute())));

        maintenanceQueryRepository.save(templateRedis);

        log.info("Redis cache updated successfully for maintenance template: {}", event.templateId());
    }
}
