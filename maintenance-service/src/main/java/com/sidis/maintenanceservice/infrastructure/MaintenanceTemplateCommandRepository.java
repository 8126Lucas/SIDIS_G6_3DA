package com.sidis.maintenanceservice.infrastructure;

import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaintenanceTemplateCommandRepository extends JpaRepository<MaintenanceTemplate, Long> {
    Optional<MaintenanceTemplate> findByTemplateId(Long recordId);
}
