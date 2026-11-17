package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftCommandRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//runtime
class AircraftCommandRepositoryTest {
    @Autowired
    private AircraftCommandRepository aircraftCommandRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndFindAircraft() {
        AircraftSpecs specs = new AircraftSpecs(5000.0, 3000.0, 850.0, 180);
        AircraftModel model = new AircraftModel("A320", "Airbus", specs, List.of("image1.jpg"));

        entityManager.persist(model);
        entityManager.flush();

        Aircraft aircraft = new Aircraft("CS-TST", model, LocalDate.of(2020, 5, 10), 1200.0, 800.0);

        aircraftCommandRepository.saveAndFlush(aircraft);
        entityManager.clear();

        Optional<Aircraft> result = aircraftCommandRepository.findById("CS-TST");

        assertTrue(result.isPresent());
        Aircraft savedAircraft = result.get();

        assertEquals("CS-TST", savedAircraft.getRegistrationNumber());
        assertEquals(AircraftAvailability.INACTIVE, savedAircraft.getStatus());
        assertEquals(LocalDate.of(2020, 5, 10),savedAircraft.getManufacturingDate());
        assertEquals(1200.0, savedAircraft.getTotalOperationalHours(), 0.001);
        assertEquals(800.0, savedAircraft.getTotalFlightHours(), 0.001);
    }

    @Test
    void shouldReturnEmptyWhenAircraftDoesNotExist() {
        Optional<Aircraft> result = aircraftCommandRepository.findById("NOT");
        assertTrue(result.isEmpty());
    }
}