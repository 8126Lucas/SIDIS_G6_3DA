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
import com.sidis.routeservice.Route.infrastructure.client.airport.AirportStatus;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Objects;

import static com.sidis.routeservice.Route.infrastructure.client.airport.AirportClientDTO.fromDTOtoAirportReference;

@UseCase
@Transactional
public class RouteService {
    private final RouteRepository routeRepository;
    private final AirportRestClientService airportRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public RouteService(RouteRepository routeRepository,
                        AirportRestClientService airportRepository,
                        ApplicationEventPublisher eventPublisher) {
        this.routeRepository = routeRepository;
        this.airportRepository = airportRepository;
        this.eventPublisher = eventPublisher;
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
        return saveAndPublish(route);
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

        if (history != null) {
            route.updateRouteHistory(history);
        }
        return saveAndPublish(route);
    }

    private Route saveAndPublish(Route route) {
        Route saved = routeRepository.saveAndFlush(route);

        eventPublisher.publishEvent(
                new RouteChangedEvent(
                        RouteReadModelMapper.from(saved)
                )
        );

        return saved;
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
