package com.sidis.aircraftservice.Aircraft.infrastructure;

import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
//import com.sidis.aircraftservice.Flight.domain.Flight;
//import com.sidis.aircraftservice.Flight.infrastructure.FlightRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Service
public class CalculationsService {
    private final AircraftSearchService aircraftSearchService;
//    private final FlightSearchService flightSearchService;

    CalculationsService(AircraftSearchService aircraftSearchService/*, FlightSearchService flightSearchService*/) {
        this.aircraftSearchService = aircraftSearchService;
//        this.flightSearchService = flightSearchService;
    }

/*    public List<HangarController.UtilizationInfo> getUtilizationInfo() {

        List<Aircraft> aircrafts = aircraftSearchService.findAll();

        return aircrafts.stream()
                .flatMap(aircraft -> {

                    List<HangarController.UtilizationInfo> result = new ArrayList<>();
                    for (int i = 11; i >= 0; i--) {
                        LocalDate monthDate = LocalDate.now().minusMonths(i);
                        LocalDateTime start = monthDate
                                .withDayOfMonth(1)
                                .atStartOfDay();
                        LocalDateTime end = monthDate
                                .withDayOfMonth(monthDate.lengthOfMonth())
                                .atTime(23, 59, 59);

                        List<Flight> flights =
                                flightSearchService
                                        .findByAircraftRegistrationNumberAndScheduledDepartureBetween(
                                                aircraft.getRegistrationNumber(),
                                                start,
                                                end
                                        );

                        double flightHours = flights.stream()
                                .filter(f -> f.getFlightHours() != null)
                                .mapToDouble(Flight::getFlightHours)
                                .sum();

                        double availableHours =ChronoUnit.HOURS.between(start, end);

                        double utilizationRate = 0;

                        if (availableHours > 0) {
                            utilizationRate =(flightHours / availableHours) * 100;
                        }

                        result.add(new HangarController.UtilizationInfo(
                                        aircraft.getRegistrationNumber(),
                                        monthDate,
                                        utilizationRate
                                )
                        );
                    }
                    return result.stream();
                })
                .toList();
    }*/
}
