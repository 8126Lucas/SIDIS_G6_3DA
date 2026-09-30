package com.sidis.routeservice.infrastructure;

import com.sidis.routeservice.domain.Route;
import com.sidis.routeservice.domain.RouteStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long>, JpaSpecificationExecutor<Route> {
    Optional<Route> findByRouteId(Long RouteId);
    Optional<Route> findByRouteName(String RouteName);
    List<Route> findByStatus(RouteStatus status, Sort sort);
    List<Route> findByOriginAirport_IataCode(String originIataCode);
    List<Route> findByOriginAirport_IataCodeOrDestinationAirport_IataCode(String originIataCode, String destinationIataCode);
}
