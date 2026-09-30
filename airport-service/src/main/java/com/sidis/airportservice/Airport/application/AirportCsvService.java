package com.sidis.airportservice.Airport.application;

import com.sidis.airportservice.Airport.domain.*;
import com.opencsv.CSVReader;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

@Service
public class AirportCsvService {
    public List<Airport> importFile(InputStream inputStream) throws Exception {
        List<Airport> airports = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] row;
            boolean first = true;
            while ((row = reader.readNext()) != null) {
                // skip header
                if (first) {
                    first = false;
                    continue;
                }
                if (row.length < 15) continue; // basic safety check
                Airport airport = new Airport(
                        row[0], // iataCode
                        row[1], // type
                        row[2], // name
                        AirportStatus.valueOf(row[3]),
                        new AirportLocation(
                                row[4],                          // city
                                parseDouble(row[5]),             // longitude
                                parseDouble(row[6]),             // latitude
                                row[7],                          // region
                                row[8],                          // timezone
                                row[9]                           // country
                        ),
                        parseFacilities(row),
                        parseDouble(row[10]),               // maxCapacity
                        parseRunways(row[11]),
                        parseCertifications(row[12]),
                        parseContacts(row[13]),
                        splitList(row[14])                  // images
                );
                airports.add(airport);
            }
        }

        return airports;
    }

    private Double parseDouble(String v) {
        try {
            return v == null ? 0.0 : Double.parseDouble(v);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.asList(value.split(";"));
    }

    private Facilities parseFacilities(String[] row) {
        return new Facilities(
                1,
                splitList(row[11]),
                parseDouble(row[10]).intValue()
        );
    }

    private List<Runway> parseRunways(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(";"))
                .map(x -> x.split("\\|"))
                .filter(x -> x.length >= 4)
                .map(x -> new Runway(
                        null,
                        x[0],
                        parseDouble(x[1]),
                        x[2],
                        RunwayStatus.valueOf(x[3])
                ))
                .toList();
    }

    private List<Certification> parseCertifications(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(";"))
                .map(name -> new Certification(
                        name,
                        "STANDARD",
                        LocalDate.now(),
                        LocalDate.now()
                ))
                .toList();
    }

    private List<Contact> parseContacts(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        return Arrays.stream(raw.split(";"))
                .map(c -> c.split("\\|"))
                .filter(c -> c.length >= 2)
                .map(c -> new Contact(
                        ContactType.valueOf(c[0]),
                        c[1]
                ))
                .toList();
    }
}