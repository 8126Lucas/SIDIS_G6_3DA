package com.sidis.maintenanceservice.infrastructure;

import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceRecordRedis;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRecordQueryRepository extends CrudRepository<MaintenanceRecordRedis, Long> {
    Optional<MaintenanceRecordRedis> findByRecordId(Long recordId);
//    List<MaintenanceRecord> findByAircraft_RegistrationNumber(String registrationNumber);
    List<MaintenanceRecordRedis> findByStatus(MaintenanceStatus status);
    List<MaintenanceRecordRedis> findAll();
    Page<MaintenanceRecordRedis> findAll(Specification<MaintenanceRecordRedis> spec, Pageable pageable);
}
