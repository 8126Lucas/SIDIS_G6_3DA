package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.infrastructure.client.RouteClientDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteSearchService {
    public List<RouteClientDTO> findCompatibleRoutes(Aircraft aircraft, List<RouteClientDTO> routes) {
        int capacity;

        if (aircraft.getSeatingPack() != null
                && aircraft.getSeatingPack().getSeatingCapacity() != null) {
            capacity = aircraft.getSeatingPack().getSeatingCapacity();
        } else {
            capacity = aircraft.getModel()
                    .getAircraftModelSpecs()
                    .getStandardSeatingCapacity();
        }
        double range;
        if(aircraft.getMeanRange() == null) {
            range= aircraft.getModel()
                    .getAircraftModelSpecs()
                    .getMaximumRange();
        }
        range= aircraft.getMeanRange();

        List<RouteClientDTO> compatibleRoutes = new ArrayList<>();
        for (RouteClientDTO route : routes) {
            if (route.capacity() <= capacity
                    && route.distanceKm() <= range) {
                compatibleRoutes.add(route);
            }
        }
        return compatibleRoutes;
    }
}
