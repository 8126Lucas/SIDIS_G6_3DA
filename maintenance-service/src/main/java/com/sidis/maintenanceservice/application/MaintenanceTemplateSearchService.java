package com.sidis.maintenanceservice.application;

import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MaintenanceTemplateSearchService {

    private final MaintenanceTemplateRepository maintenanceTemplateRepository;

    public MaintenanceTemplateSearchService(MaintenanceTemplateRepository maintenanceTemplateRepository) {
        this.maintenanceTemplateRepository = maintenanceTemplateRepository;
    }

    public Optional<MaintenanceTemplate> findByTemplateId(Long templateId) {
        return maintenanceTemplateRepository.findByTemplateId(templateId);
    }

    public List<MaintenanceTemplate> findAll() {
        return maintenanceTemplateRepository.findAll();
    }
}