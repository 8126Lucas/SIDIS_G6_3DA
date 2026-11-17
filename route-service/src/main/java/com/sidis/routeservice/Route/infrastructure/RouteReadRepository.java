package com.sidis.routeservice.Route.infrastructure;

import com.sidis.routeservice.Route.domain.RouteReadModel;
import com.sidis.routeservice.Route.domain.RouteStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RouteReadRepository extends CrudRepository<RouteReadModel, Long> {

    List<RouteReadModel> findByStatus(RouteStatus status);

    List<RouteReadModel> findByOriginAirport_IataCode(String originIataCode);

    List<RouteReadModel> findByDestinationAirport_IataCode(String destinationIataCode);
}
