package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateCommandRepository;
import com.sidis.maintenanceservice.UseCase;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceRecordSyncEvent;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceTemplateSyncEvent;
import com.sidis.maintenanceservice.infrastructure.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

@UseCase
public class AddMaintenanceTemplateUseCase {
    private final MaintenanceTemplateCommandRepository maintenanceTemplateRepository;
    private final RabbitTemplate rabbitTemplate;

    public AddMaintenanceTemplateUseCase(MaintenanceTemplateCommandRepository maintenanceTemplateRepository,
                                         RabbitTemplate rabbitTemplate) {
        this.maintenanceTemplateRepository = maintenanceTemplateRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    public MaintenanceTemplate execute(String name, Double expectedDuration,
                                       Map<String, Boolean> templateChecklist,
                                       MaintenanceType operation,
                                       MaintenanceAttribute attribute) {
        MaintenanceTemplate template = new MaintenanceTemplate(
                name,
                expectedDuration,
                templateChecklist,
                operation,
                attribute
        );
        maintenanceTemplateRepository.save(template);

        MaintenanceTemplateSyncEvent event = new MaintenanceTemplateSyncEvent(
                template.getTemplateId(),
                template.getName(),
                template.getExpectedDuration(),
                template.getTemplateChecklist(),
                template.getOperation(),
                template.getAttribute()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TEMPLATE_EXCHANGE_NAME,
                "maintenancetemplate.event.created",
                event
        );

        return template;
    }

    public MaintenanceTemplate execute(MaintenanceTemplate maintenanceTemplate) {
        maintenanceTemplateRepository.save(maintenanceTemplate);

        MaintenanceTemplateSyncEvent event = new MaintenanceTemplateSyncEvent(
                maintenanceTemplate.getTemplateId(),
                maintenanceTemplate.getName(),
                maintenanceTemplate.getExpectedDuration(),
                maintenanceTemplate.getTemplateChecklist(),
                maintenanceTemplate.getOperation(),
                maintenanceTemplate.getAttribute()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TEMPLATE_EXCHANGE_NAME,
                "maintenancetemplate.event.created",
                event
        );

        return maintenanceTemplate;
    }
}
