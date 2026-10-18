package com.sidis.flightservice.Flight.application;

import com.sidis.flightservice.Flight.domain.Flight;
import com.sidis.flightservice.Flight.domain.RouteReference;
import com.sidis.flightservice.Flight.infrastructure.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightUtilizationReportService {
    private final FlightRepository flightRepository;

    public FlightUtilizationReportService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Transactional(readOnly = true)
    public List<RouteUtilizationReport> getRouteUtilizationReport() {
        return flightRepository.findAll()
                .stream()
                .filter(flight -> flight.getRoute() != null)
                .collect(Collectors.groupingBy(Flight::getRoute, Collectors.counting()))
                .entrySet()
                .stream()
                .map(entry -> toReport(entry.getKey(), entry.getValue()))
                .sorted(Comparator
                        .comparing(RouteUtilizationReport::flightCount).reversed()
                        .thenComparing(RouteUtilizationReport::routeName))
                .toList();
    }


    private RouteUtilizationReport toReport(RouteReference route, Long flightCount) {
        return new RouteUtilizationReport(route.getRouteId(), route.getRouteName(),
                route.getOriginIata(),route.getDestinationIata(), flightCount);
    }

    public record RouteUtilizationReport(Long routeId, String routeName,
            String originIata, String destinationIata, Long flightCount) {
    }
}
