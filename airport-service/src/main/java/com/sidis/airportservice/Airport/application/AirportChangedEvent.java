package com.sidis.airportservice.Airport.application;

import com.sidis.airportservice.Airport.domain.AirportReadModel;

public record AirportChangedEvent(AirportReadModel airport) {
}