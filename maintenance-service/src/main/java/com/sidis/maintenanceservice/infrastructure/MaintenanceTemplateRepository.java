package com.sidis.maintenanceservice.infrastructure;

import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaintenanceTemplateRepository extends JpaRepository<MaintenanceTemplate, Long> {
    Optional<MaintenanceTemplate> findByTemplateId(Long recordId);
}
