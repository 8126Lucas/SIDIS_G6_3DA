package com.sidis.aircraftservice.AircraftCatalog;

import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.AircraftCatalog.application.AddModelUseCase;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelUpdater;
import com.sidis.aircraftservice.AircraftCatalog.application.FlightSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;

//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
import com.sidis.aircraftservice.exceptions.DomainException;
import com.sidis.aircraftservice.exceptions.ResourceNotFoundException;
import com.sidis.aircraftservice.infrastructure.client.FlightClientDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/catalog")
public class ModelCatalogController {
    private final AircraftModelSearchService aircraftModelSearchService;
    private final FlightSearchService flightSearchService;
    private final AircraftSearchService aircraftSearchService;
    private final AddModelUseCase addModelUseCase;
    private final AircraftModelUpdater aircraftModelUpdater;

    public ModelCatalogController(AircraftModelSearchService aircraftModelSearchService, FlightSearchService flightSearchService, AircraftSearchService aircraftSearchService, AddModelUseCase addModelUseCase, AircraftModelUpdater aircraftModelUpdater) {
        this.aircraftModelSearchService = aircraftModelSearchService;
        this.flightSearchService = flightSearchService;
        this.aircraftSearchService = aircraftSearchService;
        this.addModelUseCase = addModelUseCase;
        this.aircraftModelUpdater = aircraftModelUpdater;
    }

    /**
     * Returns an aircraft model by its model name.
     *
     * @param modelName the name of the aircraft model
     * @return the aircraft model details
     */
    @GetMapping("/{modelName}")
    public ModelResponse getModelByName(@PathVariable String modelName) {
        AircraftModel model = aircraftModelSearchService.spotAircraftInCatalog(modelName)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Model does not exist in catalog"
                ));
        //preciso disto para o flight, o api/hangar/aircraft nao expoe os detalhes do model, apenas o nome
        return ModelResponse.from(model);
    }

    /**
     * Adds a new aircraft model to the catalog.
     */
    @PostMapping("/model")
    @ResponseStatus(HttpStatus.CREATED)
    public void addModel(@Valid @RequestBody AddModelRequest request) {
        if (request.specs() == null) {
            throw new IllegalArgumentException("Object 'specs' is mandatory in the request body.");
        }
        AircraftSpecs specs = new AircraftSpecs(request.specs().fuelCapacityLiters(),request.specs().maximumRangeKm(),
                request.specs().cruisingSpeedKph(),request.specs().standardSeatingCapacity());

        AircraftModel model = new AircraftModel(request.modelName(), request.manufacturer(),
                specs, request.modelImage());
        addModelUseCase.execute(model);
    }

    /**
     * Patches an existing model
     */
    @PatchMapping("/model/{modelName}")
    public void changeModel(@PathVariable String modelName, @Valid @RequestBody PatchModelRequest request) {
        if (!modelName.equals(request.modelName())) {
            throw new DomainException("Non matching info");
        }
        if (request.modelImage()==null && request.manufacturer()==null
                &&request.specs()==null){
            throw new DomainException("Bad request");
        }
        AircraftModel model=aircraftModelSearchService.spotAircraftInCatalog(modelName)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Model does not exist in catalog"
                        )
                );

        Double fuel = request.specs() != null ? request.specs().fuelCapacityLiters() : null;
        Double range = request.specs() != null ? request.specs().maximumRangeKm() : null;
        Double speed = request.specs() != null ? request.specs().cruisingSpeedKph() : null;
        Integer seating = request.specs() != null ? request.specs().standardSeatingCapacity() : null;

        aircraftModelUpdater.aircraftModelUpdate(model, request.modelImage(), request.manufacturer(), fuel, range, speed, seating);
    }

    /**
     * Returns the top 5 models by the flight assignments
     * or by thr flight hours, this is defined by
     * @param mode
     */
    @GetMapping("/rankings")
    public List<ModelResponse> getModelRanking(@RequestParam String mode) {
        if ("assignments".equals(mode)) {
            Map<AircraftModel, Long> ranking = flightSearchService.findMostAssignedAircraftToFly().entrySet().stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getKey().getModel(),
                            Collectors.summingLong(Map.Entry::getValue)
                    ));

            return ranking.entrySet().stream()
                    .sorted(Map.Entry.<AircraftModel, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> ModelResponse.from(entry.getKey()))
                    .toList();
        }

        if ("flight-hours".equals(mode)) {
            Map<AircraftModel, Double> ranking = aircraftSearchService.findMostUsedModelsByFlyingAircraftHours().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));
            return ranking.entrySet().stream()
                    .sorted(Map.Entry.<AircraftModel, Double>comparingByValue().reversed())
                    .limit(5)
                    .map(entry -> ModelResponse.from(entry.getKey()))
                    .toList();
        }
        return List.of();
    }

    /**
     * Request body for PATCH /catalog/model/{modelName}
     */
    public record PatchModelRequest(@NotBlank String modelName, List<String> modelImage,
                                  String manufacturer, ModelSpecsRequest specs) {}

    /**
     * Request body for POST /catalog/model
     */
    public record AddModelRequest(@NotBlank String modelName, List<String> modelImage,
            @NotBlank String manufacturer, @Valid ModelSpecsRequest specs) {}

    /**
     * Request fragment representing aircraft specs.
     */
    public record ModelSpecsRequest(Double fuelCapacityLiters, Double maximumRangeKm,
            Double cruisingSpeedKph, Integer standardSeatingCapacity) {}

    /**
     * Response fragment representing aircraft specs.
     */
    public record ModelSpecsResponse(Double fuelCapacityLiters, Double maximumRangeKm,
            Double cruisingSpeedKph, Integer standardSeatingCapacity) {
        static ModelSpecsResponse from(AircraftSpecs specs) {
            return new ModelSpecsResponse(specs.getFuelCapacity(), specs.getMaximumRange(),
                    specs.getCruisingSpeed(),specs.getStandardSeatingCapacity());
        }
    }

    /**
     * Response body representing an aircraft model.
     */
    public record ModelResponse(String modelName, List<String> modelImage, String manufacturer, ModelSpecsResponse specs) {
        static ModelResponse from(AircraftModel model) {
            return new ModelResponse(model.getModelName(),model.getModelImage(),
                    model.getManufacturer(),ModelSpecsResponse.from(model.getAircraftModelSpecs()));
        }
    }
}