package com.sidis.flightservice.Flight.application;

import com.sidis.flightservice.Flight.infrastructure.client.AircraftRestClientService;
import com.sidis.flightservice.Flight.infrastructure.client.aircraft.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AircraftSearchService {
    private final AircraftRestClientService aircraftRepository;

    public AircraftSearchService(AircraftRestClientService aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    public Optional<AircraftClientDTO> spotAircraftInHangar(String id) {
        return aircraftRepository.findAircraftByRegistration(id);
    }

    public Page<AircraftClientDTO> findAircraftsMatchingFilter(String model, String status, LocalDate manufacturingDate, Pageable pageable) {
        Stream<AircraftClientDTO> spec = aircraftRepository.findAll().stream();
        if (model != null) {
            spec = spec.filter(a ->
                    a.modelDTO() != null &&
                            a.modelDTO().modelName().equalsIgnoreCase(model));
        }

        if (status != null) {
            spec = spec.filter(a ->
                    a.status() != null &&
                            a.status().equalsIgnoreCase(status));
        }

        if (manufacturingDate != null) {
            spec = spec.filter(a ->
                    manufacturingDate.equals(a.manufacturingDate()));
        }

        List<AircraftClientDTO> filtered = spec.toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<AircraftClientDTO> pageContent = start >= filtered.size()
                ? Collections.emptyList()
                : filtered.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    public List<Map.Entry<AircraftModelDTO, Double>> findMostUsedModelsByFlyingAircraftHours() {
        return aircraftRepository.findAll().stream()
                .filter(aircraft -> aircraft.modelDTO() != null)
                .filter(aircraft -> aircraft.totalFlightHours() != null)
                .collect(Collectors.groupingBy(
                        AircraftClientDTO::modelDTO,
                        Collectors.summingDouble(AircraftClientDTO::totalFlightHours)
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<AircraftModelDTO, Double>
                        comparingByValue().reversed())
                .toList();
    }

    public List<AircraftClientDTO> findAll(){
        return aircraftRepository.findAll();
    }

    public Page<AircraftClientDTO> findPages(Pageable pageable) {
        List<AircraftClientDTO> aircraft = aircraftRepository.findAll();
        int start = (int) pageable.getOffset();
        if (start >= aircraft.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, aircraft.size());
        }
        int end = Math.min(start + pageable.getPageSize(), aircraft.size());
        List<AircraftClientDTO> pageContent = aircraft.subList(start, end);
        return new PageImpl<>(pageContent, pageable, aircraft.size());
    }

    public long countByAvailability(AircraftAvailability availability) {
        return aircraftRepository.countByStatus(availability);
    }

    public List<AircraftClientDTO> findAircraftsByAvailability(List<AircraftClientDTO> aircrafts,AircraftAvailability status) {
        List<AircraftClientDTO> filteredAircrafts = new ArrayList<>();
        for(AircraftClientDTO aircraft : aircrafts){
            if(Objects.equals(aircraft.status(), status.toString())){
                filteredAircrafts.add(aircraft);
            }
        }
        return filteredAircrafts;
    }

    public List<AircraftClientDTO> findAircraftWithFeature(List<String> searchFeatures) {
        List<AircraftClientDTO> filteredAircrafts = findAll();
        if (searchFeatures == null || searchFeatures.isEmpty()) {
            return null;
        }

        return filteredAircrafts.stream()
                .filter(aircraft -> aircraft.features() != null &&
                        new HashSet<>(aircraft.features()).containsAll(searchFeatures))
                .collect(Collectors.toList());
    }
}