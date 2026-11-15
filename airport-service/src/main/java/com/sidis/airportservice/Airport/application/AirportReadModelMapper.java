package com.sidis.airportservice.Airport.application;

import com.sidis.airportservice.Airport.domain.Airport;
import com.sidis.airportservice.Airport.domain.AirportReadModel;

import java.util.List;

public final class AirportReadModelMapper {

    private AirportReadModelMapper() {}

    public static AirportReadModel from(Airport airport) {
        var location = airport.getAirportLocation();
        var facilities = airport.getFacilities();

        return new AirportReadModel(
                airport.getIataCode(),
                airport.getAirportType(),
                airport.getName(),
                airport.getStatus(),
                location == null ? null : new AirportReadModel.Location(
                        location.getCity(),
                        location.getCountry(),
                        location.getTimezone(),
                        location.getRegion(),
                        location.getLatitude(),
                        location.getLongitude()
                ),
                facilities == null ? null : new AirportReadModel.FacilitiesData(
                        facilities.getTerminalCount(),
                        facilities.getGateCount(),
                        List.copyOf(facilities.getServices())
                ),
                airport.getRouteDistance(),
                airport.getAirportRunways().stream()
                        .map(runway -> new AirportReadModel.RunwayData(
                                runway.getRunwayId(),
                                runway.getRunwayName(),
                                runway.getLength(),
                                runway.getOrientation(),
                                runway.getStatus()
                        ))
                        .toList(),
                airport.getCertifications().stream()
                        .map(certification -> new AirportReadModel.CertificationData(
                                certification.getCertificationId(),
                                certification.getName(),
                                certification.getCategory(),
                                certification.getStartsAt(),
                                certification.getExpiresAt()
                        ))
                        .toList(),
                airport.getContacts().stream()
                        .map(contact -> new AirportReadModel.ContactData(
                                contact.getType(),
                                contact.getValue()
                        ))
                        .toList(),
                List.copyOf(airport.getAirportPhotos()),
                airport.getOperationalHours(),
                airport.getVersion()
        );
    }
}