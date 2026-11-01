package com.sidis.flightservice.Flight.application;

import com.sidis.flightservice.Flight.infrastructure.FlightRepository;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftAvailability;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftClientDTO;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftModelDTO;
import com.sidis.flightservice.Flight.infrastructure.client.airport.AirportClientDTO;
import com.sidis.flightservice.Flight.infrastructure.client.airport.AirportStatus;
import com.sidis.flightservice.Flight.infrastructure.client.route.RouteHistoryDTO;
import com.sidis.flightservice.Flight.infrastructure.client.route.RouteResponse;
import com.sidis.flightservice.Flight.infrastructure.client.route.RouteStatus;
import com.sidis.flightservice.Flight.infrastructure.client.route.UpdateRouteRequest;
import com.sidis.flightservice.UseCase;
import com.sidis.flightservice.Flight.domain.*;
import com.sidis.flightservice.exceptions.DomainException;
import com.sidis.flightservice.Flight.infrastructure.client.*;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@UseCase
@Transactional
public class RouteService {
    private final FlightRepository flightRepository;
    private final RouteRestClientService routeClientRepository;
    private final AirportRestClientService airportRepository;

    @Autowired
    public RouteService(RouteRestClientService routeRepository, FlightRepository flightRepository, RouteRestClientService routeRepository1, AirportRestClientService airportRepository) {
        this.routeClientRepository = routeRepository;
        this.flightRepository = flightRepository;
        this.airportRepository = airportRepository;
    }

    public Flight scheduleFlight(Long routeId, AircraftModelDTO modelDTO, AircraftClientDTO aircraft, LocalDateTime scheduledDeparture, LocalDateTime scheduledArrival) {
        RouteResponse route = routeClientRepository.findByRouteId(routeId);
        AirportClientDTO airportOrigin=airportRepository.findAirportByIataCode(route.originAirportIata());
        AirportClientDTO airportDestination=airportRepository.findAirportByIataCode(route.destinationAirportIata());

        validateFlightSchedule(modelDTO,route, aircraft, scheduledDeparture, scheduledArrival,airportOrigin,airportDestination);

        RouteReference newRoute=new RouteReference(route.routeId(),route.originAirportIata(),route.routeName(),route.destinationAirportIata());
        AircraftReference newAircraft=new AircraftReference(aircraft.registrationNumber());
        Flight flight = new Flight(newRoute, newAircraft, scheduledDeparture, scheduledArrival);

        RouteHistoryDTO newHistory=new RouteHistoryDTO(route.routeHistoryDTO().routeBegin(),
                route.routeHistoryDTO().routeFinish(),route.routeHistoryDTO().routeUsage()+1);

        UpdateRouteRequest updatedRoute=new UpdateRouteRequest(route.destinationAirportIata(),
                route.estimatedFlightTimeHours(),RouteStatus.valueOf(route.routeStatus()),newHistory);

        routeClientRepository.update(route.routeId(),updatedRoute);
        return flightRepository.save(flight);
    }

    public List<Flight> getScheduledFlightsForAircraft(String registrationNumber) {
        return flightRepository.findByAircraftRegistrationNumber_RegistrationNumber(registrationNumber);
    }

    public void validateFlightSchedule(AircraftModelDTO modelDTO,RouteResponse route, AircraftClientDTO aircraft, LocalDateTime scheduledDeparture, LocalDateTime scheduledArrival,AirportClientDTO airportOrigin, AirportClientDTO airportDestination) {
        if (aircraft == null) {
            throw new DomainException("Aircraft does not exist");
        }
        if (scheduledDeparture == null || scheduledArrival == null
                || !scheduledArrival.isAfter(scheduledDeparture)) {
            throw new DomainException("Scheduled arrival must be after departure");
        }
        if (!Objects.equals(route.routeStatus(), RouteStatus.ACTIVE.toString())) {
            throw new DomainException("Route is not active");
        }
        if (!Objects.equals(airportOrigin.status(), AirportStatus.OPERATIONAL.toString()) ||
                !Objects.equals(airportDestination.status(), AirportStatus.OPERATIONAL.toString())) {
            throw new DomainException("Airport is not operational");
        }
        if (!Objects.equals(aircraft.status(), AircraftAvailability.AVAILABLE.toString())) {
            throw new DomainException("Aircraft is not available");
        }
        if (aircraftRange(aircraft,modelDTO) < requiredRange(route)) {
            throw new DomainException("Aircraft range does not comply with route requirements");
        }

        Integer requiredCapacity = route.routeRequirementsDTO() != null ? route.routeRequirementsDTO().requiredCapacity(): null;
        Integer aircraftCapacity = modelDTO.specs().standardSeatingCapacity();

        if (requiredCapacity != null
                && (aircraftCapacity == null || aircraftCapacity < requiredCapacity)) {
            throw new DomainException("Aircraft has insufficient capacity");
        }
        boolean aircraftBusy = flightRepository
                .existsByAircraftRegistrationNumber_RegistrationNumberAndScheduledDepartureLessThanAndScheduledArrivalGreaterThan(
                        aircraft.registrationNumber(), scheduledArrival, scheduledDeparture);
        if (aircraftBusy) {
            throw new DomainException("Aircraft is already scheduled for this period");
        }
    }

    private Double requiredRange(RouteResponse route) {
        if (route.routeRequirementsDTO() != null
                && route.routeRequirementsDTO().requiredRange() != null) {
            return route.routeRequirementsDTO().requiredRange();
        }
        return route.distanceKm();
    }

    private Double aircraftRange(AircraftClientDTO aircraft, AircraftModelDTO modelDTO) {
        if (aircraft.meanRange() != null && aircraft.meanRange() > 0) {
            return aircraft.meanRange();
        }
        if (modelDTO.specs() != null && modelDTO.specs().maximumRangeKm() != null) {
            return modelDTO.specs().maximumRangeKm();
        }
        throw new DomainException("Aircraft range information is unavailable");
    }
}
