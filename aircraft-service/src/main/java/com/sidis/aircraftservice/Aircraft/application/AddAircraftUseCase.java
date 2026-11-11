package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftCommandRepository;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelCommandRepository;
import com.sidis.aircraftservice.UseCase;
import com.sidis.aircraftservice.exceptions.DomainException;
import com.sidis.aircraftservice.infrastructure.messaging.AircraftSyncEvent;
import com.sidis.aircraftservice.infrastructure.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Use case: a ATCC operator adds a new aircraft instance to the company aircraft.
 *
 * <p>Given an  manufacturer, model name, seating capacity, fuel capacity, maximum range, and cruising speed,
 * this use case saves a new {@link AircraftModel} to the catalog.</p>
 *
 * <p>If the model name is already in the catalog the database unique constraint on the
 * {@code modelName} column will reject the duplicate.</p>
 */

@UseCase
public class AddAircraftUseCase {
    private final AircraftCommandRepository aircraftCommandRepository;
    private final AircraftModelCommandRepository aircraftModelCommandRepository;
    private final AircraftQueryRepository aircraftQueryRepository;
    private final RabbitTemplate rabbitTemplate;

    public AddAircraftUseCase(AircraftCommandRepository aircraftCommandRepository,
                              AircraftModelCommandRepository aircraftModelCommandRepository,
                              AircraftQueryRepository aircraftQueryRepository,
                              RabbitTemplate rabbitTemplate) {
        this.aircraftCommandRepository = aircraftCommandRepository;
        this.aircraftModelCommandRepository = aircraftModelCommandRepository;
        this.aircraftQueryRepository = aircraftQueryRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Looks up the model for the given modelName, creates a {@link AircraftModel}
     *with the given {@link AircraftSpecs},manufacturer,name and persists it.
     * @param registrationNumber the registration number of the aircraft;
     * @param modelName the name of the model of the aircraft;
     * @param manufacturingDate the manufacturing date of the aircraft;
     * @param totalOperationalHours the number of hours that the aircraft has operated since getting out of factory;
     * @param totalFlightHours the number of hours that the aircraft has fled since getting out of factory;
     */
    public void execute(String registrationNumber, String modelName, LocalDate manufacturingDate,
                        Double totalOperationalHours, Double totalFlightHours) {
        Optional<AircraftModel> model = aircraftModelCommandRepository.findByModelName(modelName);
        if(model.isPresent()) {
            Aircraft aircraft = new Aircraft(registrationNumber, model.get(), manufacturingDate, totalOperationalHours,totalFlightHours);
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
                    "aircraft.event.created",
                    event
            );
        }
        else {
            throw new DomainException("Aircraft Model does not exist!");
        }
    }

    /**
     * Adds a model to persist
     * @param aircraft the model object to be persisted
     */
    public void execute(Aircraft aircraft) {
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
                "aircraft.event.created",
                event
        );
    }
}
