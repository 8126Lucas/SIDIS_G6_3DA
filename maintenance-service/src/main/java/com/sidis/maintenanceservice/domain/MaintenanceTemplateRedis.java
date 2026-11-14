package com.sidis.maintenanceservice.domain;


import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.util.Map;

@RedisHash("MaintenanceTemplate")
public class MaintenanceTemplateRedis {
    @Id
    private Long templateId;
    @Indexed
    private String name;
    private Double expectedDuration;
    private Map<String, Boolean> templateChecklist;
    @Indexed
    private MaintenanceType operation;
    @Indexed
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

    public MaintenanceTemplateRedis() {}

    public Long getTemplateId() {
        return templateId;
    }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    public String getName() {
        return name;
    }
    public void setName(String name) { this.name = name; }

    public Double getExpectedDuration() {
        return expectedDuration;
    }
    public void setExpectedDuration(Double expectedDuration) { this.expectedDuration = expectedDuration; }

    public Map<String, Boolean> getTemplateChecklist() {
        return templateChecklist;
    }
    public void setTemplateChecklist(Map<String, Boolean> templateChecklist) { this.templateChecklist = templateChecklist; }

    public MaintenanceType getOperation() {
        return operation;
    }
    public void setOperation(MaintenanceType operation) { this.operation = operation; }

    public MaintenanceAttribute getAttribute() {
        return attribute;
    }
    public void setAttribute(MaintenanceAttribute attribute) {
        this.attribute = attribute;
    }
}
