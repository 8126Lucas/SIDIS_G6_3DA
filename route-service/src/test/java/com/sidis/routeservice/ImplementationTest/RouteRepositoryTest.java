
package com.sidis.routeservice.ImplementationTest;

import com.sidis.routeservice.Route.domain.*;
import com.sidis.routeservice.Route.infrastructure.RouteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("unit")
class RouteRepositoryTest {
    @Autowired
    private RouteRepository routeRepository;

    private Route activeRoute1;
    private Route inactiveRoute;
    private Route activeRoute2;

    @BeforeEach
    void setUp() {
        AirportReference lisbon = createAirport(
                "LIS", "Lisboa", -9.1393, 38.7223,
                "Lisbon", "Europe/Lisbon", "Portugal"
        );

        AirportReference porto = createAirport(
                "OPO", "Porto", -8.6291, 41.1579,
                "Porto", "Europe/Lisbon", "Portugal"
        );

        AirportReference faro = createAirport(
                "FAO", "Faro", -7.9659, 37.0194,
                "Algarve", "Europe/Lisbon", "Portugal"
        );

        AirportReference madrid = createAirport(
                "MAD", "Madrid", -3.5676, 40.4983,
                "Madrid", "Europe/Madrid", "Spain"
        );

        activeRoute1 = createRoute(
                "Lisbon-Porto", RouteStatus.ACTIVE,
                RouteType.DIRECT, lisbon, porto
        );

        inactiveRoute = createRoute(
                "Lisbon-Faro", RouteStatus.INACTIVE,
                RouteType.SCALED, lisbon, faro
        );

        activeRoute2 = createRoute(
                "Porto-Madrid", RouteStatus.ACTIVE,
                RouteType.SCALED, porto, madrid
        );

        routeRepository.saveAll(
                List.of(activeRoute1, inactiveRoute, activeRoute2)
        );
    }

    private AirportReference createAirport(String iataCode, String city, Double longitude, Double latitude, String region, String timezone, String country) {
        AirportLocation location = new AirportLocation(city, longitude, latitude,
                region, timezone, country);
        return new AirportReference(iataCode, location);
    }

    private Route createRoute(String routeName, RouteStatus status, RouteType type, AirportReference origin, AirportReference destination) {
        RouteRequirements requirements = new RouteRequirements(500.0, 180);
        RouteHistory history = new RouteHistory(LocalDate.of(2020, 1, 1), 25);
        return new Route(requirements, status, type,
                history, 1.5, 300.0,
                origin, destination, routeName);
    }

    @Test
    void shouldSaveRoute() {
        Route route = createRoute("Test-Route", RouteStatus.ACTIVE, RouteType.DIRECT,
                activeRoute1.getOriginAirport(), activeRoute1.getDestinationAirport());
        Route savedRoute = routeRepository.save(route);
        assertNotNull(savedRoute.getRouteId());
        assertEquals("Test-Route", savedRoute.getRouteName());
    }

    @Test
    void shouldFindRouteByRouteId() {
        Long routeId = activeRoute1.getRouteId();
        Optional<Route> result = routeRepository.findByRouteId(routeId);
        assertTrue(result.isPresent());
        assertEquals("Lisbon-Porto", result.get().getRouteName());
    }

    @Test
    void shouldReturnEmptyWhenRouteIdDoesNotExist() {
        Optional<Route> result = routeRepository.findByRouteId(999999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindRouteByRouteName() {
        Optional<Route> result = routeRepository.findByRouteName("Lisbon-Porto");
        assertTrue(result.isPresent());
        assertEquals("Lisbon-Porto", result.get().getRouteName());
    }

    @Test
    void shouldReturnEmptyWhenRouteNameDoesNotExist() {
        Optional<Route> result = routeRepository.findByRouteName("Nonexistent-Route");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindRoutesByStatusAndSortThem() {
        List<Route> routes = routeRepository.findByStatus(RouteStatus.ACTIVE, Sort.by(Sort.Direction.ASC, "routeName"));
        assertEquals(2, routes.size());
        assertEquals("Lisbon-Porto", routes.get(0).getRouteName());
        assertEquals("Porto-Madrid", routes.get(1).getRouteName());
    }

    @Test
    void shouldFindRoutesByOriginAirportIataCode() {
        List<Route> routes = routeRepository.findByOriginAirport_IataCode("LIS");
        assertEquals(2, routes.size());
        assertTrue(routes.stream().allMatch(route -> route.getOriginAirport().getIataCode().equals("LIS")));
    }

    @Test
    void shouldReturnEmptyListWhenOriginAirportDoesNotExist() {
        List<Route> routes = routeRepository.findByOriginAirport_IataCode("XXX");
        assertTrue(routes.isEmpty());
    }

    @Test
    void shouldFindRoutesByOriginOrDestinationAirportIataCode() {
        List<Route> routes = routeRepository.findByOriginAirport_IataCodeOrDestinationAirport_IataCode("LIS", "MAD");
        assertEquals(3, routes.size());
    }

    @Test
    void shouldDeleteRoute() {
        Long routeId = activeRoute1.getRouteId();
        routeRepository.delete(activeRoute1);
        assertFalse(routeRepository.findById(routeId).isPresent());
    }

    @Test
    void shouldCheckIfRouteExists() {
        Long routeId = activeRoute1.getRouteId();
        assertTrue(routeRepository.existsById(routeId));
    }

    @Test
    void shouldReturnFalseWhenRouteDoesNotExist() {
        assertFalse(routeRepository.existsById(999999L));
    }
}
