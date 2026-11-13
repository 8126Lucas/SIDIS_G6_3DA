package com.sidis.airportservice.Airport.application;

import com.sidis.airportservice.Airport.domain.Airport;
import com.sidis.airportservice.Airport.domain.AirportLocation;
import com.sidis.airportservice.Airport.domain.AirportStatus;
import com.sidis.airportservice.Airport.domain.Certification;
import com.sidis.airportservice.Airport.domain.Contact;
import com.sidis.airportservice.Airport.domain.Facilities;
import com.sidis.airportservice.Airport.domain.Runway;
import com.sidis.airportservice.Airport.infrastructure.AirportRepository;
import com.sidis.airportservice.UseCase;
import com.sidis.airportservice.exceptions.DomainException;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;

@UseCase
@Transactional
public class AirportService {
    private final AirportRepository airportRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AirportService(AirportRepository airportRepository, ApplicationEventPublisher eventPublisher) {
        this.airportRepository = airportRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(Airport airport) {
        validateIataCode(airport.getIataCode());
        if (airportRepository.findByIataCode(airport.getIataCode()).isPresent()) {
            throw new DomainException("Airport already exists");
        }
        saveAndPublish(airport);
    }

    public void execute(String iataCode, String airportType, String name,
                        AirportStatus status, AirportLocation airportLocation,
                        Facilities facilities, Double routeDistance,
                        List<Runway> airportRunways, List<Certification> certifications,
                        List<Contact> contacts, List<String> airportPhotos) {
        execute(new Airport(iataCode, airportType, name, status, airportLocation,
                facilities, routeDistance, airportRunways, certifications,
                contacts, airportPhotos));
    }

    public Certification addCertification(String iataCode, String name,
                                          String category, LocalDate startsAt, LocalDate expiresAt) {
        Airport airport = airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));

        Certification certification =
                new Certification(name, category, startsAt, expiresAt);
        airport.addCertification(certification);
        saveAndPublish(airport);
        return certification;
    }

    public Airport updateStatus(String iataCode, AirportStatus status) {
        Airport airport = airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));
        airport.changeStatus(status);
        return saveAndPublish(airport);
    }

    public Airport updateDetails(String iataCode, String name, String operationalHours,
                                 List<Contact> contacts) {
        Airport airport = airportRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));
        airport.updateDetails(name, operationalHours, contacts);
        return saveAndPublish(airport);
    }

    private void validateIataCode(String iataCode) {
        if (iataCode == null || !iataCode.matches("[A-Z]{3}")) {
            throw new DomainException("IATA code must have 3 uppercase letters");
        }
    }
    private Airport saveAndPublish(Airport airport) {
        Airport saved = airportRepository.saveAndFlush(airport);

        eventPublisher.publishEvent(
                new AirportChangedEvent(
                        AirportReadModelMapper.from(saved)
                )
        );

        return saved;
    }
}
