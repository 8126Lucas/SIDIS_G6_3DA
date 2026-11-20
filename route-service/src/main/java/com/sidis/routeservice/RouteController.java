package com.sidis.routeservice;

import com.sidis.routeservice.Route.application.RouteExportService;
import com.sidis.routeservice.Route.application.RouteQueryService;
import com.sidis.routeservice.Route.application.RouteService;
import com.sidis.routeservice.Route.domain.Route;
import com.sidis.routeservice.Route.domain.RouteHistory;
import com.sidis.routeservice.Route.domain.RouteReadModel;
import com.sidis.routeservice.Route.domain.RouteRequirements;
import com.sidis.routeservice.Route.domain.RouteStatus;
import com.sidis.routeservice.Route.domain.RouteType;
import com.sidis.routeservice.exceptions.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {
    private final RouteService routeService;
    private final RouteQueryService routeQueryService;
    private final RouteExportService routeExportService;

    public RouteController(RouteService routeService,
                           RouteQueryService routeQueryService,
                           RouteExportService routeExportService) {
        this.routeService = routeService;
        this.routeQueryService = routeQueryService;
        this.routeExportService = routeExportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ATCC')")//US110
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
    @PreAuthorize("hasAnyRole('ATCC', 'BACKOFFICE')")//US112
    public RouteResponse updateRoute(@PathVariable Long routeId,@RequestBody UpdateRouteRequest request) {
        if (request == null
                || (request.destinationIataCode() == null
                && request.estimatedFlightTimeHours() == null
                && request.status() == null
                && request.history() == null)) {

            throw new IllegalArgumentException(
                    "At least one field must be provided for update"
            );
        }
        return RouteResponse.from(routeService.updateRoute(routeId,
                request.destinationIataCode(),
                request.estimatedFlightTimeHours(),
                request.status(),
                request.history()));
    }

    @GetMapping("/{routeId}/history")
    @PreAuthorize("hasRole('ATCC')")//US111
    public RouteHistoryResponse getRouteHistory(@PathVariable Long routeId) {
        return RouteHistoryResponse.from(routeQueryService.getRouteHistory(routeId));
    }


    @GetMapping("/from/{airportIataCode}")
    @PreAuthorize("hasRole('ATCC')")//US113
    public List<RouteResponse> getRoutesFromAirport(@PathVariable String airportIataCode) {
        return routeQueryService.getRoutesFromAirport(airportIataCode)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/{routeId:\\d+}")
    @PreAuthorize("hasRole('ATCC')")//US113
    public RouteResponse getRouteDetails(@PathVariable Long routeId) {
        return RouteResponse.from(routeQueryService.getRouteDetails(routeId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ATCC')")//US114
    public List<RouteResponse> searchRoutes(@RequestParam(required = false) String origin,@RequestParam(required = false) String destination) {
        return routeQueryService.searchRoutes(origin, destination)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ATCC')")//US214
    public List<RouteResponse> listActiveRoutes(@RequestParam(defaultValue = "popularity") String sortBy) {
        return routeQueryService.listActiveRoutes(sortBy)
                .stream()
                .map(RouteResponse::from)
                .toList();
    }

    @GetMapping("/total-distance")
    @PreAuthorize("hasRole('ATCC')")//US215
    public TotalDistanceResponse calculateTotalDistance() {
        return new TotalDistanceResponse(routeQueryService.calculateTotalRouteDistance());
    }


    // US228 — exportar rede de rotas em GeoJSON
    @GetMapping("/export/geojson")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public ResponseEntity<String> exportGeoJson() {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/geo+json"))
                .body(routeExportService.exportAsGeoJson());
    }

    // US228 — exportar rede de rotas em KML
    @GetMapping("/export/kml")
    @PreAuthorize("hasRole('BACKOFFICE')")
    public ResponseEntity<String> exportKml() {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/vnd.google-earth.kml+xml"))
                .body(routeExportService.exportAsKml());
    }

    @GetMapping("/alternatives")
    @PreAuthorize("hasRole('ATCC')")//US216
    public List<RouteResponse> searchAlternativeRoutes(@RequestParam String origin,@RequestParam String destination) {
        return routeQueryService.searchAlternativeRoutes(origin, destination)
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
            Double estimatedFlightTimeHours, RouteStatus status, RouteHistory history) {
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

        public static RouteResponse from(RouteReadModel route) {
            return new RouteResponse(
                    route.routeId(), route.routeName(), route.status(), route.type(),
                    route.originAirport() == null ? null : route.originAirport().iataCode(),
                    route.destinationAirport() == null ? null : route.destinationAirport().iataCode(),
                    route.estimatedFlightTimeHours(), route.distanceKm(),
                    RouteRequirementsResponse.from(route.routeRequirements()),
                    RouteHistoryResponse.from(route.routeHistory())
            );
        }
    }

    public record RouteRequirementsResponse(Double requiredRange,
            Integer requiredCapacity) {
        public static RouteRequirementsResponse from(RouteRequirements requirements) {
            return new RouteRequirementsResponse(requirements.getRequiredRange(),
                    requirements.getRequiredCapacity());
        }

        public static RouteRequirementsResponse from(RouteReadModel.RequirementsData requirements) {
            return requirements == null ? null : new RouteRequirementsResponse(
                    requirements.requiredRange(), requirements.requiredCapacity());
        }
    }

    public record RouteHistoryResponse(LocalDate routeBegin,
            LocalDate routeFinish, Integer routeUsage) {
        public static RouteHistoryResponse from(RouteHistory history) {
            return new RouteHistoryResponse(history.getRouteBegin(),
                    history.getRouteFinish(), history.getRouteUsage());
        }

        public static RouteHistoryResponse from(RouteReadModel.HistoryData history) {
            return history == null ? null : new RouteHistoryResponse(
                    history.routeBegin(), history.routeFinish(), history.routeUsage());
        }
    }
}
