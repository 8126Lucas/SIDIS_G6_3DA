package com.sidis.routeservice.Route.application;

import com.sidis.routeservice.Route.domain.AirportReference;
import com.sidis.routeservice.Route.domain.Route;
import com.sidis.routeservice.Route.domain.RouteReadModel;

public final class RouteReadModelMapper {

    private RouteReadModelMapper() {}

    public static RouteReadModel from(Route route) {
        var history = route.getRouteHistory();
        var requirements = route.getRouteRequirements();

        return new RouteReadModel(
                route.getRouteId(),
                route.getRouteName(),
                route.getStatus(),
                route.getType(),
                airportData(route.getOriginAirport()),
                airportData(route.getDestinationAirport()),
                history == null ? null : new RouteReadModel.HistoryData(
                        history.getRouteBegin(),
                        history.getRouteFinish(),
                        history.getRouteUsage()
                ),
                route.getEstimatedFlightTimeHours(),
                route.getDistanceKm(),
                requirements == null ? null : new RouteReadModel.RequirementsData(
                        requirements.getRequiredRange(),
                        requirements.getRequiredCapacity()
                ),
                route.getVersion()
        );
    }

    private static RouteReadModel.AirportData airportData(AirportReference airport) {
        if (airport == null) {
            return null;
        }

        var location = airport.getAirportLocation();
        return new RouteReadModel.AirportData(
                airport.getIataCode(),
                location == null ? null : new RouteReadModel.Location(
                        location.getCity(),
                        location.getCountry(),
                        location.getTimezone(),
                        location.getRegion(),
                        location.getLatitude(),
                        location.getLongitude()
                )
        );
    }
}
