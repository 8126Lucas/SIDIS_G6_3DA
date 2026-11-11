package com.sidis.maintenanceservice.infrastructure.messaging;

import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceType;

import java.util.Map;

public record MaintenanceTemplateSyncEvent (
        Long templateId,
        String name,
        Double expectedDuration,
        Map<String, Boolean> templateChecklist,
        MaintenanceType operation,
        MaintenanceAttribute attribute
) {}
