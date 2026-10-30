package com.sidis.routeservice.Route.application;

import com.sidis.routeservice.Route.domain.AirportLocation;
import com.sidis.routeservice.Route.domain.Route;
import com.sidis.routeservice.Route.infrastructure.RouteRepository;
import com.sidis.routeservice.UseCase;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
public class RouteExportService {
    private final RouteRepository routeRepository;

    public RouteExportService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public String exportAsGeoJson() {
        List<Route> routes = routeRepository.findAll();
        String features = routes.stream()
                .map(this::routeToGeoJsonFeature)
                .collect(Collectors.joining(",\n    "));

        return "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n    " + features + "\n  ]\n" +
                "}";
    }

    public String exportAsKml() {
        List<Route> routes = routeRepository.findAll();
        String placemarks = routes.stream()
                .map(this::routeToKmlPlacemark)
                .collect(Collectors.joining("\n    "));

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                "  <Document>\n" +
                "    <name>Route Network</name>\n" +
                "    " + placemarks + "\n" +
                "  </Document>\n" +
                "</kml>";
    }

    private String routeToGeoJsonFeature(Route route) {
        AirportLocation origin = route.getOriginAirport().getAirportLocation();
        AirportLocation destination = route.getDestinationAirport().getAirportLocation();

        String originCoords = formatCoords(origin);
        String destCoords = formatCoords(destination);

        return "{\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"LineString\",\n" +
                "        \"coordinates\": [" + originCoords + ", " + destCoords + "]\n" +
                "      },\n" +
                "      \"properties\": {\n" +
                "        \"routeId\": " + route.getRouteId() + ",\n" +
                "        \"routeName\": \"" + route.getRouteName() + "\",\n" +
                "        \"status\": \"" + route.getStatus() + "\",\n" +
                "        \"type\": \"" + route.getType() + "\",\n" +
                "        \"distanceKm\": " + route.getDistanceKm() + ",\n" +
                "        \"origin\": \"" + route.getOriginAirport().getIataCode() + "\",\n" +
                "        \"destination\": \"" + route.getDestinationAirport().getIataCode() + "\"\n" +
                "      }\n" +
                "    }";
    }

    private String routeToKmlPlacemark(Route route) {
        AirportLocation origin = route.getOriginAirport().getAirportLocation();
        AirportLocation destination = route.getDestinationAirport().getAirportLocation();

        String coordLine = origin.getLongitude() + "," + origin.getLatitude() + " " +
                destination.getLongitude() + "," + destination.getLatitude();

        return "<Placemark>\n" +
                "      <name>" + route.getRouteName() + "</name>\n" +
                "      <description>" + route.getStatus() + " | " + route.getDistanceKm() + " km</description>\n" +
                "      <LineString>\n" +
                "        <coordinates>" + coordLine + "</coordinates>\n" +
                "      </LineString>\n" +
                "    </Placemark>";
    }

    private String formatCoords(AirportLocation location) {
        if (location == null) {
            throw new IllegalStateException(
                    "Cannot export route: airport location is missing"
            );
        }
        if (location.getLongitude() == null || location.getLatitude() == null) {
            throw new IllegalStateException(
                    "Cannot export route: airport coordinates are missing"
            );
        }
        return "[" + location.getLongitude() + ", "+ location.getLatitude() + "]";
    }
}
