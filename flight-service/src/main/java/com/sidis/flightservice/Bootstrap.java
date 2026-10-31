package com.sidis.flightservice;

import com.sidis.flightservice.Flight.domain.AircraftReference;
import com.sidis.flightservice.Flight.domain.Flight;
import com.sidis.flightservice.Flight.domain.RouteReference;
import com.sidis.flightservice.Flight.infrastructure.FlightRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class Bootstrap implements ApplicationRunner {

    private final FlightRepository flightRepository;

    public Bootstrap(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("========== BOOTSTRAP RUNNING ==========");

        long count = flightRepository.count();
        System.out.println("Existing flights: " + count);

        if (count == 0) {
            seedFlights();
            System.out.println("========== FLIGHTS SEEDED ==========");
        }
    }

    private void seedFlights() {
        AircraftReference csTua = new AircraftReference("CS-TUA");
        AircraftReference dAbxa = new AircraftReference("D-ABXA");
        AircraftReference fHxka = new AircraftReference("F-HXKA");
        AircraftReference n789dl = new AircraftReference("N789DL");

        RouteReference route1 = new RouteReference(1L, "OPO", "Porto-Lisbon", "LIS");
        RouteReference route2 = new RouteReference(2L, "LIS", "Lisbon-Madrid", "MAD");
        RouteReference route3 = new RouteReference(3L, "MAD", "Madrid-Paris", "CDG");
        RouteReference route4 = new RouteReference(4L, "CDG", "Paris-London", "LHR");

        Flight f1 = new Flight(route1, csTua, LocalDateTime.of(2026, 6, 15, 8, 0), LocalDateTime.of(2026, 6, 15, 9, 0));
        Flight f2 = new Flight(route1, dAbxa, LocalDateTime.of(2026, 6, 15, 10, 30), LocalDateTime.of(2026, 6, 15, 12, 0));
        Flight f3 = new Flight(route2, fHxka, LocalDateTime.of(2026, 6, 15, 14, 0), LocalDateTime.of(2026, 6, 15, 15, 20));
        Flight f4 = new Flight(route3, fHxka, LocalDateTime.of(2026, 6, 16, 9, 15), LocalDateTime.of(2026, 6, 16, 11, 20));
        Flight f5 = new Flight(route4, csTua, LocalDateTime.of(2026, 6, 16, 13, 0), LocalDateTime.of(2026, 6, 16, 14, 15));
        Flight f6 = new Flight(route3, n789dl, LocalDateTime.of(2026, 6, 17, 11, 0), LocalDateTime.of(2026, 6, 17, 13, 40));

        flightRepository.saveAll(List.of(f1, f2, f3, f4, f5, f6));
    }
}