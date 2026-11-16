package com.sidis.flightservice.Flight.application;

import com.sidis.flightservice.Flight.domain.Flight;
import com.sidis.flightservice.Flight.domain.FlightReadModel;

public final class FlightReadModelMapper {

    private FlightReadModelMapper() {}

    public static FlightReadModel from(Flight flight) {
        var route = flight.getRoute();
        var aircraft = flight.getAircraft();

        return new FlightReadModel(
                flight.getFlightId(),
                route == null ? null : new FlightReadModel.RouteData(
                        route.getRouteId(),
                        route.getRouteName(),
                        route.getOriginIata(),
                        route.getDestinationIata()
                ),
                aircraft == null ? null : aircraft.getRegistrationNumber(),
                flight.getFlightStatus(),
                flight.getScheduledDeparture(),
                flight.getScheduledArrival(),
                flight.getTimeOfDeparture(),
                flight.getTimeOfArrival(),
                flight.getVersion()
        );
    }
}
