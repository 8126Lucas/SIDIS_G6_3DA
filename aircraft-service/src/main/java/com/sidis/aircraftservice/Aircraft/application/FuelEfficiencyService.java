package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftCommandRepository;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecsRedis;
import com.sidis.aircraftservice.infrastructure.client.FlightClientDTO;
import com.sidis.aircraftservice.infrastructure.client.FlightRestClientService;
import com.sidis.aircraftservice.infrastructure.client.RouteClientDTO;
import com.sidis.aircraftservice.infrastructure.client.RouteRestClientService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.UseCase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
public class FuelEfficiencyService {
    private final AircraftCommandRepository aircraftCommandRepository;
    private final AircraftQueryRepository aircraftQueryRepository;
    private final FlightRestClientService flightClient;
    private final RouteRestClientService routeClient;
    private final AircraftModelSearchService aircraftModelSearchService;

    public FuelEfficiencyService(AircraftCommandRepository aircraftCommandRepository,
                                 AircraftQueryRepository aircraftQueryRepository,
                                 FlightRestClientService flightClient,
                                 RouteRestClientService routeClient,
                                 AircraftModelSearchService aircraftModelSearchService) {
        this.aircraftCommandRepository = aircraftCommandRepository;
        this.aircraftQueryRepository = aircraftQueryRepository;
        this.flightClient = flightClient;
        this.routeClient = routeClient;
        this.aircraftModelSearchService = aircraftModelSearchService;
    }

    public record AircraftFuelEfficiency(
            String registrationNumber,
            String modelName,
            Double fuelCapacityLiters,
            Double maxRangeKm,
            Double fuelConsumptionLitPerKm
    ) {}

    public record RouteFuelEfficiency(
            Long routeId,
            String routeName,
            String originIata,
            String destinationIata,
            Double distanceKm,
            Double avgFuelConsumptionLitPerKm,
            Double estimatedFuelLiters
    ) {}

    public List<AircraftFuelEfficiency> getFuelEfficiencyPerAircraft() {
        List<AircraftRedis> aircrafts = new ArrayList<>();
        aircraftQueryRepository.findAll().forEach(aircrafts::add);

        return aircrafts.stream()
                .map(a -> {
                    AircraftModelRedis model = aircraftModelSearchService.spotAircraftInCatalog(a.getModelName()).orElse(null);
                    double rate = 0.0;
                    Double capacity = 0.0;
                    Double maxRange = 0.0;

                    if (model != null && model.getAircraftModelSpecs() != null) {
                        AircraftSpecsRedis specs = model.getAircraftModelSpecs();
                        capacity = specs.getFuelCapacity();
                        maxRange = specs.getMaximumRange();
                        if (capacity != null && maxRange != null && maxRange > 0) {
                            rate = Math.round(capacity / maxRange * 100.0) / 100.0;
                        }
                    }

                    return new AircraftFuelEfficiency(
                            a.getRegistrationNumber(),
                            a.getModelName(),
                            capacity,
                            maxRange,
                            rate
                    );
                })
                .sorted(Comparator.comparing(AircraftFuelEfficiency::fuelConsumptionLitPerKm))
                .toList();
    }

    public List<RouteFuelEfficiency> getFuelEfficiencyPerRoute() {
        List<FlightClientDTO> allFlights = flightClient.getAllFlights();

        Map<Long, List<FlightClientDTO>> flightsByRouteId = allFlights.stream()
                .filter(f -> f.aircraftRegistration() != null)
                .collect(Collectors.groupingBy(FlightClientDTO::routeId));

        return flightsByRouteId.entrySet().stream()
                .map(entry -> {
                    Long routeId = entry.getKey();
                    List<FlightClientDTO> flights = entry.getValue();

                    RouteClientDTO route = routeClient.getRouteById(routeId);

                    double avgRate = flights.stream()
                            .mapToDouble(f -> {
                                AircraftRedis aircraft = aircraftQueryRepository.findById(f.aircraftRegistration()).orElse(null);

                                if (aircraft == null) {
                                    return 0.0;
                                }

                                AircraftModelRedis model = aircraftModelSearchService.spotAircraftInCatalog(aircraft.getModelName()).orElse(null);
                                AircraftSpecsRedis specs = model.getAircraftModelSpecs();
                                if (specs.getFuelCapacity() == null || specs.getMaximumRange() == null
                                        || specs.getMaximumRange() == 0) {
                                    return 0.0;
                                }
                                return specs.getFuelCapacity() / specs.getMaximumRange();
                            })
                            .average()
                            .orElse(0.0);

                    double avgRateRounded = Math.round(avgRate * 100.0) / 100.0;
                    double estimatedFuel = Math.round(avgRateRounded * route.distanceKm() * 100.0) / 100.0;

                    return new RouteFuelEfficiency(
                            route.routeId(),
                            route.routeName(),
                            route.originAirportIata(),
                            route.destinationAirportIata(),
                            route.distanceKm(),
                            avgRateRounded,
                            estimatedFuel
                    );
                })
                .sorted(Comparator.comparing(RouteFuelEfficiency::routeId))
                .toList();
    }
}