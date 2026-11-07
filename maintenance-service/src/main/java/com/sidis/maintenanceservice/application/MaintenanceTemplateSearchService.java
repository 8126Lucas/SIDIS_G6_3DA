package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceTemplateRedis;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateCommandRepository;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateQueryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceTemplateSearchService {

    private final MaintenanceTemplateQueryRepository maintenanceTemplateRepository;

    public MaintenanceTemplateSearchService(MaintenanceTemplateQueryRepository maintenanceTemplateRepository) {
        this.maintenanceTemplateRepository = maintenanceTemplateRepository;
    }

    public Optional<MaintenanceTemplateRedis> findByTemplateId(Long templateId) {
        return maintenanceTemplateRepository.findByTemplateId(templateId);
    }

    public List<MaintenanceTemplateRedis> findAll() {
        return maintenanceTemplateRepository.findAll();
    }
}