package com.sidis.aircraftservice.AircraftCatalog.application;

import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftCommandRepository;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModelRedis;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelCommandRepository;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelQueryRepository;
import com.sidis.aircraftservice.exceptions.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AircraftModelUpdater {
    private final AircraftModelCommandRepository aircraftModelCommandRepository;
    private final AircraftModelQueryRepository aircraftModelQueryRepository;

    public AircraftModelUpdater(AircraftModelCommandRepository aircraftModelCommandRepository,
                                AircraftModelQueryRepository aircraftModelQueryRepository) {
        this.aircraftModelCommandRepository = aircraftModelCommandRepository;
        this.aircraftModelQueryRepository = aircraftModelQueryRepository;
    }

    @Transactional
    public void aircraftModelUpdate(String modelName, List<String> modelImage,
                                    String manufacturer, Double fuelCapacityLiters, Double maximumRangeKm,
                                    Double cruisingSpeedKph, Integer standardSeatingCapacity) {
        AircraftModel model = aircraftModelCommandRepository.findByModelName(modelName)
                .orElseThrow(() -> new ResourceNotFoundException("Model does not exist in catalog"));

        if(modelImage != null){
            for (String image : modelImage) {
                if (!model.getModelImage().contains(image)) {
                    model.getModelImage().add(image);
                }
            }
        }

        if(manufacturer != null){
            model.changeManufacturer(manufacturer);
        }


        if (fuelCapacityLiters != null && model.getAircraftModelSpecs() != null) {
            model.getAircraftModelSpecs().changeFuelCapacityLiters(fuelCapacityLiters);
        }

        if (maximumRangeKm != null && model.getAircraftModelSpecs() != null) {
            model.getAircraftModelSpecs().changeMaximumRangeKm(maximumRangeKm);
        }

        if (cruisingSpeedKph != null && model.getAircraftModelSpecs() != null) {
            model.getAircraftModelSpecs().changeCruisingSpeedKph(cruisingSpeedKph);
        }

        if (standardSeatingCapacity != null && model.getAircraftModelSpecs() != null) {
            model.getAircraftModelSpecs().changeStandardSeatingCapacity(standardSeatingCapacity);
        }

        aircraftModelCommandRepository.save(model);
    }
}
