package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateRepository;
import com.sidis.maintenanceservice.UseCase;

import java.util.Map;

@UseCase
public class AddMaintenanceTemplateUseCase {
    private final MaintenanceTemplateRepository maintenanceTemplateRepository;

    public AddMaintenanceTemplateUseCase(MaintenanceTemplateRepository maintenanceTemplateRepository) {
        this.maintenanceTemplateRepository = maintenanceTemplateRepository;
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
        return maintenanceTemplateRepository.save(template);
    }

    public MaintenanceTemplate execute(MaintenanceTemplate maintenanceTemplate) {
        return maintenanceTemplateRepository.save(maintenanceTemplate);
    }
}
