package com.sidis.aircraftservice.AircraftCatalog;

import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
import com.sidis.aircraftservice.AircraftCatalog.application.AddModelUseCase;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelUpdater;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;

//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
import com.sidis.aircraftservice.exceptions.DomainException;
import com.sidis.aircraftservice.exceptions.ResourceNotFoundException;
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
//    private final FlightSearchService flightSearchService;
    private final AircraftSearchService aircraftSearchService;
    private final AddModelUseCase addModelUseCase;
    private final AircraftModelUpdater aircraftModelUpdater;

    public ModelCatalogController(AircraftModelSearchService aircraftModelSearchService, /* FlightSearchService flightSearchService,*/ AircraftSearchService aircraftSearchService, AddModelUseCase addModelUseCase, AircraftModelUpdater aircraftModelUpdater) {
        this.aircraftModelSearchService = aircraftModelSearchService;
//        this.flightSearchService = flightSearchService;
        this.aircraftSearchService = aircraftSearchService;
        this.addModelUseCase = addModelUseCase;
        this.aircraftModelUpdater = aircraftModelUpdater;
    }

    /**
     * Adds a new aircraft model to the catalog.
     */
    @PostMapping("/model")
    @ResponseStatus(HttpStatus.CREATED)
    public void addModel(@Valid @RequestBody AddModelRequest request) {
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
        aircraftModelUpdater.aircraftModelUpdate(model,request.modelImage(),request.manufacturer(),request.specs().fuelCapacityLiters(),
                request.specs().maximumRangeKm(),request.specs().cruisingSpeedKph(),request.specs().standardSeatingCapacity());
    }

    /**
     * Returns the top 5 models by the flight assignments
     * or by thr flight hours, this is defined by
     * @param mode
     */
/*    @GetMapping("/rankings")
    public List<ModelResponse> getModelRanking(@RequestParam String mode) {
        if (mode.equals("assignments")) {
            Map<AircraftModel, Long> ranking = flightSearchService.findMostAssignedAircraftToFly().stream()
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

        if (mode.equals("flight-hours")) {
            Map<AircraftModel, Double> ranking =
                    aircraftSearchService.findMostUsedModelsByFlyingAircraftHours().stream()
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
*/
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