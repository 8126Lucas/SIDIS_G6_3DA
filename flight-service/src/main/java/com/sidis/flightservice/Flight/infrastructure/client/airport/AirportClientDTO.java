package com.sidis.flightservice.Flight.infrastructure.client.airport;

import java.util.List;

public record AirportClientDTO(
    String iataCode,
    String airportType,
    String name,
    Long version,
    String status,
    AirportLocationDTO airportLocation,
    FacilitiesDTO facilities,
    Double routeDistance,
    List<RunwayDTO> airportRunways,
    List<CertificationDTO> certifications,
    List<ContactDTO> contacts,
    List<String> airportPhotos
){
}
