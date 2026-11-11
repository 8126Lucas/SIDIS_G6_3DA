package com.sidis.aircraftservice.AircraftCatalog.application;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecsRedis;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelCommandRepository;
import com.sidis.aircraftservice.UseCase;
import com.sidis.aircraftservice.infrastructure.messaging.AircraftModelSyncEvent;
import com.sidis.aircraftservice.infrastructure.messaging.AircraftSyncEvent;
import com.sidis.aircraftservice.infrastructure.messaging.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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
    private final AircraftModelCommandRepository aircraftModelRepository;
    private final RabbitTemplate rabbitTemplate;

    public AddModelUseCase(AircraftModelCommandRepository aircraftModelRepository,
                           RabbitTemplate rabbitTemplate) {
        this.aircraftModelRepository = aircraftModelRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Looks up the model for the given modelName, creates a {@link AircraftModel}
     *with the given {@link AircraftSpecs},manufacturer,name and persists it.
     * @param modelName the model name of the model to add;
     * @param manufacturer the manufacturer of the model;
     * @param modelImage the list of images of the model; can be null;
     * @param aircraftModelSpecs the group of physical characteristics of the model
     */
    public void execute(String modelName, String manufacturer, List <String> modelImage, AircraftSpecs aircraftModelSpecs) {
        AircraftModel model = new AircraftModel(modelName, manufacturer,aircraftModelSpecs, modelImage);
        aircraftModelRepository.save(model);

        AircraftSpecsRedis specs = new AircraftSpecsRedis(
                model.getAircraftModelSpecs().getFuelCapacity(),
                model.getAircraftModelSpecs().getMaximumRange(),
                model.getAircraftModelSpecs().getCruisingSpeed(),
                model.getAircraftModelSpecs().getStandardSeatingCapacity());

        AircraftModelSyncEvent event = new AircraftModelSyncEvent(
                model.getModelName(),
                model.getModelImage(),
                model.getManufacturer(),
                specs
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MODEL_EXCHANGE_NAME,
                "aircraftmodel.event.created",
                event
        );
    }

    /**
     * Adds a model to persist
     * @param aircraftModel the model object to be persisted
     */
    public void execute(AircraftModel aircraftModel) {
        aircraftModelRepository.save(aircraftModel);

        AircraftSpecsRedis specs = new AircraftSpecsRedis(
                aircraftModel.getAircraftModelSpecs().getFuelCapacity(),
                aircraftModel.getAircraftModelSpecs().getMaximumRange(),
                aircraftModel.getAircraftModelSpecs().getCruisingSpeed(),
                aircraftModel.getAircraftModelSpecs().getStandardSeatingCapacity());

        AircraftModelSyncEvent event = new AircraftModelSyncEvent(
                aircraftModel.getModelName(),
                aircraftModel.getModelImage(),
                aircraftModel.getManufacturer(),
                specs
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.MODEL_EXCHANGE_NAME,
                "aircraftmodel.event.created",
                event
        );
    }

    public record modelSpecsInput(Double fuelCapacityLiters, Double maximumRangeKm,
                                  Double cruisingSpeedKph, Integer standardSeatingCapacity){}
}
