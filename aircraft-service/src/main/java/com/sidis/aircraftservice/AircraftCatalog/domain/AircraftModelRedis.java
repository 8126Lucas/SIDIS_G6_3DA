package com.sidis.aircraftservice.AircraftCatalog.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.ArrayList;
import java.util.List;

@RedisHash("AircraftModel")
public class AircraftModelRedis {
    @Id
    private String modelName;
    private List<String> modelImage;
    private String manufacturer;
    private AircraftSpecsRedis aircraftModelSpecs;

    protected AircraftModelRedis() {}

    public AircraftModelRedis(String modelName, String manufacturer,
                              AircraftSpecsRedis aircraftModelSpecs,
                              List<String> modelImage) {
        this.modelName = modelName;
        this.manufacturer = manufacturer;
        this.aircraftModelSpecs = aircraftModelSpecs;
        this.modelImage = modelImage;
    }

    public String getModelName() {
        return modelName;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public AircraftSpecsRedis getAircraftModelSpecs() {
        return aircraftModelSpecs;
    }

    public List<String> getModelImage() {
        return modelImage;
    }

    public void changeModelName(String modelName) { this.modelName = modelName; }
    public void changeManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    public void changeAircraftModelSpecs(AircraftSpecsRedis aircraftModelSpecs) { this.aircraftModelSpecs = aircraftModelSpecs; }
    public void changeModelImage(List<String> modelImage) { this.modelImage = modelImage; }
}