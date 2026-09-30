package com.sidis.routeservice.application;
/*
import com.example.AISafePSOFT_26.Airport.domain.Airport;
import com.example.AISafePSOFT_26.Airport.domain.AirportStatus;
import com.example.AISafePSOFT_26.Airport.infrastructure.AirportRepository;

import com.example.AISafePSOFT_26.Aircraft.domain.Aircraft;
import com.example.AISafePSOFT_26.Aircraft.domain.AircraftAvailability;

import com.example.AISafePSOFT_26.Flight.domain.Flight;
import com.example.AISafePSOFT_26.Flight.infrastructure.FlightRepository;
*/
import com.sidis.routeservice.domain.Route;
import com.sidis.routeservice.domain.RouteHistory;
import com.sidis.routeservice.domain.RouteRequirements;
import com.sidis.routeservice.domain.RouteStatus;
import com.sidis.routeservice.domain.RouteType;
import com.sidis.routeservice.infrastructure.RouteRepository;
import com.sidis.routeservice.UseCase;
import com.sidis.routeservice.exceptions.DomainException;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@UseCase
@Transactional
public class RouteService {
    private final RouteRepository routeRepository;
    //private final FlightRepository flightRepository;
    //private final AirportRepository airportRepository;

    @Autowired
    public RouteService(RouteRepository routeRepository/*, FlightRepository flightRepository,
            AirportRepository airportRepository*/) {
        this.routeRepository = routeRepository;
        //this.flightRepository = flightRepository;
        //this.airportRepository = airportRepository;
    }

    public Optional<Route> getRouteById(Long routeId) {
        return routeRepository.findByRouteId(routeId);
    }

    /*
    public Route createRoute(String routeName, String originIataCode,
            String destinationIataCode, Double estimatedFlightTimeHours,
            RouteType type, RouteRequirements routeRequirements) {
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

        Airport origin = findAirport(originIataCode, "Origin airport does not exist");
        Airport destination = findAirport(destinationIataCode, "Destination airport does not exist");
        if (origin.getStatus() != AirportStatus.OPERATIONAL
                || destination.getStatus() != AirportStatus.OPERATIONAL) {
            throw new DomainException("Airport is not operational");
        }

        Route route = new Route(routeRequirements, RouteStatus.ACTIVE,
                type == null ? RouteType.DIRECT : type,
                new RouteHistory(LocalDate.now(), 0), estimatedFlightTimeHours,
                origin, destination, routeName);
        return routeRepository.save(route);
    }
    */

    public RouteHistory getRouteHistory(Long routeId) {
        return getRouteDetails(routeId).getRouteHistory();
    }

    /*
    public Route updateRoute(Long routeId, String destinationIataCode,
            Double estimatedFlightTimeHours, RouteStatus status) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new DomainException("Route does not exist"));

        if (destinationIataCode != null) {
            validateIataCode(destinationIataCode);
            if (route.getOriginAirport().getIataCode().equals(destinationIataCode)) {
                throw new DomainException("Origin and destination airports must be different");
            }
            Airport destination = findAirport(destinationIataCode,
                    "Destination airport does not exist");
            if (destination.getStatus() != AirportStatus.OPERATIONAL) {
                throw new DomainException("Airport is not operational");
            }
            route.changeDestination(destination);
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
    */

    /*
    public List<Route> getRoutesFromAirport(String airportIataCode) {
        validateIataCode(airportIataCode);
        findAirport(airportIataCode, "Airport does not exist");
        return routeRepository.findByOriginAirport_IataCode(airportIataCode);
    }*/

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

    /*
    public Flight scheduleFlight(Long routeId, Aircraft aircraft,
            LocalDateTime scheduledDeparture, LocalDateTime scheduledArrival) {
        Route route = routeRepository.findByRouteId(routeId)
                .orElseThrow(() -> new DomainException("Route does not exist"));
        validateFlightSchedule(route, aircraft, scheduledDeparture, scheduledArrival);

        Flight flight = new Flight(route, aircraft, scheduledDeparture, scheduledArrival);
        route.getRouteHistory().updateRouteUsage(1);
        routeRepository.save(route);
        return flightRepository.save(flight);
    }

    public List<Flight> getScheduledFlightsForAircraft(String registrationNumber) {
        return flightRepository.findByAircraft_RegistrationNumber(registrationNumber);
    }
    */

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

    /*
    private void validateFlightSchedule(Route route, Aircraft aircraft,
            LocalDateTime scheduledDeparture, LocalDateTime scheduledArrival) {
        if (aircraft == null) {
            throw new DomainException("Aircraft does not exist");
        }
        if (scheduledDeparture == null || scheduledArrival == null
                || !scheduledArrival.isAfter(scheduledDeparture)) {
            throw new DomainException("Scheduled arrival must be after departure");
        }
        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new DomainException("Route is not active");
        }
        if (route.getOriginAirport().getStatus() != AirportStatus.OPERATIONAL
                || route.getDestinationAirport().getStatus() != AirportStatus.OPERATIONAL) {
            throw new DomainException("Airport is not operational");
        }
        if (aircraft.getStatus() != AircraftAvailability.AVAILABLE) {
            throw new DomainException("Aircraft is not available");
        }
        if (aircraftRange(aircraft) < requiredRange(route)) {
            throw new DomainException("Aircraft range does not comply with route requirements");
        }
        boolean aircraftBusy = flightRepository
                .existsByAircraft_RegistrationNumberAndScheduledDepartureLessThanAndScheduledArrivalGreaterThan(
                        aircraft.getRegistrationNumber(), scheduledArrival, scheduledDeparture);
        if (aircraftBusy) {
            throw new DomainException("Aircraft is already scheduled for this period");
        }
    }

    private Double aircraftRange(Aircraft aircraft) {
        if (aircraft.getMeanRange() != null) {
            return aircraft.getMeanRange();
        }
        return aircraft.getModel().getAircraftModelSpecs().getMaximumRange();
    }
    */

    private Double requiredRange(Route route) {
        if (route.getRouteRequirements() != null
                && route.getRouteRequirements().getRequiredRange() != null) {
            return route.getRouteRequirements().getRequiredRange();
        }
        return route.getDistanceKm();
    }

    /*
    private Airport findAirport(String iataCode, String message) {
        if (airportRepository == null) {
            throw new DomainException(message);
        }
        return airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException(message));
    }*/

    private void validateIataCode(String iataCode) {
        if (iataCode == null || !iataCode.matches("[A-Z]{3}")) {
            throw new DomainException("IATA code must have 3 uppercase letters");
        }
    }
}
