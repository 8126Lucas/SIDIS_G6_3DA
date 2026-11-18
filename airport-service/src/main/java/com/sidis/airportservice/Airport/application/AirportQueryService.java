package com.sidis.airportservice.Airport.application;

import com.sidis.airportservice.Airport.domain.AirportReadModel;
import com.sidis.airportservice.Airport.infrastructure.AirportReadRepository;
import com.sidis.airportservice.Airport.infrastructure.client.RouteRestClientService;
import com.sidis.airportservice.Airport.infrastructure.client.route.RouteResponse;
import com.sidis.airportservice.exceptions.DomainException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.HashMap;
@Service
public class AirportQueryService {

    private final AirportReadRepository airportReadRepository;
    private final RouteRestClientService routeClient;

    public AirportQueryService(
            AirportReadRepository airportReadRepository,
            RouteRestClientService routeClient
    ) {
        this.airportReadRepository = airportReadRepository;
        this.routeClient = routeClient;
    }

    public Optional<AirportReadModel> getAirportByIata(String iataCode) {
        return airportReadRepository.findById(iataCode);
    }

    public List<AirportReadModel> searchAirports(
            String city,
            String country,
            String name
    ) {
        Iterable<AirportReadModel> candidates;

        if (city != null) {
            candidates = airportReadRepository.findByLocation_City(city);
        } else if (country != null) {
            candidates = airportReadRepository.findByLocation_Country(country);
        } else {
            candidates = airportReadRepository.findAll();
        }

        return StreamSupport.stream(candidates.spliterator(), false)
                .filter(airport -> city == null
                        || (airport.location() != null
                        && city.equals(airport.location().city())))
                .filter(airport -> country == null
                        || (airport.location() != null
                        && country.equals(airport.location().country())))
                .filter(airport -> name == null
                        || name.equals(airport.name()))
                .toList();
    }
    public Map<String, List<AirportReadModel>> groupAirportsBy(String criteria) {
        Function<AirportReadModel, String> grouping = switch (criteria) {
            case "country" -> airport -> airport.location().country();
            case "city" -> airport -> airport.location().city();
            case "region" -> airport -> airport.location().region();
            default -> throw new DomainException(
                    "Unknown grouping criteria: " + criteria
            );
        };

        return StreamSupport.stream(
                        airportReadRepository.findAll().spliterator(),
                        false
                )
                .collect(Collectors.groupingBy(grouping));
    }
    public record AirportRouteStatistic(
            AirportReadModel airport,
            long numberOfRoutes
    ) {}

    public List<AirportRouteStatistic> busiestAirports() {
        Map<String, Long> counts = new HashMap<>();

        for (RouteResponse route : routeClient.getRoutes()) {
            counts.merge(route.originAirportIata(), 1L, Long::sum);
            counts.merge(route.destinationAirportIata(), 1L, Long::sum);
        }

        return StreamSupport.stream(
                        airportReadRepository.findAll().spliterator(),
                        false
                )
                .map(airport -> new AirportRouteStatistic(
                        airport,
                        counts.getOrDefault(airport.iataCode(), 0L)
                ))
                .filter(stat -> stat.numberOfRoutes() > 0)
                .sorted(
                        Comparator.comparingLong(
                                AirportRouteStatistic::numberOfRoutes
                        ).reversed()
                )
                .toList();
    }
    public List<RouteResponse> findRoutesByAirport(String iataCode) {
        airportReadRepository.findById(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));

        return routeClient.getRoutes().stream()
                .filter(route ->
                        iataCode.equals(route.originAirportIata())
                                || iataCode.equals(route.destinationAirportIata())
                )
                .toList();
    }
}