package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftCommandRepository;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import com.sidis.aircraftservice.exceptions.ResourceNotFoundException;
import com.sidis.aircraftservice.infrastructure.messaging.AircraftSyncEvent;
import com.sidis.aircraftservice.infrastructure.messaging.RabbitMQConfig;
import jakarta.transaction.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class AircraftLifeCycleUpdaterService {
    private final AircraftCommandRepository aircraftCommandRepository;
    private final AircraftQueryRepository aircraftQueryRepository;
    private final RabbitTemplate rabbitTemplate;

    public AircraftLifeCycleUpdaterService(AircraftCommandRepository aircraftCommandRepository,
                                           AircraftQueryRepository aircraftQueryRepository,
                                           RabbitTemplate rabbitTemplate) {
        this.aircraftCommandRepository = aircraftCommandRepository;
        this.aircraftQueryRepository = aircraftQueryRepository;
        this.rabbitTemplate = rabbitTemplate;
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

        AircraftSyncEvent event = new AircraftSyncEvent(
                aircraft.getRegistrationNumber(),
                aircraft.getModel().getModelName(),
                aircraft.getStatus().name(),
                aircraft.getManufacturingDate(),
                aircraft.getTotalOperationalHours(),
                aircraft.getTotalFlightHours(),
                aircraft.getMeanRange(),
                aircraft.getFeatures()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.AIRCRAFT_EXCHANGE_NAME,
                "aircraft.event.updated",
                event
        );
    }
}