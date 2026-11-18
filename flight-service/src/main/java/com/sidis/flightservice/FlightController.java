package com.sidis.flightservice;

import com.sidis.flightservice.Flight.application.AircraftSearchService;
import com.sidis.flightservice.Flight.application.FlightQueryService;
import com.sidis.flightservice.Flight.application.FlightService;
import com.sidis.flightservice.Flight.domain.Flight;
import com.sidis.flightservice.Flight.domain.FlightReadModel;
import com.sidis.flightservice.Flight.domain.FlightStatus;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftClientDTO;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.AircraftModelDTO;
import com.sidis.flightservice.exceptions.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {
    private final FlightService flightService;
    private final AircraftSearchService aircraftSearchService;
    private final FlightQueryService flightQueryService;

    public FlightController(FlightService flightService,
                            AircraftSearchService aircraftSearchService,
                            FlightQueryService flightQueryService) {
        this.flightService = flightService;
        this.aircraftSearchService = aircraftSearchService;
        this.flightQueryService = flightQueryService;
    }

    @GetMapping({"", "/"})
    public List<FlightResponse> getFlights() {
        List<FlightReadModel> flights = flightQueryService.findAll();
        return flights.stream()
                .map(FlightResponse::from)
                .toList();
    }

    @PostMapping("/scheduled")
    @ResponseStatus(HttpStatus.CREATED)
    public FlightResponse scheduleFlight(@Valid @RequestBody ScheduleFlightRequest request) {
        AircraftClientDTO aircraft = aircraftSearchService
                .spotAircraftInHangar(request.aircraftRegistrationNumber())
                .orElseThrow(() -> new DomainException("Aircraft does not exist"));
        AircraftModelDTO model=aircraftSearchService.spotAircraftModelInCatalog(aircraft.modelName()).orElseThrow(() -> new DomainException("Aircraft does not exist"));
        return FlightResponse.from(flightService.scheduleFlight(request.routeId(),model,
                aircraft, request.scheduledDeparture(), request.scheduledArrival()));
    }

    @GetMapping("/aircraft/{registrationNumber}")
    public List<FlightResponse> getScheduledFlightsForAircraft(@PathVariable String registrationNumber) {
        return flightQueryService.getScheduledFlightsForAircraft(registrationNumber)
                .stream()
                .map(FlightResponse::from)
                .toList();
    }

    @GetMapping("/reports/route-utilization")
    public List<RouteUtilizationReportResponse> getRouteUtilizationReport() {
        return flightQueryService.getRouteUtilizationReport()
                .stream()
                .map(RouteUtilizationReportResponse::from)
                .toList();
    }

    record ScheduleFlightRequest(@NotNull Long routeId, @NotBlank String aircraftRegistrationNumber, @NotNull LocalDateTime scheduledDeparture, @NotNull LocalDateTime scheduledArrival) {
    }

    public record FlightResponse(Long flightId, Long routeId, String aircraftRegistrationNumber, FlightStatus status, LocalDateTime scheduledDeparture, LocalDateTime scheduledArrival) {
        public static FlightResponse from(Flight flight) {
            return new FlightResponse(flight.getFlightId(),
                    flight.getRoute().getRouteId(),
                    flight.getAircraft().getRegistrationNumber(),
                    flight.getFlightStatus(),
                    flight.getScheduledDeparture(),
                    flight.getScheduledArrival());
        }

        public static FlightResponse from(FlightReadModel flight) {
            return new FlightResponse(
                    flight.flightId(),
                    flight.route() == null ? null : flight.route().routeId(),
                    flight.aircraftRegistrationNumber(),
                    flight.status(),
                    flight.scheduledDeparture(),
                    flight.scheduledArrival()
            );
        }
    }

    public record RouteUtilizationReportResponse(Long routeId, String routeName,String originIata, String destinationIata, Long flightCount) {
        public static RouteUtilizationReportResponse from(
                FlightQueryService.RouteUtilizationReport report) {
            return new RouteUtilizationReportResponse(report.routeId(),
                    report.routeName(), report.originIata(), report.destinationIata(),
                    report.flightCount());
        }
    }
}
