package com.sidis.airportservice;

import com.sidis.airportservice.Airport.domain.*;
import com.sidis.airportservice.Airport.infrastructure.AirportRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Component
public class Bootstrap implements ApplicationRunner {
    private final AirportRepository airportRepository;

    public Bootstrap(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println("========== BOOTSTRAP RUNNING ==========");
        long count = airportRepository.count();
        System.out.println("Existing airports: " + count);
        if (count == 0) {
            seedAirports();
            System.out.println("========== AIRPORTS SEEDED ==========");
        }
    }

    private void seedAirports() {
        airportRepository.save(new Airport(
                "OPO",
                "International",
                "Francisco Sá Carneiro Airport",
                AirportStatus.OPERATIONAL,
                new AirportLocation(
                        "Porto",
                        -8.68139,
                        41.2421,
                        "Porto District",
                        "Europe/Lisbon",
                        "Portugal"
                ),
                new Facilities(
                        1,
                        List.of(
                                "Cargo Terminal",
                                "Restaurants",
                                "Duty Free",
                                "Free WiFi",
                                "Parking"
                        ),
                        35
                ),
                313.0,
                List.of(
                        new Runway(
                                null,
                                "17/35",
                                3480.0,
                                "Asphalt",
                                RunwayStatus.IN_USE
                        )
                ),
                List.of(
                        new Certification(
                                "ICAO Annex 14 Compliance",
                                "SAFETY",
                                LocalDate.of(2023, 1, 1),
                                LocalDate.of(2027, 1, 1)
                        )
                ),
                List.of(
                        new Contact(ContactType.PHONE, "+351 22 943 2400"),
                        new Contact(ContactType.EMAIL, "info@ana.pt")
                ),
                List.of(
                        "opo_terminal.jpg",
                        "opo_runway.jpg",
                        "opo_night_view.jpg"
                )
        ));

        airportRepository.save(new Airport(
                "LIS",
                "International",
                "Humberto Delgado Airport",
                AirportStatus.OPERATIONAL,
                new AirportLocation(
                        "Lisbon",
                        -9.13592,
                        38.7813,
                        "Lisbon Region",
                        "Europe/Lisbon",
                        "Portugal"
                ),
                new Facilities(
                        2,
                        List.of(
                                "VIP Lounge",
                                "Medical Center",
                                "Duty Free",
                                "Metro Access",
                                "WiFi",
                                "Conference Rooms"
                        ),
                        47
                ),
                0.0,
                List.of(
                        new Runway(
                                null,
                                "03/21",
                                3805.0,
                                "Asphalt",
                                RunwayStatus.OPEN
                        ),
                        new Runway(
                                null,
                                "17/35",
                                2400.0,
                                "Asphalt",
                                RunwayStatus.UNDER_MAINTENANCE
                        )
                ),
                List.of(
                        new Certification(
                                "ICAO Safety Certification",
                                "SAFETY",
                                LocalDate.of(2024, 1, 1),
                                LocalDate.of(2028, 1, 1)
                        ),
                        new Certification(
                                "EASA Aerodrome License",
                                "REGULATORY",
                                LocalDate.of(2023, 6, 1),
                                LocalDate.of(2026, 6, 1)
                        )
                ),
                List.of(
                        new Contact(ContactType.PHONE, "+351 21 841 3500"),
                        new Contact(ContactType.EMAIL, "lisbon.airport@ana.pt")
                ),
                List.of(
                        "lis_terminal_1.jpg",
                        "lis_terminal_2.jpg",
                        "lis_runway_night.jpg"
                )
        ));

        airportRepository.save(new Airport(
                "MAD",
                "International",
                "Adolfo Suárez Madrid–Barajas Airport",
                AirportStatus.OPERATIONAL,
                new AirportLocation(
                        "Madrid",
                        -3.56795,
                        40.4983,
                        "Madrid Region",
                        "Europe/Madrid",
                        "Spain"
                ),
                new Facilities(
                        4,
                        List.of(
                                "High-Speed Rail Connection",
                                "VIP Lounges",
                                "Shopping Mall",
                                "Cargo Hub",
                                "Hotel On-Site",
                                "WiFi"
                        ),
                        60
                ),
                0.0,
                List.of(
                        new Runway(
                                null,
                                "14L/32R",
                                3500.0,
                                "Asphalt",
                                RunwayStatus.IN_USE
                        ),
                        new Runway(
                                null,
                                "14R/32L",
                                3500.0,
                                "Asphalt",
                                RunwayStatus.IN_USE
                        ),
                        new Runway(
                                null,
                                "18L/36R",
                                4100.0,
                                "Asphalt",
                                RunwayStatus.OPEN
                        ),
                        new Runway(
                                null,
                                "18R/36L",
                                3900.0,
                                "Asphalt",
                                RunwayStatus.OPEN
                        )
                ),
                List.of(
                        new Certification(
                                "EU Major Airport Compliance",
                                "REGULATORY",
                                LocalDate.of(2022, 5, 1),
                                LocalDate.of(2027, 5, 1)
                        )
                ),
                List.of(
                        new Contact(ContactType.PHONE, "+34 913 21 10 00"),
                        new Contact(ContactType.EMAIL, "info@aena.es")
                ),
                List.of(
                        "mad_terminal.jpg",
                        "mad_runways.jpg",
                        "mad_control_tower.jpg"
                )
        ));

        airportRepository.save(new Airport(
                "CDG",
                "International",
                "Charles de Gaulle Airport",
                AirportStatus.OPERATIONAL,
                new AirportLocation(
                        "Paris",
                        2.55,
                        49.0097,
                        "Île-de-France",
                        "Europe/Paris",
                        "France"
                ),
                new Facilities(
                        3,
                        List.of(
                                "TGV Station",
                                "Luxury Lounges",
                                "Cargo Hub",
                                "Duty Free",
                                "WiFi",
                                "Medical Center"
                        ),
                        78
                ),
                0.0,
                List.of(
                        new Runway(
                                null,
                                "09L/27R",
                                4200.0,
                                "Concrete",
                                RunwayStatus.IN_USE
                        ),
                        new Runway(
                                null,
                                "09R/27L",
                                2700.0,
                                "Concrete",
                                RunwayStatus.OPEN
                        ),
                        new Runway(
                                null,
                                "08L/26R",
                                4215.0,
                                "Concrete",
                                RunwayStatus.OPEN
                        )
                ),
                List.of(
                        new Certification(
                                "ICAO Category III Landing Capability",
                                "SAFETY",
                                LocalDate.of(2023, 9, 1),
                                LocalDate.of(2028, 9, 1)
                        )
                ),
                List.of(
                        new Contact(ContactType.PHONE, "+33 1 70 36 39 50"),
                        new Contact(ContactType.EMAIL, "info@parisaeroport.fr")
                ),
                List.of(
                        "cdg_terminal.jpg",
                        "cdg_runway.jpg",
                        "cdg_night.jpg"
                )
        ));

        airportRepository.save(new Airport(
                "LHR",
                "International",
                "London Heathrow Airport",
                AirportStatus.OPERATIONAL,
                new AirportLocation(
                        "London",
                        -0.4543,
                        51.4700,
                        "Greater London",
                        "Europe/London",
                        "United Kingdom"
                ),
                new Facilities(
                        4,
                        List.of(
                                "Heathrow Express",
                                "Premium Lounges",
                                "Duty Free",
                                "Hotels",
                                "WiFi",
                                "Cargo Operations"
                        ),
                        85
                ),
                0.0,
                List.of(
                        new Runway(
                                null,
                                "09L/27R",
                                3902.0,
                                "Asphalt",
                                RunwayStatus.IN_USE
                        ),
                        new Runway(
                                null,
                                "09R/27L",
                                3660.0,
                                "Asphalt",
                                RunwayStatus.OPEN
                        )
                ),
                List.of(
                        new Certification(
                                "UK CAA Operational Approval",
                                "REGULATORY",
                                LocalDate.of(2024, 2, 1),
                                LocalDate.of(2029, 2, 1)
                        )
                ),
                List.of(
                        new Contact(ContactType.PHONE, "+44 844 335 1801"),
                        new Contact(ContactType.EMAIL, "info@heathrow.com")
                ),
                List.of(
                        "lhr_terminal.jpg",
                        "lhr_runway.jpg"
                )
        ));
    }
}