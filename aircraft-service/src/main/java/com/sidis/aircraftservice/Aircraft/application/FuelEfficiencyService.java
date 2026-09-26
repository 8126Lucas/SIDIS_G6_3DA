/*

package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.Flight.domain.Flight;
import com.sidis.aircraftservice.Flight.infrastructure.FlightRepository;
import com.sidis.aircraftservice.Route.domain.Route;
import com.sidis.aircraftservice.UseCase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UseCase
public class FuelEfficiencyService {

    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;

    public FuelEfficiencyService(AircraftRepository aircraftRepository, FlightRepository flightRepository) {
        this.aircraftRepository = aircraftRepository;
        this.flightRepository = flightRepository;
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
        Map<Route, List<Flight>> byRoute = flightRepository.findAll().stream()
                .filter(f -> f.getAircraft() != null)
                .collect(Collectors.groupingBy(Flight::getRoute));

        return byRoute.entrySet().stream()
                .map(e -> {
                    Route route = e.getKey();
                    List<Flight> flights = e.getValue();

                    double avgRate = flights.stream()
                            .mapToDouble(f -> {
                                AircraftSpecs specs = f.getAircraft().getModel().getAircraftModelSpecs();
                                if (specs.getFuelCapacity() == null || specs.getMaximumRange() == null
                                        || specs.getMaximumRange() == 0) {
                                    return 0.0;
                                }
                                return specs.getFuelCapacity() / specs.getMaximumRange();
                            })
                            .average()
                            .orElse(0.0);

                    double avgRateRounded = Math.round(avgRate * 100.0) / 100.0;
                    double estimatedFuel = Math.round(avgRateRounded * route.getDistanceKm() * 100.0) / 100.0;

                    return new RouteFuelEfficiency(
                            route.getRouteId(),
                            route.getRouteName(),
                            route.getOriginAirport().getIataCode(),
                            route.getDestinationAirport().getIataCode(),
                            route.getDistanceKm(),
                            avgRateRounded,
                            estimatedFuel
                    );
                })
                .sorted(Comparator.comparing(RouteFuelEfficiency::routeId))
                .toList();
    }
}
*/