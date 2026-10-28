package com.sidis.aircraftservice.Aircraft;

import com.sidis.aircraftservice.Aircraft.application.*;
import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
import com.sidis.aircraftservice.Aircraft.domain.*;
import com.sidis.aircraftservice.infrastructure.client.FlightRestClientService;
import com.sidis.aircraftservice.infrastructure.client.RouteClientDTO;
import com.sidis.aircraftservice.infrastructure.client.RouteRestClientService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
//import com.sidis.aircraftservice.Route.RouteController;
import com.sidis.aircraftservice.exceptions.DomainException;
import com.sidis.aircraftservice.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hangar")
public class HangarController {
    private final AddAircraftUseCase addAircraftUseCase;
    private final AircraftModelSearchService aircraftModelSearchService;
    private final AircraftLifeCycleUpdaterService aircraftLifeCycleUpdaterService;
    private final AircraftSearchService aircraftSearchService;
    private final RouteRestClientService routeClient;
    private final RouteSearchService routeSearchService;
    private final FlightRestClientService flightClient;
//    private final FlightSearchService flightSearchService;
    private final FuelEfficiencyService fuelEfficiencyService;

    public HangarController(AddAircraftUseCase addAircraftUseCase,
                            AircraftModelSearchService aircraftModelSearchService,
                            AircraftLifeCycleUpdaterService aircraftLifeCycleUpdaterService,
                            AircraftSearchService aircraftSearchService,
                            RouteRestClientService routeClient,
                            RouteSearchService routeSearchService,
                            FlightRestClientService flightClient,
//                            FlightSearchService flightSearchService,
                            FuelEfficiencyService fuelEfficiencyService) {
        this.addAircraftUseCase = addAircraftUseCase;
        this.aircraftModelSearchService = aircraftModelSearchService;
        this.aircraftLifeCycleUpdaterService = aircraftLifeCycleUpdaterService;
        this.aircraftSearchService = aircraftSearchService;
        this.routeClient = routeClient;
        this.routeSearchService = routeSearchService;
        this.flightClient = flightClient;
//        this.flightSearchService = flightSearchService;
        this.fuelEfficiencyService = fuelEfficiencyService;
    }
    @PostMapping("/aircraft")
    @ResponseStatus(HttpStatus.CREATED)
    public void addAircraft(@Valid @RequestBody AddAircraftRequest request) {
        AircraftModel model = aircraftModelSearchService
                .spotAircraftInCatalog(request.modelName())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Model does not exist in catalog"));
        Aircraft aircraft = new Aircraft(
                request.registrationNumber(),
                model,
                request.manufacturingDate(),
                request.totalOperationalHours(),
                request.totalFlightHours()
        );
        aircraftLifeCycleUpdaterService.changeAvailability(aircraft,
                AircraftAvailability.valueOf(request.availability()));
        addAircraftUseCase.execute(aircraft);
    }

