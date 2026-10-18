package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.Aircraft.infrastructure.client.FlightClientDTO;
import com.sidis.aircraftservice.Aircraft.infrastructure.client.FlightRestClientService;
import com.sidis.aircraftservice.Aircraft.infrastructure.client.RouteClientDTO;
import com.sidis.aircraftservice.Aircraft.infrastructure.client.RouteRestClientService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
//import com.sidis.aircraftservice.Flight.domain.Flight;
//import com.sidis.aircraftservice.Flight.infrastructure.FlightRepository;
//import com.sidis.aircraftservice.Route.domain.Route;
import com.sidis.aircraftservice.UseCase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
public class FuelEfficiencyService {

    private final AircraftRepository aircraftRepository;
    private final FlightRestClientService flightClient;
    private final RouteRestClientService routeClient;

    public FuelEfficiencyService(AircraftRepository aircraftRepository,
                                 FlightRestClientService flightClient,
                                 RouteRestClientService routeClient) {

        this.aircraftRepository = aircraftRepository;
        this.flightClient = flightClient;
        this.routeClient = routeClient;
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
        return aircraftRepository.findAll().stream()
                .map(a -> {
                    AircraftSpecs specs = a.getModel().getAircraftModelSpecs();
                    double rate = 0.0;
                    if (specs.getFuelCapacity() != null && specs.getMaximumRange() != null
                            && specs.getMaximumRange() > 0) {
                        rate = Math.round(specs.getFuelCapacity() / specs.getMaximumRange() * 100.0) / 100.0;
                    }
                    return new AircraftFuelEfficiency(
                            a.getRegistrationNumber(),
                            a.getModel().getModelName(),
                            specs.getFuelCapacity(),
                            specs.getMaximumRange(),
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
                                Aircraft aircraft = aircraftRepository.findById(f.aircraftRegistration()).orElse(null);

                                if (aircraft == null) {
                                    return 0.0;
                                }

                                AircraftSpecs specs = aircraft.getModel().getAircraftModelSpecs();
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