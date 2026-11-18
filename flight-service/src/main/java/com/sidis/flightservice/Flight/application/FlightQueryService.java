package com.sidis.flightservice.Flight.application;

import com.sidis.flightservice.Flight.domain.FlightReadModel;
import com.sidis.flightservice.Flight.infrastructure.FlightReadRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class FlightQueryService {

    private final FlightReadRepository flightReadRepository;

    public FlightQueryService(FlightReadRepository flightReadRepository) {
        this.flightReadRepository = flightReadRepository;
    }

    public List<FlightReadModel> findAll() {
        return StreamSupport.stream(flightReadRepository.findAll().spliterator(), false)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<FlightReadModel> getScheduledFlightsForAircraft(String registrationNumber) {
        return flightReadRepository.findByAircraftRegistrationNumber(registrationNumber);
    }

    public List<FlightReadModel> findByAircraftRegistrationNumberAndScheduledDepartureBetween(
            String registrationNumber, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return List.of();
        }

        return getScheduledFlightsForAircraft(registrationNumber).stream()
                .filter(flight -> flight.scheduledDeparture() != null)
                .filter(flight -> !flight.scheduledDeparture().isBefore(start)
                        && !flight.scheduledDeparture().isAfter(end))
                .toList();
    }

    public List<Map.Entry<String, Long>> findMostAssignedAircraftToFly() {
        return findAll().stream()
                .filter(flight -> flight.aircraftRegistrationNumber() != null)
                .collect(Collectors.groupingBy(
                        FlightReadModel::aircraftRegistrationNumber,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();
    }

    public List<RouteUtilizationReport> getRouteUtilizationReport() {
        return findAll().stream()
                .filter(flight -> flight.route() != null && flight.route().routeId() != null)
                .collect(Collectors.groupingBy(flight -> flight.route().routeId()))
                .values().stream()
                .map(flights -> {
                    var route = flights.getFirst().route();
                    return new RouteUtilizationReport(
                            route.routeId(), route.routeName(),
                            route.originIata(), route.destinationIata(),
                            (long) flights.size()
                    );
                })
                .sorted(Comparator
                        .comparing(RouteUtilizationReport::flightCount).reversed()
                        .thenComparing(RouteUtilizationReport::routeName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public record RouteUtilizationReport(
            Long routeId,
            String routeName,
            String originIata,
            String destinationIata,
            Long flightCount
    ) {}
}
