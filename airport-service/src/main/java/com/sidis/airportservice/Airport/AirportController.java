package com.sidis.airportservice.Airport;

import com.sidis.airportservice.Airport.application.AirportService;
import com.sidis.airportservice.Airport.domain.*;
import com.sidis.airportservice.Airport.application.AirportCsvService;
//import com.sidis.airportservice.Route.domain.*;
import com.sidis.airportservice.exceptions.DomainException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/airports")
public class AirportController {
    private final AirportService airportService;
    private final AirportCsvService airportCsvService;

    public AirportController(AirportService airportService, AirportCsvService airportCsvService) {
        this.airportService = airportService;
        this.airportCsvService = airportCsvService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void registerAirport(@Valid @RequestBody AddAirportRequest request) {
        AirportLocation location = new AirportLocation(
                request.location().city(),
                request.location().longitude(),
                request.location().latitude(),
                request.location().region(),
                request.location().timezone(),
                request.location().country()
        );

        Facilities facilities = new Facilities(
                request.facilities().terminalCount(),
                request.facilities().services(),
                request.facilities().gateCount()
        );

        List<Runway> runways = request.runways().stream()
                .map(runway -> new Runway(
                        null,
                        runway.runwayName(),
                        runway.length(),
                        runway.orientation(),
                        RunwayStatus.valueOf(runway.status())
                ))
                .toList();

        List<Certification> certifications = request.certifications() == null
                ? List.of()
                : request.certifications().stream()
                .map(certification -> new Certification(
                        certification.name(),
                        certification.category(),
                        certification.startsAt(),
                        certification.expiresAt()
                ))
                .toList();

        List<Contact> contacts = request.contacts() == null
                ? List.of()
                : request.contacts().stream()
                .map(contact -> new Contact(
                        ContactType.valueOf(contact.type()),
                        contact.value()
                ))
                .toList();

        Airport airport = new Airport(
                request.iataCode(),
                request.airportType(),
                request.name(),
                AirportStatus.valueOf(request.status()),
                location,
                facilities,
                request.routeDistance(),
                runways,
                certifications,
                contacts,
                request.airportPhotos()
        );
        airportService.execute(airport);
    }

    @PostMapping("/{iataCode}/certifications")
    @ResponseStatus(HttpStatus.CREATED)
    public CertificationResponse addCertification(
            @PathVariable String iataCode,
            @Valid @RequestBody AddCertificationRequest request) {
        Certification certification = airportService.addCertification(
                iataCode,
                request.name(),
                request.category(),
                request.startsAt(),
                request.expiresAt()
        );
        return CertificationResponse.from(certification);
    }

    @GetMapping("/{iataCode}")
    public AirportResponse getAirport(@PathVariable String iataCode) {
        Airport airport = airportService.getAirportByIata(iataCode)
                .orElseThrow(() -> new DomainException("Airport does not exist"));
        return AirportResponse.from(airport);
    }

    @GetMapping
    public List<AirportResponse> searchAirports(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String name) {
        return airportService.searchAirports(city, country, name)
                .stream()
                .map(AirportResponse::from)
                .toList();
    }

    @PatchMapping("/{iataCode}/status")
    public AirportResponse updateAirportStatus(
            @PathVariable String iataCode,
            @Valid @RequestBody UpdateAirportStatusRequest request) {
        Airport airport = airportService.updateStatus(
                iataCode,
                AirportStatus.valueOf(request.status())
        );
        return AirportResponse.from(airport);
    }

    @PatchMapping("/{iataCode}/details")
    public AirportResponse updateAirportDetails(
            @PathVariable String iataCode,
            @Valid @RequestBody UpdateAirportDetailsRequest request) {
        Airport airport = airportService.updateDetails(
                iataCode,
                request.name(),
                request.operationalHours(),
                request.contacts() == null ? List.of() :
                    request.contacts().stream()
                        .map(contact -> new Contact(
                            ContactType.valueOf(contact.type()),
                            contact.value()
                        ))
                        .toList()
        );
        return AirportResponse.from(airport);
    }
    /*
    @GetMapping("/{iataCode}/routes")
    public List<RouteResponse> getAirportRoutes(@PathVariable String iataCode) {
        return airportService.findRoutesByAirport(iataCode)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/busiest")
    public List<AirportRouteStatisticResponse> getBusiestAirports() {
        return airportService.busiestAirports()
                .stream()
                .map(AirportRouteStatisticResponse::from)
                .toList();
    }*/

    @GetMapping("/grouped")
    public Map<String, List<AirportResponse>> getGroupedAirports(
            @RequestParam(required = false, defaultValue = "country") String by) {
        return airportService.groupAirportsBy(by)
                .entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(AirportResponse::from)
                                .toList()
                ));
    }

    /**
     * Posts the data of airports from a .csv file
     */
    @PostMapping("/import")
    public void importCsv(@RequestParam("file") MultipartFile file) throws Exception {
        List<Airport> airports = airportCsvService.importFile(file.getInputStream());
        airports.forEach(airportService::execute);
    }

    record AddAirportRequest(String iataCode, String airportType, String name,
                             String status, AirportLocationRequest location,
                             FacilitiesRequest facilities, Double routeDistance,
                             List<RunwayRequest> runways,
                             List<CertificationRequest> certifications,
                             List<ContactRequest> contacts, List<String> airportPhotos) {}

    record AirportLocationRequest(String city, Double longitude,
                                  Double latitude, String region, String timezone, String country) {}

    record FacilitiesRequest(Integer terminalCount, List<String> services,
                             Integer gateCount) {}

    record RunwayRequest(String runwayName, Double length, String orientation,
                         String status) {}

    record CertificationRequest(String name, String category,
                                LocalDate startsAt, LocalDate expiresAt) {}

    record ContactRequest(String type, String value) {}

    record AddCertificationRequest(String name, String category,
                                   LocalDate startsAt, LocalDate expiresAt) {}

    record UpdateAirportStatusRequest(String status) {}

    record UpdateAirportDetailsRequest(String name, String operationalHours,
                                          List<ContactRequest> contacts) {}

    public record AirportResponse(String iataCode, String airportType,
                                  String name, AirportStatus status, AirportLocationResponse location,
                                  FacilitiesResponse facilities, Double routeDistance,
                                  List<RunwayResponse> runways,
                                  List<CertificationResponse> certifications,
                                  List<ContactResponse> contacts, List<String> airportPhotos) {
        static AirportResponse from(Airport airport) {
            return new AirportResponse(
                    airport.getIataCode(),
                    airport.getAirportType(),
                    airport.getName(),
                    airport.getStatus(),
                    AirportLocationResponse.from(airport.getAirportLocation()),
                    FacilitiesResponse.from(airport.getFacilities()),
                    airport.getRouteDistance(),
                    airport.getAirportRunways().stream()
                            .map(RunwayResponse::from)
                            .toList(),
                    airport.getCertifications().stream()
                            .map(CertificationResponse::from)
                            .toList(),
                    airport.getContacts().stream()
                            .map(ContactResponse::from)
                            .toList(),
                    airport.getAirportPhotos()
            );
        }
    }

    record AirportLocationResponse(String city, Double longitude,
                                   Double latitude, String region, String timezone, String country) {
        static AirportLocationResponse from(AirportLocation location) {
            return new AirportLocationResponse(location.getCity(),
                    location.getLongitude(), location.getLatitude(),
                    location.getRegion(), location.getTimezone(),
                    location.getCountry());
        }
    }

    record FacilitiesResponse(Integer terminalCount, List<String> services,
                              Integer gateCount) {
        static FacilitiesResponse from(Facilities facilities) {
            return new FacilitiesResponse(facilities.getTerminalCount(),
                    facilities.getServices(), facilities.getGateCount());
        }
    }

    record RunwayResponse(String runwayName, Double length,
                          String orientation, RunwayStatus status) {
        static RunwayResponse from(Runway runway) {
            return new RunwayResponse(runway.getRunwayName(),
                    runway.getLength(), runway.getOrientation(),
                    runway.getStatus());
        }
    }

    record CertificationResponse(String name, String category,
                                 LocalDate startsAt, LocalDate expiresAt) {
        static CertificationResponse from(Certification certification) {
            return new CertificationResponse(certification.getName(),
                    certification.getCategory(), certification.getStartsAt(),
                    certification.getExpiresAt());
        }
    }

    record ContactResponse(ContactType type, String value) {
        static ContactResponse from(Contact contact) {
            return new ContactResponse(contact.getType(), contact.getValue());
        }
    }
    /*
    public record RouteResponse(Long routeId, String routeName, RouteStatus status,
                               RouteType type, String originIataCode,
                               String destinationIataCode, Double estimatedFlightTimeHours,
                               RouteRequirementsResponse requirements,
                               RouteHistoryResponse history) {
        static RouteResponse from(Route route) {
            return new RouteResponse(
                    route.getRouteId(),
                    route.getRouteName(),
                    route.getStatus(),
                    route.getType(),
                    route.getOriginAirport().getIataCode(),
                    route.getDestinationAirport().getIataCode(),
                    route.getEstimatedFlightTimeHours(),
                    RouteRequirementsResponse.from(route.getRouteRequirements()),
                    RouteHistoryResponse.from(route.getRouteHistory())
            );
        }
    }

    record RouteRequirementsResponse(Double requiredRange, Integer requiredCapacity) {
        static RouteRequirementsResponse from(RouteRequirements requirements) {
            return new RouteRequirementsResponse(
                    requirements.getRequiredRange(),
                    requirements.getRequiredCapacity()
            );
        }
    }

    record RouteHistoryResponse(LocalDate routeBegin,
                               LocalDate routeFinish,
                               Integer routeUsage) {
        static RouteHistoryResponse from(RouteHistory history) {
            return new RouteHistoryResponse(
                    history.getRouteBegin(),
                    history.getRouteFinish(),
                    history.getRouteUsage()
            );
        }
    }

    record AirportRouteStatisticResponse(String iataCode, String name, Long numberOfRoutes) {
        static AirportRouteStatisticResponse from(AirportService.AirportRouteStatistic stat) {
            return new AirportRouteStatisticResponse(
                    stat.airport().getIataCode(),
                    stat.airport().getName(),
                    stat.numberOfRoutes()
            );
        }
    }*/
}