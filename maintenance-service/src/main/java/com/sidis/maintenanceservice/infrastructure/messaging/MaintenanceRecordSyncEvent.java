package com.sidis.maintenanceservice.infrastructure.messaging;

import com.sidis.maintenanceservice.domain.DoneListRedis;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.domain.UsedPartRedis;

import java.time.LocalDate;
import java.util.List;

public record MaintenanceRecordSyncEvent (
        Long recordId,
        String aircraftRegistration,
        Long maintenanceTemplateId,
        String description,
        List<String> techniciansUsername,
        String supervisorUsername,
        LocalDate startDate,
        LocalDate endDate,
        List<UsedPartRedis> usedParts,
        Double durationHours,
        DoneListRedis doneList,
        MaintenanceStatus status
) {}
