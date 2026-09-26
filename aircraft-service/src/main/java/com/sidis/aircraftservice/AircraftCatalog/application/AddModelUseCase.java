package com.sidis.aircraftservice.AircraftCatalog.application;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelRepository;
import com.sidis.aircraftservice.UseCase;

import java.util.List;

/**
 * Use case: a backoffice operator adds a new aircraft model to the aircraft catalog.
 *
 * <p>Given an  manufacturer, model name, seating capacity, fuel capacity, maximum range, and cruising speed,
 * this use case saves a new {@link AircraftModel} to the catalog.</p>
 *
 * <p>If the model name is already in the catalog the database unique constraint on the
 * {@code modelName} column will reject the duplicate.</p>
 */

@UseCase
public class AddModelUseCase {
    private final AircraftModelRepository aircraftModelRepository;

    public AddModelUseCase(AircraftModelRepository aircraftModelRepository) {
        this.aircraftModelRepository = aircraftModelRepository;
    }

    /**
     * Looks up the model for the given modelName, creates a {@link AircraftModel}
     *with the given {@link AircraftSpecs},manufacturer,name and persists it.
     * @param modelName the model name of the model to add;
     * @param manufacturer the manufacturer of the model;
     * @param modelImage the list of images of the model; can be null;
     * @param aircraftModelSpecs the group of physical characteristics of the model
     */
    public void execute(String modelName, String manufacturer,List <String> modelImage,AircraftSpecs aircraftModelSpecs) {
        aircraftModelRepository.save(new AircraftModel(modelName, manufacturer,aircraftModelSpecs, modelImage));
    }

    /**
     * Adds a model to persist
     * @param aircraftModel the model object to be persisted
     */
    public void execute(AircraftModel aircraftModel) {
        aircraftModelRepository.save(aircraftModel);
    }

    public record modelSpecsInput(Double fuelCapacityLiters, Double maximumRangeKm,
                                  Double cruisingSpeedKph, Integer standardSeatingCapacity){}
}
