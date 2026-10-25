package com.sidis.aircraftservice.Aircraft.application;

//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
//import com.sidis.aircraftservice.Flight.domain.Flight;
//import com.sidis.aircraftservice.Flight.infrastructure.FlightRepository;
import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.infrastructure.client.FlightClientDTO;
import com.sidis.aircraftservice.infrastructure.client.FlightRestClientService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Service("applicationCalculationsService")
public class CalculationsService {
    private final AircraftSearchService aircraftSearchService;
    private final FlightRestClientService flightClient;

    CalculationsService(AircraftSearchService aircraftSearchService, FlightRestClientService flightClient) {
        this.aircraftSearchService = aircraftSearchService;
        this.flightClient = flightClient;
    }

    public List<HangarController.UtilizationInfo> getUtilizationInfo() {

        List<Aircraft> aircrafts = aircraftSearchService.findAll();

        return aircrafts.stream()
                .flatMap(aircraft -> {
                    List<FlightClientDTO> allAircraftFlights = flightClient
                            .getFlightsByAircraft(aircraft.getRegistrationNumber());

                    List<HangarController.UtilizationInfo> result = new ArrayList<>();

                    for (int i = 11; i >= 0; i--) {
                        LocalDate monthDate = LocalDate.now().minusMonths(i);
                        LocalDateTime start = monthDate.withDayOfMonth(1).atStartOfDay();
                        LocalDateTime end = monthDate.withDayOfMonth(monthDate.lengthOfMonth()).atTime(23, 59, 59);
                        double flightHours = allAircraftFlights.stream()
                                .filter(f -> f.departure() != null &&
                                        !LocalDateTime.parse(f.departure()).isBefore(start) &&
                                        !LocalDateTime.parse(f.departure()).isAfter(end))
                                .filter(f -> f.flightHours() != null)
                                .mapToDouble(FlightClientDTO::flightHours)
                                .sum();

                        double availableHours = ChronoUnit.HOURS.between(start, end);
                        double utilizationRate = 0;

                        if (availableHours > 0) {
                            utilizationRate = (flightHours / availableHours) * 100;
                        }

                        double utilizationRateRounded = Math.round(utilizationRate * 100.0) / 100.0;

                        result.add(new HangarController.UtilizationInfo(
                                aircraft.getRegistrationNumber(),
                                monthDate,
                                utilizationRateRounded
                        ));
                    }
                    return result.stream();
                })
                .toList();
    }
}
