package com.sidis.maintenanceservice.infrastructure;

import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceTemplateRedis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceTemplateQueryRepository extends CrudRepository<MaintenanceTemplateRedis, Long> {
    List<MaintenanceTemplateRedis> findAll();
    Optional<MaintenanceTemplateRedis> findByTemplateId(Long recordId);
}
