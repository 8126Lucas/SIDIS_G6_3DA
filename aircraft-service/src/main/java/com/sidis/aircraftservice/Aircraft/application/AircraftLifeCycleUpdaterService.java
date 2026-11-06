package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftCommandRepository;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import com.sidis.aircraftservice.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AircraftLifeCycleUpdaterService {
    private final AircraftCommandRepository aircraftCommandRepository;
    private final AircraftQueryRepository aircraftQueryRepository;

    public AircraftLifeCycleUpdaterService(AircraftCommandRepository aircraftCommandRepository, AircraftQueryRepository aircraftQueryRepository) {
        this.aircraftCommandRepository = aircraftCommandRepository;
        this.aircraftQueryRepository = aircraftQueryRepository;
    }

    @Transactional
    public void changeAvailability(String registrationNumber, AircraftAvailability newStatus) {
        Aircraft aircraft = aircraftCommandRepository.findById(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found in database"));

        if (newStatus == AircraftAvailability.AVAILABLE) aircraft.activateAircraft();
        else if (newStatus == AircraftAvailability.INACTIVE) aircraft.retireAircraft();
        else if (newStatus == AircraftAvailability.MAINTENANCE) aircraft.sendToMaintenance();
        else if (newStatus == AircraftAvailability.IN_FLIGHT) aircraft.sendAircraftInFlight();

        aircraftCommandRepository.save(aircraft);

        AircraftRedis aircraftRedis = aircraftQueryRepository.findById(registrationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft not found in cache"));

        aircraftRedis.setStatus(newStatus);
        aircraftQueryRepository.save(aircraftRedis);
    }
}