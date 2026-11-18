package com.sidis.routeservice.Route.application;

import com.sidis.routeservice.Route.domain.RouteReadModel;
import com.sidis.routeservice.Route.domain.RouteStatus;
import com.sidis.routeservice.Route.infrastructure.RouteReadRepository;
import com.sidis.routeservice.Route.infrastructure.client.aircraft.AircraftClientDTO;
import com.sidis.routeservice.Route.infrastructure.client.aircraft.AircraftModelDTO;
import com.sidis.routeservice.exceptions.DomainException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class RouteQueryService {

    private final RouteReadRepository routeReadRepository;

    public RouteQueryService(RouteReadRepository routeReadRepository) {
        this.routeReadRepository = routeReadRepository;
    }

    public List<RouteReadModel> findAll() {
        return StreamSupport.stream(routeReadRepository.findAll().spliterator(), false)
                .filter(Objects::nonNull)
                .toList();
    }

    public Optional<RouteReadModel> getRouteById(Long routeId) {
        return routeReadRepository.findById(routeId);
    }

    public RouteReadModel getRouteDetails(Long routeId) {
        return getRouteById(routeId)
                .orElseThrow(() -> new DomainException("Route does not exist"));
    }

    public RouteReadModel.HistoryData getRouteHistory(Long routeId) {
        return getRouteDetails(routeId).routeHistory();
    }

    public List<RouteReadModel> getRoutesFromAirport(String airportIataCode) {
        validateIataCode(airportIataCode);
        return routeReadRepository.findByOriginAirport_IataCode(airportIataCode)
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    public List<RouteReadModel> searchRoutes(String originIataCode, String destinationIataCode) {
        if (originIataCode != null) {
            validateIataCode(originIataCode);
        }
        if (destinationIataCode != null) {
            validateIataCode(destinationIataCode);
        }

        Iterable<RouteReadModel> candidates;
        if (originIataCode != null) {
            candidates = routeReadRepository.findByOriginAirport_IataCode(originIataCode);
        } else if (destinationIataCode != null) {
            candidates = routeReadRepository.findByDestinationAirport_IataCode(destinationIataCode);
        } else {
            candidates = routeReadRepository.findAll();
        }

        return StreamSupport.stream(candidates.spliterator(), false)
                .filter(Objects::nonNull)
                .filter(route -> originIataCode == null
                        || (route.originAirport() != null
                        && originIataCode.equals(route.originAirport().iataCode())))
                .filter(route -> destinationIataCode == null
                        || (route.destinationAirport() != null
                        && destinationIataCode.equals(route.destinationAirport().iataCode())))
                .toList();
    }

    public List<RouteReadModel> listActiveRoutes(String sortBy) {
        Comparator<RouteReadModel> order = "distance".equalsIgnoreCase(sortBy)
                ? Comparator.comparing(RouteReadModel::distanceKm,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                : Comparator.comparing(
                        route -> route.routeHistory() == null ? null : route.routeHistory().routeUsage(),
                        Comparator.nullsFirst(Comparator.reverseOrder()));

        return routeReadRepository.findByStatus(RouteStatus.ACTIVE).stream()
                .filter(Objects::nonNull)
                .sorted(order)
                .toList();
    }

    public Double calculateTotalRouteDistance() {
        return findAll().stream()
                .mapToDouble(route -> route.distanceKm() == null ? 0.0 : route.distanceKm())
                .sum();
    }

    public List<RouteReadModel> searchAlternativeRoutes(String originIataCode, String destinationIataCode) {
        if (originIataCode == null || destinationIataCode == null) {
            return List.of();
        }

        return routeReadRepository.findByOriginAirport_IataCode(originIataCode).stream()
                .filter(Objects::nonNull)
                .filter(route -> route.status() == RouteStatus.ACTIVE)
                .filter(route -> route.destinationAirport() != null
                        && destinationIataCode.equals(route.destinationAirport().iataCode()))
                .toList();
    }

    public List<RouteReadModel> findCompatibleRoutes(AircraftClientDTO aircraft, AircraftModelDTO modelDTO) {
        int capacity;
        if (aircraft.seatingPackDTO() != null
                && aircraft.seatingPackDTO().seatingCapacity() != null) {
            capacity = aircraft.seatingPackDTO().seatingCapacity();
        } else {
            capacity = modelDTO.aircraftModelSpecsDTO().standardSeatingCapacity();
        }

        double range;
        if (aircraft.meanRange() != null && aircraft.meanRange() > 0) {
            range = aircraft.meanRange();
        } else {
            range = modelDTO.aircraftModelSpecsDTO().maximumRangeKm();
        }

        return findAll().stream()
                .filter(route -> route.routeRequirements() != null
                        && route.routeRequirements().requiredCapacity() != null
                        && route.routeRequirements().requiredRange() != null)
                .filter(route -> route.routeRequirements().requiredCapacity() <= capacity
                        && route.routeRequirements().requiredRange() <= range)
                .toList();
    }

    private void validateIataCode(String iataCode) {
        if (iataCode == null || !iataCode.matches("[A-Z]{3}")) {
            throw new DomainException("IATA code must have 3 uppercase letters");
        }
    }
}
