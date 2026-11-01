package com.sidis.flightservice.Flight.infrastructure.client.aircraft;

import java.time.LocalDate;
import java.util.List;

public record AircraftClientDTO (
        String registrationNumber,
        String modelName,
        String status,
        LocalDate manufacturingDate,
        Double totalFlightHours,
        Double meanRange,
        List<String> features,
        SeatingPackClientDTO seatingPackDTO
){}
