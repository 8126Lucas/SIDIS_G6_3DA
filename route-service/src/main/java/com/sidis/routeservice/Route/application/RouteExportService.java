package com.sidis.routeservice.Route.application;

import com.sidis.routeservice.Route.domain.RouteReadModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RouteExportService {
    private final RouteQueryService routeQueryService;

    public RouteExportService(RouteQueryService routeQueryService) {
        this.routeQueryService = routeQueryService;
    }

    public String exportAsGeoJson() {
        List<RouteReadModel> routes = routeQueryService.findAll();
        String features = routes.stream()
                .map(this::routeToGeoJsonFeature)
                .collect(Collectors.joining(",\n    "));

        return "{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n    " + features + "\n  ]\n" +
                "}";
    }

    public String exportAsKml() {
        List<RouteReadModel> routes = routeQueryService.findAll();
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

    private String routeToGeoJsonFeature(RouteReadModel route) {
        RouteReadModel.Location origin = route.originAirport().location();
        RouteReadModel.Location destination = route.destinationAirport().location();

        String originCoords = formatCoords(origin);
        String destCoords = formatCoords(destination);

        return "{\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"LineString\",\n" +
                "        \"coordinates\": [" + originCoords + ", " + destCoords + "]\n" +
                "      },\n" +
                "      \"properties\": {\n" +
                "        \"routeId\": " + route.routeId() + ",\n" +
                "        \"routeName\": \"" + route.routeName() + "\",\n" +
                "        \"status\": \"" + route.status() + "\",\n" +
                "        \"type\": \"" + route.type() + "\",\n" +
                "        \"distanceKm\": " + route.distanceKm() + ",\n" +
                "        \"origin\": \"" + route.originAirport().iataCode() + "\",\n" +
                "        \"destination\": \"" + route.destinationAirport().iataCode() + "\"\n" +
                "      }\n" +
                "    }";
    }

    private String routeToKmlPlacemark(RouteReadModel route) {
        RouteReadModel.Location origin = route.originAirport().location();
        RouteReadModel.Location destination = route.destinationAirport().location();

        String coordLine = origin.longitude() + "," + origin.latitude() + "\n        " +
                destination.longitude() + "," + destination.latitude();

        return "<Placemark>\n" +
                "      <name><![CDATA[" + route.routeName() + "]]></name>\n" +
                "      <description><![CDATA[" + route.status() + " | " + route.distanceKm() + " km]]></description>\n" +
                "      <LineString>\n" +
                "        <coordinates>" + coordLine + "</coordinates>\n" +
                "      </LineString>\n" +
                "    </Placemark>\n";
    }

    private String formatCoords(RouteReadModel.Location location) {
        if (location == null) {
            throw new IllegalStateException(
                    "Cannot export route: airport location is missing"
            );
        }
        if (location.longitude() == null || location.latitude() == null) {
            throw new IllegalStateException(
                    "Cannot export route: airport coordinates are missing"
            );
        }
        return "[" + location.longitude() + ", "+ location.latitude() + "]";
    }
}
