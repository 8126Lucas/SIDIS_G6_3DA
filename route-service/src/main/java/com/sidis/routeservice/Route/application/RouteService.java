package com.sidis.routeservice.Route.application;

import com.sidis.routeservice.Route.domain.Route;
import com.sidis.routeservice.Route.domain.RouteHistory;
import com.sidis.routeservice.Route.domain.RouteRequirements;
import com.sidis.routeservice.Route.domain.RouteStatus;
import com.sidis.routeservice.Route.domain.RouteType;
import com.sidis.routeservice.Route.infrastructure.RouteRepository;
import com.sidis.routeservice.UseCase;
import com.sidis.routeservice.exceptions.DomainException;

import com.sidis.routeservice.Route.infrastructure.client.airport.AirportClientDTO;
import com.sidis.routeservice.Route.infrastructure.client.AirportRestClientService;
import com.sidis.routeservice.Route.infrastructure.client.FlightRestClientService;
import com.sidis.routeservice.Route.infrastructure.client.airport.AirportStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.sidis.routeservice.Route.infrastructure.client.airport.AirportClientDTO.fromDTOtoAirportReference;

@UseCase
@Transactional
public class RouteService {
    private final RouteRepository routeRepository;
    private final FlightRestClientService flightRepository;
    private final AirportRestClientService airportRepository;

    @Autowired
    public RouteService(RouteRepository routeRepository, FlightRestClientService flightRepository,AirportRestClientService airportRepository) {
        this.routeRepository = routeRepository;
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
    }

    public RouteService(RouteRepository routeRepository, FlightRestClientService flightRepository) {
        this(routeRepository, flightRepository, null);
    }

    public RouteService(RouteRepository routeRepository, AirportRestClientService airportRepository) {
        this(routeRepository, null, airportRepository);
    }

    public Optional<Route> getRouteById(Long routeId) {
        return routeRepository.findByRouteId(routeId);
    }

    public Route createRoute(String routeName, String originIataCode, String destinationIataCode, Double estimatedFlightTimeHours, RouteType type, RouteRequirements routeRequirements) {
        validateIataCode(originIataCode);
        validateIataCode(destinationIataCode);
        if (originIataCode.equals(destinationIataCode)) {
            throw new DomainException("Origin and destination airports must be different");
        }
        if (estimatedFlightTimeHours == null || estimatedFlightTimeHours <= 0) {
            throw new DomainException("Estimated flight time must be positive");
        }
        if (routeRepository.findByRouteName(routeName).isPresent()) {
            throw new DomainException("Route already exists");
        }
        AirportClientDTO origin = findAirport(originIataCode, "Origin airport does not exist");
        AirportClientDTO destination = findAirport(destinationIataCode, "Destination airport does not exist");
        if (!Objects.equals(origin.status(), AirportStatus.OPERATIONAL.toString())
                || !Objects.equals(destination.status(), AirportStatus.OPERATIONAL.toString())) {
            throw new DomainException("Airport is not operational");
        }
        Route route = new Route(routeRequirements, RouteStatus.ACTIVE,
                type == null ? RouteType.DIRECT : type,
                new RouteHistory(LocalDate.now(), 0), estimatedFlightTimeHours,
                fromDTOtoAirportReference(origin), fromDTOtoAirportReference(destination), routeName);
        return routeRepository.save(route);
    }

    public RouteHistory getRouteHistory(Long routeId) {
        return getRouteDetails(routeId).getRouteHistory();
    }

    public Route updateRoute(Long routeId, String destinationIataCode,Double estimatedFlightTimeHours, RouteStatus status, RouteHistory history) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new DomainException("Route does not exist"));
        if (destinationIataCode != null) {
            validateIataCode(destinationIataCode);
            if (route.getOriginAirport().getIataCode().equals(destinationIataCode)) {
                throw new DomainException("Origin and destination airports must be different");
            }
            AirportClientDTO destination = findAirport(destinationIataCode,"Destination airport does not exist");
            if (!Objects.equals(destination.status(), AirportStatus.OPERATIONAL.toString())) {
                throw new DomainException("Airport is not operational");
            }
            route.changeDestination(fromDTOtoAirportReference(destination));
        }

        if (estimatedFlightTimeHours != null) {
            route.updateEstimatedFlightTime(estimatedFlightTimeHours);
        }

        if (status != null) {
            if (status == RouteStatus.ACTIVE) {
                route.activateRoute();
            } else if (status == RouteStatus.INACTIVE) {
                route.suspendRoute();
            } else if (status == RouteStatus.ARCHIVED) {
                route.retireRoute();
                route.getRouteHistory().endRoute(LocalDate.now());
            }
        }
        return routeRepository.save(route);
    }

    public List<Route> getRoutesFromAirport(String airportIataCode) {
        validateIataCode(airportIataCode);
        return routeRepository.findByOriginAirport_IataCode(airportIataCode);
    }

    public Route getRouteDetails(Long routeId) {
        return routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new DomainException("Route does not exist"));
    }

    public List<Route> searchRoutes(String originIataCode, String destinationIataCode) {
        if (originIataCode != null) {
            validateIataCode(originIataCode);
        }
        if (destinationIataCode != null) {
            validateIataCode(destinationIataCode);
        }

        Specification<Route> spec = (root, query, cb) -> cb.conjunction();
        if (originIataCode != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("originAirport").get("iataCode"), originIataCode));
        }
        if (destinationIataCode != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("destinationAirport").get("iataCode"), destinationIataCode));
        }
        return routeRepository.findAll(spec);
    }

    public List<Route> listActiveRoutes(String sortBy) {
        Sort sort = "distance".equalsIgnoreCase(sortBy)
                ? Sort.by(Sort.Direction.ASC, "distanceKm")
                : Sort.by(Sort.Direction.DESC, "routeHistory.routeUsage");
        return routeRepository.findByStatus(RouteStatus.ACTIVE, sort);
    }

    public Double calculateTotalRouteDistance() {
        return routeRepository.findAll()
                .stream()
                .mapToDouble(route -> route.getDistanceKm() == null ? 0.0 : route.getDistanceKm())
                .sum();
    }

    public List<Route> searchAlternativeRoutes(String originIataCode, String destinationIataCode) {
        Specification<Route> spec = (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), RouteStatus.ACTIVE),
                cb.equal(root.get("originAirport").get("iataCode"), originIataCode),
                cb.equal(root.get("destinationAirport").get("iataCode"), destinationIataCode)
        );
        return routeRepository.findAll(spec);
    }

    private Double requiredRange(Route route) {
        if (route.getRouteRequirements() != null
                && route.getRouteRequirements().getRequiredRange() != null) {
            return route.getRouteRequirements().getRequiredRange();
        }
        return route.getDistanceKm();
    }

    private AirportClientDTO findAirport(String iataCode, String message) {
        if (airportRepository == null) {
            throw new DomainException(message);
        }
        return airportRepository.findAirportByIataCode(iataCode);
    }

    private void validateIataCode(String iataCode) {
        if (iataCode == null || !iataCode.matches("[A-Z]{3}")) {
            throw new DomainException("IATA code must have 3 uppercase letters");
        }
    }

}
