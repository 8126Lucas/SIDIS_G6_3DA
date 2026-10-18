package com.sidis.routeservice.Route.application;

import com.sidis.routeservice.Route.infrastructure.client.aircraft.AircraftClientDTO;
import com.sidis.routeservice.Route.domain.Route;
import com.sidis.routeservice.Route.infrastructure.RouteRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class RouteSearchService {
    private final RouteRepository routeRepository;

    public RouteSearchService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<Route> findCompatibleRoutes(AircraftClientDTO aircraft) {
        int capacity;
        if (aircraft.seatingPackDTO() != null && aircraft.seatingPackDTO().seatingCapacity() != null) {
            capacity = aircraft.seatingPackDTO().seatingCapacity();
        } else {
            capacity = aircraft.modelDTO().aircraftModelSpecsDTO().standardSeatingCapacity();
        }
        double range;
        if(aircraft.meanRange() == null) {
            range=aircraft.modelDTO().aircraftModelSpecsDTO().maximumRangeKm();
        }else{
            range= aircraft.meanRange();
        }
        List<Route> compatibleRoutes = new ArrayList<>();
        for (Route route : routeRepository.findAll()) {
            if (route.getRouteRequirements().getRequiredCapacity() <= capacity
                    && route.getRouteRequirements().getRequiredRange() <= range) {
                compatibleRoutes.add(route);
            }
        }
        return compatibleRoutes;
    }

    public List<Route> findAll(){
        return routeRepository.findAll();
    }
}
