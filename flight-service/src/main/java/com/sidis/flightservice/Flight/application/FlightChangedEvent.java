package com.sidis.flightservice.Flight.application;

import com.sidis.flightservice.Flight.domain.FlightReadModel;

public record FlightChangedEvent(FlightReadModel flight) {
}
