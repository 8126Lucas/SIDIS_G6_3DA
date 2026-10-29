package com.sidis.routeservice.Route.infrastructure.client.airport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sidis.routeservice.Route.domain.AirportLocation;
import com.sidis.routeservice.Route.domain.AirportReference;

import java.util.List;

public record AirportClientDTO(
    String iataCode,
    String airportType,
    String name,
    Long version,
    String status,
    @JsonProperty("location")
    AirportLocationDTO airportLocation,
    FacilitiesDTO facilities,
    Double routeDistance,
    List<RunwayDTO> airportRunways,
    List<CertificationDTO> certifications,
    List<ContactDTO> contacts,
    List<String> airportPhotos
){
    public static AirportReference fromDTOtoAirportReference(AirportClientDTO dto) {
        AirportLocationDTO location = dto.airportLocation();
        return new AirportReference(
                dto.iataCode(),
                new AirportLocation(
                        location.city(),
                        location.longitude(),
                        location.latitude(),
                        location.region(),
                        location.timezone(),
                        location.country()
                )
        );
    }
}
