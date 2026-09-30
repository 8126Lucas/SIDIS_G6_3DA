package com.sidis.routeservice.application;
/*
import com.example.AISafePSOFT_26.Aircraft.domain.Aircraft;
import com.example.AISafePSOFT_26.Route.domain.Route;
import com.example.AISafePSOFT_26.Route.infrastructure.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteSearchService {
    private final RouteRepository routeRepository;

    public RouteSearchService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<Route> findCompatibleRoutes(Aircraft aircraft) {
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
*/