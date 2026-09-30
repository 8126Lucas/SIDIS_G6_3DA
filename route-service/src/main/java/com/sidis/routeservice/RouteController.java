package com.sidis.routeservice;

//import com.sidis.routeservice.application.RouteExportService;
import com.sidis.routeservice.application.RouteService;
import com.sidis.routeservice.domain.Route;
import com.sidis.routeservice.domain.RouteHistory;
import com.sidis.routeservice.domain.RouteRequirements;
import com.sidis.routeservice.domain.RouteStatus;
import com.sidis.routeservice.domain.RouteType;
import com.sidis.routeservice.exceptions.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService routeService;
    //private final RouteExportService routeExportService;

    public RouteController(RouteService routeService/*, RouteExportService routeExportService*/) {
        this.routeService = routeService;
        //this.routeExportService = routeExportService;
    }

    /*
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse createRoute(@RequestBody CreateRouteRequest request) {
        if (request.requirements() == null) {
            throw new DomainException("Route requirements are required");
        }
        return RouteResponse.from(routeService.createRoute(
                request.routeName(),
                request.originIataCode(),
                request.destinationIataCode(),
                request.estimatedFlightTimeHours(),
                request.type(),
                request.requirements()
        ));
    }

    @PatchMapping("/{routeId}")
    public RouteResponse updateRoute(@PathVariable Long routeId,
            @RequestBody UpdateRouteRequest request) {
        return RouteResponse.from(routeService.updateRoute(routeId,
                request.destinationIataCode(),
                request.estimatedFlightTimeHours(),
                request.status()));
    }*/

    @GetMapping("/{routeId}/history")
    public RouteHistoryResponse getRouteHistory(@PathVariable Long routeId) {
        return RouteHistoryResponse.from(routeService.getRouteHistory(routeId));
    }

    /*
    @GetMapping("/from/{airportIataCode}")
    public List<RouteResponse> getRoutesFromAirport(@PathVariable String airportIataCode) {
        return routeService.getRoutesFromAirport(airportIataCode)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }*/

    @GetMapping("/{routeId:\\d+}")
    public RouteResponse getRouteDetails(@PathVariable Long routeId) {
        return RouteResponse.from(routeService.getRouteDetails(routeId));
    }

    @GetMapping
    public List<RouteResponse> searchRoutes(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination) {
        return routeService.searchRoutes(origin, destination)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/active")
    public List<RouteResponse> listActiveRoutes(
            @RequestParam(defaultValue = "popularity") String sortBy) {
        return routeService.listActiveRoutes(sortBy)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/total-distance")
    public TotalDistanceResponse calculateTotalDistance() {
        return new TotalDistanceResponse(routeService.calculateTotalRouteDistance());
    }

    /*
    // US228 — exportar rede de rotas em GeoJSON
    @GetMapping("/export/geojson")
    public ResponseEntity<String> exportGeoJson() {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/geo+json"))
                .body(routeExportService.exportAsGeoJson());
    }

    // US228 — exportar rede de rotas em KML
    @GetMapping("/export/kml")
    public ResponseEntity<String> exportKml() {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/vnd.google-earth.kml+xml"))
                .body(routeExportService.exportAsKml());
    }*/

    @GetMapping("/alternatives")
    public List<RouteResponse> searchAlternativeRoutes(
            @RequestParam String origin,
            @RequestParam String destination) {
        return routeService.searchAlternativeRoutes(origin, destination)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    public record TotalDistanceResponse(Double totalDistanceKm) {
    }

    record CreateRouteRequest(String routeName, String originIataCode,
            String destinationIataCode, Double estimatedFlightTimeHours,
            RouteType type, RouteRequirements requirements) {
    }

    record UpdateRouteRequest(String destinationIataCode,
            Double estimatedFlightTimeHours, RouteStatus status) {
    }

    public record RouteResponse(Long routeId, String routeName, RouteStatus status,
            RouteType type, String originIataCode,
            String destinationIataCode, Double estimatedFlightTimeHours,
            Double distanceKm,
            RouteRequirementsResponse routeRequirements,
            RouteHistoryResponse routeHistory) {

        public static RouteResponse from(Route route) {
            return new RouteResponse(route.getRouteId(), route.getRouteName(),
                    route.getStatus(), route.getType(),
                    route.getOriginAirport().getIataCode(),
                    route.getDestinationAirport().getIataCode(),
                    route.getEstimatedFlightTimeHours(), route.getDistanceKm(),
                    RouteRequirementsResponse.from(route.getRouteRequirements()),
                    RouteHistoryResponse.from(route.getRouteHistory()));
        }
    }

    public record RouteRequirementsResponse(Double requiredRange,
            Integer requiredCapacity) {
        public static RouteRequirementsResponse from(RouteRequirements requirements) {
            return new RouteRequirementsResponse(requirements.getRequiredRange(),
                    requirements.getRequiredCapacity());
        }
    }

    public record RouteHistoryResponse(LocalDate routeBegin,
            LocalDate routeFinish, Integer routeUsage) {
        public static RouteHistoryResponse from(RouteHistory history) {
            return new RouteHistoryResponse(history.getRouteBegin(),
                    history.getRouteFinish(), history.getRouteUsage());
        }
    }
}
