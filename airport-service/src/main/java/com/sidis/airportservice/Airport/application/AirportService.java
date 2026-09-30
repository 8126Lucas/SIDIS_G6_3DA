package com.sidis.airportservice.Airport.application;

import com.sidis.airportservice.Airport.domain.Airport;
import com.sidis.airportservice.Airport.domain.AirportLocation;
import com.sidis.airportservice.Airport.domain.AirportStatus;
import com.sidis.airportservice.Airport.domain.Certification;
import com.sidis.airportservice.Airport.domain.Contact;
import com.sidis.airportservice.Airport.domain.Facilities;
import com.sidis.airportservice.Airport.domain.Runway;
import com.sidis.airportservice.Airport.infrastructure.AirportRepository;
//import com.sidis.airportservice.Route.domain.Route;
//import com.sidis.airportservice.Route.infrastructure.RouteRepository;
import com.sidis.airportservice.UseCase;
import com.sidis.airportservice.exceptions.DomainException;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@UseCase
@Transactional
public class AirportService {
    private final AirportRepository airportRepository;
    //private final RouteRepository routeRepository;

    public AirportService(AirportRepository airportRepository/*, RouteRepository routeRepository*/) {
        this.airportRepository = airportRepository;
        //this.routeRepository = routeRepository;
    }

    public record AirportRouteStatistic(Airport airport, long numberOfRoutes) {
    }

    public void execute(Airport airport) {
        validateIataCode(airport.getIataCode());
        if (airportRepository.findByIataCode(airport.getIataCode()).isPresent()) {
            throw new DomainException("Airport already exists");
        }
        airportRepository.save(airport);
    }

    public void execute(String iataCode, String airportType, String name,
                        AirportStatus status, AirportLocation airportLocation,
                        Facilities facilities, Double routeDistance,
                        List<Runway> airportRunways, List<Certification> certifications,
                        List<Contact> contacts, List<String> airportPhotos) {
        execute(new Airport(iataCode, airportType, name, status, airportLocation,
                facilities, routeDistance, airportRunways, certifications,
                contacts, airportPhotos));
    }

    public Optional<Airport> getAirportByIata(String iataCode) {
        return airportRepository.findByIataCode(iataCode);
    }

    public List<Airport> searchAirports(String city, String country, String name) {
        Specification<Airport> spec = (root, query, cb) -> cb.conjunction();

        if (city != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("airportLocation").get("city"), city));
        }

        if (country != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("airportLocation").get("country"), country));
        }

        if (name != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("name"), name));
        }

        return airportRepository.findAll(spec);
    }

    public Certification addCertification(String iataCode, String name,
                                          String category, LocalDate startsAt, LocalDate expiresAt) {
        Airport airport = airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));

        Certification certification =
                new Certification(name, category, startsAt, expiresAt);
        certification.setAirport(airport);
        airport.addCertification(certification);
        airportRepository.save(airport);
        return certification;
    }

    public Airport updateStatus(String iataCode, AirportStatus status) {
        Airport airport = airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));
        airport.changeStatus(status);
        return airportRepository.save(airport);
    }

    public Airport updateDetails(String iataCode, String name, String operationalHours,
                                 List<Contact> contacts) {
        Airport airport = airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));
        airport.updateDetails(name, operationalHours, contacts);
        return airportRepository.save(airport);
    }
    /*
    TODO: needs to be in the route service
    public List<Route> findRoutesByAirport(String iataCode) {
        airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));
        return routeRepository.findByOriginAirport_IataCodeOrDestinationAirport_IataCode(
                iataCode, iataCode);
    }

    public List<AirportRouteStatistic> busiestAirports() {
        List<Route> routes = routeRepository.findAll();
        Map<Airport, Long> counts = new HashMap<>();
        for (Route route : routes) {
            counts.merge(route.getOriginAirport(), 1L, Long::sum);
            counts.merge(route.getDestinationAirport(), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .map(e -> new AirportRouteStatistic(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(AirportRouteStatistic::numberOfRoutes).reversed())
                .collect(Collectors.toList());
    }*/

    public Map<String, List<Airport>> groupAirportsBy(String criteria) {
        List<Airport> airports = airportRepository.findAll();
        return airports.stream().collect(Collectors.groupingBy(airport -> switch (criteria) {
            case "country" -> airport.getAirportLocation().getCountry();
            case "city" -> airport.getAirportLocation().getCity();
            default -> throw new DomainException("Unknown grouping criteria: " + criteria);
        }));
    }

    private void validateIataCode(String iataCode) {
        if (iataCode == null || !iataCode.matches("[A-Z]{3}")) {
            throw new DomainException("IATA code must have 3 uppercase letters");
        }
    }
}