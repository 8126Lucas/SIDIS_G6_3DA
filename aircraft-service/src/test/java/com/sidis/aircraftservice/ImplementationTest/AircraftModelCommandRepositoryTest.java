package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelCommandRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//runtime
class AircraftModelCommandRepositoryTest {
    @Autowired
    private AircraftModelCommandRepository repository;

    @Test
    void shouldSaveAndFindAircraftModel() {
        AircraftSpecs specs = new AircraftSpecs(5000.0, 3000.0, 850.0, 180);
        AircraftModel model = new AircraftModel("A320", "Airbus", specs, List.of("image1.jpg", "image2.jpg"));

        repository.saveAndFlush(model);
        Optional<AircraftModel> result = repository.findByModelName("A320");

        assertTrue(result.isPresent());

        AircraftModel savedModel = result.get();

        assertEquals("A320", savedModel.getModelName());
        assertEquals("Airbus", savedModel.getManufacturer());
        assertEquals(5000.0, savedModel.getAircraftModelSpecs().getFuelCapacity());
        assertEquals(3000.0, savedModel.getAircraftModelSpecs().getMaximumRange());
        assertEquals(850.0, savedModel.getAircraftModelSpecs().getCruisingSpeed());
        assertEquals(180, savedModel.getAircraftModelSpecs().getStandardSeatingCapacity());

        assertEquals(List.of("image1.jpg", "image2.jpg"), savedModel.getModelImage());
    }

    @Test
    void shouldReturnEmptyWhenModelDoesNotExist() {
        Optional<AircraftModel> result = repository.findByModelName("NON");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldUpdateAircraftModelManufacturer() {
        AircraftModel model = new AircraftModel("A320", "Airbus", new AircraftSpecs(5000.0, 3000.0, 850.0, 180), List.of("image1.jpg"));

        repository.saveAndFlush(model);

        model.changeManufacturer("Airbus Updated");
        repository.saveAndFlush(model);

        AircraftModel updatedModel = repository.findByModelName("A320").orElseThrow();
        assertEquals("Airbus Updated", updatedModel.getManufacturer());
    }
}
