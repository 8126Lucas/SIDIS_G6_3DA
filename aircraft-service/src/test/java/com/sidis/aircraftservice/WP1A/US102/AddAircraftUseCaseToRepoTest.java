package com.sidis.aircraftservice.WP1A.US102;

import com.sidis.aircraftservice.Aircraft.application.AddAircraftUseCase;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

class AddAircraftUseCaseToRepoTest {
    private AircraftRepository aircraftRepository;
    private AddAircraftUseCase useCase;

    @BeforeEach
    void setUp() {
        aircraftRepository = mock(AircraftRepository.class);
        useCase = new AddAircraftUseCase(aircraftRepository);
    }

    @Test
    void shouldSaveAircraftWhenExecuteWithFields() {
        AircraftModel model = mock(AircraftModel.class);
        String registrationNumber = "CS-TST";
        LocalDate manufacturingDate = LocalDate.of(2020, 1, 1);
        Double totalOperationalHours = 1000.0;
        Double totalFlightHours = 800.0;

        useCase.execute(
                registrationNumber,
                model,
                manufacturingDate,
                totalOperationalHours,
                totalFlightHours
        );

        ArgumentCaptor<Aircraft> captor = ArgumentCaptor.forClass(Aircraft.class);
        verify(aircraftRepository, times(1)).save(captor.capture());

        Aircraft savedAircraft = captor.getValue();

        assertEquals(registrationNumber, savedAircraft.getRegistrationNumber());
        assertEquals(model, savedAircraft.getModel());
        assertEquals(manufacturingDate, savedAircraft.getManufacturingDate());
        assertEquals(totalOperationalHours, savedAircraft.getTotalOperationalHours());
        assertEquals(totalFlightHours, savedAircraft.getTotalFlightHours());
    }

    @Test
    void shouldSaveAircraftWhenExecuteWithAircraftObject() {
        Aircraft aircraft = mock(Aircraft.class);

        useCase.execute(aircraft);

        verify(aircraftRepository, times(1)).save(aircraft);
    }
}