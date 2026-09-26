package com.sidis.aircraftservice.AircraftCatalog.application;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class AircraftModelUpdater {
    public void aircraftModelUpdate(AircraftModel model, List<String> modelImage,
                                    String manufacturer, Double fuelCapacityLiters, Double maximumRangeKm,
                                    Double cruisingSpeedKph, Integer standardSeatingCapacity) {
        if(modelImage!=null){
            for (String image : modelImage) {
                if (!model.getModelImage().contains(image)) {
                    model.getModelImage().add(image);
                }
            }
        }

        if(manufacturer!=null){
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
    }
}
