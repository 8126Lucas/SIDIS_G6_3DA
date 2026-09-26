package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AircraftSearchService {
    private final AircraftRepository aircraftRepository;

    public AircraftSearchService(AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    public Optional<Aircraft> spotAircraftInHangar(String id) {
        return aircraftRepository.findById(id);
    }

    public Page<Aircraft> findAircraftsMatchingFilter(String model,String status,LocalDate manufacturingDate,Pageable pageable) {
        Specification<Aircraft> spec =
                (root, query, cb) -> cb.conjunction();

        if (model != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("model").get("modelName"), model));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            root.get("status"),
                            AircraftAvailability.valueOf(status.toUpperCase())
                    ));
        }

        if (manufacturingDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("manufacturingDate"), manufacturingDate));
        }

        return aircraftRepository.findAll(spec, pageable);
    }

    public List<Map.Entry<AircraftModel, Double>> findMostUsedModelsByFlyingAircraftHours() {
        return aircraftRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        Aircraft::getModel,
                        Collectors.summingDouble(Aircraft::getTotalFlightHours)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<AircraftModel, Double>comparingByValue().reversed())
                .toList();
    }

    public List<Aircraft> findAll(){
        return aircraftRepository.findAll();
    }

    public Page<Aircraft> findPages(Pageable pageable) {
        return aircraftRepository.findAll(pageable);
    }

    public long countByAvailability(AircraftAvailability availability) {
        return aircraftRepository.countByStatus(availability);
    }

    public List<Aircraft> findAircraftsByAvailability(List<Aircraft> aircrafts,AircraftAvailability status) {
        List<Aircraft> filteredAircrafts = new ArrayList<>();
        for(Aircraft aircraft : aircrafts){
            if(aircraft.getStatus() == status){
                filteredAircrafts.add(aircraft);
            }
        }
        return filteredAircrafts;
    }

    public List<Aircraft> findAircraftWithFeature(List<String> searchFeatures) {
        List<Aircraft> filteredAircrafts = findAll();

        if (searchFeatures == null || searchFeatures.isEmpty()) {
            return null;
        }

        return filteredAircrafts.stream()
                .filter(aircraft -> aircraft.getFeatures() != null &&
                        new HashSet<>(aircraft.getFeatures()).containsAll(searchFeatures))
                .collect(Collectors.toList());
    }
}