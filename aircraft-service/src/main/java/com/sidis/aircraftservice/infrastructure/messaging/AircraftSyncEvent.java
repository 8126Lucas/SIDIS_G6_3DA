package com.sidis.aircraftservice.infrastructure.messaging;

import java.time.LocalDate;
import java.util.List;

public record AircraftSyncEvent(
        String registrationNumber,
        String modelName,
        String status,
        LocalDate manufacturingDate,
        Double totalOperationalHours,
        Double totalFlightHours,
        Double meanRange,
        List<String> features
        // Não está implementado para componentes e certificações (só se necessário)
) {}