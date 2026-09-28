package com.sidis.maintenanceservice.domain;

import jakarta.persistence.*;

import java.util.Map;

@Entity
public class MaintenanceTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long templateId;

    private String name;

    @Column(nullable = false)
    private Double expectedDuration;

    @ElementCollection
    @CollectionTable(name = "maintenance_template_checklist")
    @MapKeyColumn(name = "check_item")
    @Column(name = "is_required")
    private Map<String, Boolean> templateChecklist;

    @Enumerated(EnumType.STRING)
    private MaintenanceType operation;

    @Enumerated(EnumType.STRING)
    private MaintenanceAttribute attribute;

    public MaintenanceTemplate(String name,Double expectedDuration,
            Map<String, Boolean> templateChecklist,MaintenanceType operation,
                               MaintenanceAttribute attribute) {
        this.name = name;
        this.expectedDuration = expectedDuration;
        this.templateChecklist = templateChecklist;
        this.operation = operation;
        this.attribute = attribute;
    }

    protected MaintenanceTemplate() {}

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
