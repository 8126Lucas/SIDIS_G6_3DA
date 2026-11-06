package com.sidis.aircraftservice.Aircraft.application;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftCommandRepository;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AircraftSearchService {
    private final AircraftCommandRepository aircraftCommandRepository;
    private final AircraftQueryRepository aircraftQueryRepository;

    public AircraftSearchService(AircraftCommandRepository aircraftCommandRepository,
                                 AircraftQueryRepository aircraftQueryRepository) {
        this.aircraftCommandRepository = aircraftCommandRepository;
        this.aircraftQueryRepository = aircraftQueryRepository;
    }

    public Optional<AircraftRedis> spotAircraftInHangar(String id) {
        return aircraftQueryRepository.findById(id);
    }

    public Page<AircraftRedis> findAircraftsMatchingFilter(String model, String status, LocalDate manufacturingDate, Pageable pageable) {
        List<AircraftRedis> allAircrafts = findAll();

        List<AircraftRedis> filteredAircrafts = allAircrafts.stream()
                .filter(a -> model == null || a.getModelName().equals(model))
                .filter(a -> status == null || a.getStatus().name().equalsIgnoreCase(status))
                .filter(a -> manufacturingDate == null || a.getManufacturingDate().equals(manufacturingDate))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredAircrafts.size());

        List<AircraftRedis> pageContent = (start <= end && start < filteredAircrafts.size())
                ? filteredAircrafts.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(pageContent, pageable, filteredAircrafts.size());
    }
    public List<Map.Entry<String, Double>> findMostUsedModelsByFlyingAircraftHours() {
        return aircraftQueryRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        AircraftRedis::getModelName,
                        Collectors.summingDouble(AircraftRedis::getTotalFlightHours)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();
    }

    public List<AircraftRedis> findAll(){
        return aircraftQueryRepository.findAll();
    }

    public Page<AircraftRedis> findPages(Pageable pageable) {
        return aircraftQueryRepository.findAll(pageable);
    }

    public long countByAvailability(AircraftAvailability availability) {
        return aircraftQueryRepository.countByStatus(availability);
    }

    public List<AircraftRedis> findAircraftsByAvailability(List<AircraftRedis> aircrafts, AircraftAvailability status) {
        List<AircraftRedis> filteredAircrafts = new ArrayList<>();
        for(AircraftRedis aircraft : aircrafts){
            if(aircraft.getStatus() == status){
                filteredAircrafts.add(aircraft);
            }
        }
        return filteredAircrafts;
    }

    public List<AircraftRedis> findAircraftWithFeature(List<String> searchFeatures) {
        List<AircraftRedis> filteredAircrafts = findAll();

        if (searchFeatures == null || searchFeatures.isEmpty()) {
            return null;
        }

        return filteredAircrafts.stream()
                .filter(aircraft -> aircraft.getFeatures() != null &&
                        new HashSet<>(aircraft.getFeatures()).containsAll(searchFeatures))
                .collect(Collectors.toList());
    }
}