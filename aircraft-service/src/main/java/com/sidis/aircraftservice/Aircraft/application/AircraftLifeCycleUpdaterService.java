package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AircraftLifeCycleUpdaterService {
    public void changeAvailability(Aircraft aircraft, AircraftAvailability newStatus) {
        switch (newStatus) {
            case AVAILABLE -> aircraft.activateAircraft();
            case MAINTENANCE -> aircraft.sendToMaintenance();
            case INACTIVE -> aircraft.retireAircraft();
            default -> throw new IllegalArgumentException(
                    "Unsupported status transition"
            );
        }
    }
}