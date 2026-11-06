package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.infrastructure.client.RouteClientDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteSearchService {
    private final AircraftModelSearchService aircraftModelSearchService;

    public RouteSearchService(AircraftModelSearchService aircraftModelSearchService) {
        this.aircraftModelSearchService = aircraftModelSearchService;
    }

    public List<RouteClientDTO> findCompatibleRoutes(AircraftRedis aircraft, List<RouteClientDTO> routes) {
        AircraftModel model = aircraftModelSearchService.spotAircraftInCatalog(aircraft.getModelName())
                .orElseThrow(() -> new IllegalStateException("Model not found in catalog"));

        int capacity;
        if (aircraft.getSeatingPack() != null && aircraft.getSeatingPack().getSeatingCapacity() != null) {
            capacity = aircraft.getSeatingPack().getSeatingCapacity();
        } else {
            capacity = model.getAircraftModelSpecs().getStandardSeatingCapacity();
        }

        double range = (aircraft.getMeanRange() != null && aircraft.getMeanRange() > 0)
                ? aircraft.getMeanRange()
                : model.getAircraftModelSpecs().getMaximumRange();

        List<RouteClientDTO> compatibleRoutes = new ArrayList<>();
        for (RouteClientDTO route : routes) {
            if (route.capacity() <= capacity && route.distanceKm() <= range) {
                compatibleRoutes.add(route);
            }
        }
        return compatibleRoutes;
    }
}