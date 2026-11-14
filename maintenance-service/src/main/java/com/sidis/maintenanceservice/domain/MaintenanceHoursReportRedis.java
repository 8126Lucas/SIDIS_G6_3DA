package com.sidis.maintenanceservice.domain;


import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("MaintenanceHoursReport")
public class MaintenanceHoursReportRedis {
    @Id
    private String aircraftRegistration;
    private Double totalMaintenanceHours;

    public MaintenanceHoursReportRedis() {}
    public MaintenanceHoursReportRedis(String aircraftRegistration, Double totalMaintenanceHours) {
        this.aircraftRegistration = aircraftRegistration;
        this.totalMaintenanceHours = totalMaintenanceHours;
    }

    public String getAircraftRegistration() {
        return aircraftRegistration;
    }
    public void setAircraftRegistration(String aircraftRegistration) {
        this.aircraftRegistration = aircraftRegistration;
    }
    public Double getTotalMaintenanceHours() {
        return totalMaintenanceHours;
    }
    public void setTotalMaintenanceHours(Double totalMaintenanceHours) {
        this.totalMaintenanceHours = totalMaintenanceHours;
    }
}