    @GetMapping("/aircraft/{registrationNumber}")
    public AircraftResponse getAircraft(@PathVariable String registrationNumber) {
        Aircraft aircraft = aircraftSearchService
                .spotAircraftInHangar(registrationNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Aircraft does not exist in hangar"
                        )
                );
        return AircraftResponse.from(aircraft);
    }

    @GetMapping("/aircraft/{registrationNumber}/status")
    public AircraftStatusResponse getAircraftStatus(@PathVariable String registrationNumber) {
        Aircraft aircraft = aircraftSearchService
                .spotAircraftInHangar(registrationNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Aircraft does not exist in hangar"
                        )
                );
        return new AircraftStatusResponse(aircraft.getStatus(),Instant.now().truncatedTo(ChronoUnit.MINUTES));
    }

    @PatchMapping("/aircraft/{registrationNumber}")
    public void changeAircraft(@PathVariable String registrationNumber,@Valid @RequestBody PatchAircraftAvailabilityRequest request) {
        if (!registrationNumber.equals(request.registrationNumber())) {
            throw new DomainException("Non matching info");
        }

        Aircraft aircraft = aircraftSearchService
                .spotAircraftInHangar(request.registrationNumber())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Aircraft does not exist in hangar"
                        )
                );
        aircraftLifeCycleUpdaterService.changeAvailability(aircraft,AircraftAvailability.valueOf(request.availability()));
    }

    @Transactional(readOnly = true)
    @GetMapping("/aircraft")
    public List<AircraftResponse> getAircraftsWithFiltering(@RequestParam(required = false) String model,
            @RequestParam(required = false) String status,@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate manufacturingDate,@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return aircraftSearchService
                .findAircraftsMatchingFilter(model, status, manufacturingDate, pageable)
                .map(AircraftResponse::from).toList();
    }

    @GetMapping("/aircraft/{registrationNumber}/compatible-routes")
    public List<RouteClientDTO> getCompatibleRoutes(@PathVariable String registrationNumber) {
        Aircraft aircraft = aircraftSearchService
                .spotAircraftInHangar(registrationNumber)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Aircraft does not exist in hangar"
                        )
                );
        List<RouteClientDTO> routes = routeClient.getAllRoutes();
        return routeSearchService.findCompatibleRoutes(aircraft, routes);
    }

    /**
     * Gets all aircrafts grouped by their availability
     */
    @GetMapping("/analytics/aircraft-availability")
    public Map<AircraftAvailability, List<String>> getAircraftsAvailability() {
        Map<AircraftAvailability, List<String>> result = new EnumMap<>(AircraftAvailability.class);
        List<Aircraft> aircrafts = aircraftSearchService.findAll();
        for (AircraftAvailability availability : AircraftAvailability.values()) {
            List<String> registrations =aircraftSearchService.findAircraftsByAvailability(aircrafts, availability)
                            .stream().map(Aircraft::getRegistrationNumber)
                            .toList();
            result.put(availability, registrations);
        }
        return result;
    }

    /**
     * Gets the real-time aircraft availability status
     */
    @GetMapping("/analytics/status-summary")
    public Map<AircraftAvailability, Long> getAircraftAvailabilitySummary() {
        return Arrays.stream(AircraftAvailability.values())
                .collect(Collectors.toMap(
                        status -> status,
                        aircraftSearchService::countByAvailability
                ));
    }

    /**
     * Gets the summary of the fuel efficiency metrics per aircraft and per route.
     * Example Value for a real aircraft: 0.177 km/L <=> 5.65 L/km
     */
    @GetMapping("/analytics/fuel-efficiency")
    public Map<String,Double> getFuelEfficiency(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        Map<String, Double> fuelEfficiency = aircraftSearchService.findAll().stream()
                .collect(Collectors.toMap(
                        aircraft -> aircraft.getRegistrationNumber(),
                        aircraft ->  Math.round(aircraft.getModel().getAircraftModelSpecs().getFuelCapacity()
                                / aircraft.getModel().getAircraftModelSpecs().getMaximumRange()* 100.0) / 100.0
                ));




        flightClient.getAllFlights().forEach(flight -> {
            Double fuelCapacity = 0.0;
            Optional<Aircraft> aircraft = aircraftSearchService.spotAircraftInHangar(flight.aircraftRegistration());
            if(aircraft.isPresent()) {fuelCapacity = aircraft.get().getModel().getAircraftModelSpecs().getFuelCapacity();}

            fuelEfficiency.put(
                    "Flight "+ flight.id().toString()+ ":",
                    Math.round(fuelCapacity / flight.range() * 100.0) / 100.0
            );
        });
        return fuelEfficiency;
    }

    /**
     * Gets all Aircrafts and returns them in descending order
     */
    @GetMapping("/analytics/operational-hours")
    public Map<String, Double> getOperationalHours(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "totalOperationalHours"));
        Page<Aircraft> aircraftPage = aircraftSearchService.findPages(pageable);
        return aircraftPage.getContent().stream()
                .collect(Collectors.toMap(
                        Aircraft::getRegistrationNumber,
                        Aircraft::getTotalOperationalHours,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    /**
     * US227 — Fuel efficiency per aircraft: L/km based on model specs.
     */
    @GetMapping("/analytics/fuel-efficiency/aircraft")
    public List<FuelEfficiencyService.AircraftFuelEfficiency> getFuelEfficiencyPerAircraft() {
        return fuelEfficiencyService.getFuelEfficiencyPerAircraft();
    }

    /**
     * US227 — Estimated fuel consumption per route, averaged across aircraft that flew it.
     */
    @GetMapping("/analytics/fuel-efficiency/routes")
    public List<FuelEfficiencyService.RouteFuelEfficiency> getFuelEfficiencyPerRoute() {
        return fuelEfficiencyService.getFuelEfficiencyPerRoute();
    }

    /**
     * Gets all the aircrafts with the specified features
     */
    @GetMapping("/aircraft-features")
    public List<AircraftResponse> getAircraftsWithFeatures(@RequestBody GetFeaturesRequest request) {
        if (request.features() == null || request.features().isEmpty()) {
            throw new IllegalArgumentException("Feature list cannot be empty!");
        }
        return aircraftSearchService.findAircraftWithFeature(request.features())
                .stream()
                .map(AircraftResponse::from)
                .toList();
    }

    /**
     * Request body for POST /hangar/aircraft
     */
    public record AddAircraftRequest(String registrationNumber, String modelName, LocalDate manufacturingDate,
                                     Double totalOperationalHours, Double totalFlightHours, String availability) {}

    /**
     *Request body for PATCH /hangar/aircraft{id}
     */
    public record  PatchAircraftAvailabilityRequest(String registrationNumber,String availability) {}

    /**
     * Request body for GET /hangar/aircraft-features
     */
    public record GetFeaturesRequest(List<String> features){}

    /**
    * Response body representing an aircraft.
    */
    public record AircraftResponse(String registrationNumber, String modelName, AircraftAvailability status,
            LocalDate manufacturingDate, Double totalOperationalHours, Double totalFlightHours,
            Double meanRange, List<String> features) {
        public static AircraftResponse from(Aircraft aircraft) {
            return new AircraftResponse(aircraft.getRegistrationNumber(),
                    aircraft.getModel().getModelName(), aircraft.getStatus(),
                    aircraft.getManufacturingDate(), aircraft.getTotalOperationalHours(),
                    aircraft.getTotalFlightHours(), aircraft.getMeanRange(),
                    aircraft.getFeatures()
            );
        }
    }

    /**
     * Response body representing an aircraft availability and its timestamp
     */
    public record AircraftStatusResponse(AircraftAvailability status, Instant timestamp) {}


    /**
     * Graph value from an aircraft to view its own utilization rate
     */
    public record UtilizationInfo(String registrationNumber,LocalDate period,Double utilizationRate) {}}

