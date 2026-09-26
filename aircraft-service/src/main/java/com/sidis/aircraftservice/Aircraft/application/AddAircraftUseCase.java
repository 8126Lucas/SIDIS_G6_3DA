package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.UseCase;

import java.time.LocalDate;

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
    private final AircraftRepository aircraftRepository;

    public AddAircraftUseCase(AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    /**
     * Looks up the model for the given modelName, creates a {@link AircraftModel}
     *with the given {@link AircraftSpecs},manufacturer,name and persists it.
     * @param registrationNumber the registration number of the aircraft;
     * @param model the model of the aircraft;
     * @param manufacturingDate the manufacturing date of the aircraft;
     * @param totalOperationalHours the number of hours that the aircraft has operated since getting out of factory;
     * @param totalFlightHours the number of hours that the aircraft has fled since getting out of factory;
     */
    public void execute(String registrationNumber, AircraftModel model, LocalDate manufacturingDate,
                        Double totalOperationalHours, Double totalFlightHours) {
        aircraftRepository.save(new Aircraft(registrationNumber, model,manufacturingDate, totalOperationalHours,totalFlightHours));
    }

    /**
     * Adds a model to persist
     * @param aircraft the model object to be persisted
     */
    public void execute(Aircraft aircraft) {
        aircraftRepository.save(aircraft);
    }
}
