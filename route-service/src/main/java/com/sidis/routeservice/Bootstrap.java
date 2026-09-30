package com.sidis.routeservice;

import com.sidis.routeservice.domain.*;
import com.sidis.routeservice.infrastructure.RouteRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Component
public class Bootstrap implements ApplicationRunner {
    private final RouteRepository routeRepository;

    public Bootstrap(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("========== BOOTSTRAP RUNNING ==========");
        long count = routeRepository.count();
        System.out.println("Existing routes: " + count);

        if (routeRepository.count() == 0) {
            seedRoutes();
            System.out.println("========== ROUTES SEEDED ==========");
        }
    }

    private void seedRoutes() {
        AirportReference opo=new AirportReference("OPO");
        AirportReference lis=new AirportReference("LIS");
        AirportReference mad= new AirportReference("MAD");
        AirportReference cdg=new AirportReference("CDG");
        AirportReference lhr=new AirportReference("LHR");

        routeRepository.save(new Route(
                new RouteRequirements(1000.0, 100),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.now(), 0),
                1.0,
                opo,
                lis,
                "TEST ROUTE 1"
        ));

        routeRepository.save(new Route(
                new RouteRequirements(1000.0, 150),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.now(), 0),
                1.0,
                lis,
                mad,
                "TEST ROUTE 2"
        ));

        routeRepository.save(new Route(
                new RouteRequirements(1000.0, 250),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.now(), 0),
                1.0,
                mad,
                cdg,
                "TEST ROUTE 3"
        ));

        routeRepository.save(new Route(
                new RouteRequirements(10000.0, 100),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.now(), 0),
                1.0,
                cdg,
                lhr,
                "TEST ROUTE 4"
        ));

        // ===================== OPO → LIS =====================
        routeRepository.save(new Route(
                new RouteRequirements(2000.0, 128),
                RouteStatus.ACTIVE,
                RouteType.SCALED,
                new RouteHistory(LocalDate.of(2018, 5, 10), 0),
                24.0,200.0,
                opo,
                lis,
                "Pati&Patatá"
        ));

        // ===================== OPO → MAD =====================
        routeRepository.save(new Route(
                new RouteRequirements(8500.0, 180),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2019, 3, 15), 2),
                1.5,500.0,
                opo,
                mad,
                "Iberia Connect"
        ));

        // ===================== LIS → MAD =====================
        routeRepository.save(new Route(
                new RouteRequirements(9000.0, 180),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2020, 7, 1), 1),
                1.2,600.0,
                lis,
                mad,
                "TAP Express"
        ));

        // ===================== MAD → CDG =====================
        routeRepository.save(new Route(
                new RouteRequirements(14000.0, 220),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2017, 10, 20), 5),
                2.1,1000.0,
                mad,
                cdg,
                "Air France Iberia Codeshare"
        ));

        // ===================== CDG → LHR =====================
        routeRepository.save(new Route(
                new RouteRequirements(12000.0, 200),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2016, 6, 5), 8),
                1.3,700.0,
                cdg,
                lhr,
                "Air France - British Airways Alliance"
        ));

        // ===================== LHR → LIS =====================
        routeRepository.save(new Route(
                new RouteRequirements(15000.0, 240),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2015, 2, 12), 10),
                2.5,900.0,
                lhr,
                lis,
                "British Airways"
        ));

        // ===================== MAD → OPO =====================
        routeRepository.save(new Route(
                new RouteRequirements(8000.0, 160),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2021, 11, 30), 1),
                1.4,600.0,
                mad,
                opo,
                "Iberia Regional"
        ));

        // ===================== CDG → MAD =====================
        routeRepository.save(new Route(
                new RouteRequirements(14500.0, 230),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2018, 8, 25), 6),
                2.0,700.0,
                cdg,
                mad,
                "Air France"
        ));

        // ===================== LHR → CDG =====================
        routeRepository.save(new Route(
                new RouteRequirements(11000.0, 210),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(LocalDate.of(2017, 4, 14), 7),
                1.25,800.0,
                lhr,
                cdg,
                "British Airways"
        ));
    }

}