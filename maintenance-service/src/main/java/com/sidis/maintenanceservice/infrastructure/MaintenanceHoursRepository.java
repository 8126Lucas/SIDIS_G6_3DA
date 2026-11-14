package com.sidis.maintenanceservice.infrastructure;

import com.sidis.maintenanceservice.domain.MaintenanceHoursReportRedis;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaintenanceHoursRepository extends CrudRepository <MaintenanceHoursReportRedis, String> {
}
