package com.sidis.aircraftservice.infrastructure.messaging;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecsRedis;

import java.util.List;

public record AircraftModelSyncEvent (
        String modelName,
        List<String> modelImage,
        String manufacturer,
        AircraftSpecsRedis aircraftModelSpecs
) {}
