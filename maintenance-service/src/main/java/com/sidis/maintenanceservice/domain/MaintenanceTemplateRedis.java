package com.sidis.maintenanceservice.domain;


import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.Map;

@RedisHash("MaintenanceTemplate")
public class MaintenanceTemplateRedis {
    @Id
    private Long templateId;
    private String name;
    private Double expectedDuration;
    private Map<String, Boolean> templateChecklist;
    private MaintenanceType operation;
    private MaintenanceAttribute attribute;

    public MaintenanceTemplateRedis(String name, Double expectedDuration,
                                    Map<String, Boolean> templateChecklist, MaintenanceType operation,
                                    MaintenanceAttribute attribute) {
        this.name = name;
        this.expectedDuration = expectedDuration;
        this.templateChecklist = templateChecklist;
        this.operation = operation;
        this.attribute = attribute;
    }

    protected MaintenanceTemplateRedis() {}

    public Long getTemplateId() {
        return templateId;
    }

    public String getName() {
        return name;
    }

    public Double getExpectedDuration() {
        return expectedDuration;
    }

    public Map<String, Boolean> getTemplateChecklist() {
        return templateChecklist;
    }

    public MaintenanceType getOperation() {
        return operation;
    }

    public MaintenanceAttribute getAttribute() {
        return attribute;
    }

    public void changeAttribute(MaintenanceAttribute attribute) {
        this.attribute = attribute;
    }
}
