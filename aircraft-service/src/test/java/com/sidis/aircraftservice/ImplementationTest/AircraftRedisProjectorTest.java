package com.sidis.aircraftservice.ImplementationTest;

import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.domain.AircraftRedis;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftQueryRepository;
import com.sidis.aircraftservice.infrastructure.messaging.AircraftRedisProjector;
import com.sidis.aircraftservice.infrastructure.messaging.AircraftSyncEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AircraftRedisProjectorTest {

    @Mock
    private AircraftQueryRepository aircraftQueryRepository;

    @InjectMocks
    private AircraftRedisProjector projector;

    @Test
    void shouldUpdateExistingAircraftInRedis() {
        String registration = "CS-TST";
        AircraftRedis existingAircraft = new AircraftRedis(registration, "OLD-MODEL",
                AircraftAvailability.INACTIVE, LocalDate.of(2019, 1, 1), 100.0,
                80.0, 2000.0, List.of("OLD-FEATURE"), null,
                null, null);

        AircraftSyncEvent event = new AircraftSyncEvent(registration, "A320",
                AircraftAvailability.AVAILABLE.name(), LocalDate.of(2020, 5, 10),
                1500.0, 1200.0, 3000.0, List.of("GPS", "AUTOPILOT"));

        when(aircraftQueryRepository.findById(registration)).thenReturn(Optional.of(existingAircraft));
        projector.syncAircraftToRedis(event);

        assertThat(existingAircraft.getRegistrationNumber()).isEqualTo(registration);
        assertThat(existingAircraft.getModelName()).isEqualTo("A320");
        assertThat(existingAircraft.getStatus()).isEqualTo(AircraftAvailability.AVAILABLE);
        assertThat(existingAircraft.getManufacturingDate()).isEqualTo(LocalDate.of(2020, 5, 10));
        assertThat(existingAircraft.getTotalOperationalHours()).isEqualTo(1500.0);
        assertThat(existingAircraft.getTotalFlightHours()).isEqualTo(1200.0);
        assertThat(existingAircraft.getMeanRange()).isEqualTo(3000.0);
        assertThat(existingAircraft.getFeatures()).containsExactly("GPS", "AUTOPILOT");

        verify(aircraftQueryRepository).findById(registration);
        verify(aircraftQueryRepository).save(existingAircraft);
    }

    @Test
    void shouldCreateNewAircraftWhenItDoesNotExistInRedis() {
        String registration = "CS-NEW";
        AircraftSyncEvent event = new AircraftSyncEvent(registration, "A320", AircraftAvailability.AVAILABLE.name(),
                LocalDate.of(2022, 1, 15), 100.0, 80.0,
                2500.0, List.of("GPS"));

        when(aircraftQueryRepository.findById(registration)).thenReturn(Optional.empty());

        projector.syncAircraftToRedis(event);

        ArgumentCaptor<AircraftRedis> captor = ArgumentCaptor.forClass(AircraftRedis.class);
        verify(aircraftQueryRepository).save(captor.capture());
        AircraftRedis savedAircraft = captor.getValue();

        assertThat(savedAircraft.getRegistrationNumber()).isEqualTo(registration);
        assertThat(savedAircraft.getModelName()).isEqualTo("A320");
        assertThat(savedAircraft.getStatus()).isEqualTo(AircraftAvailability.AVAILABLE);
        assertThat(savedAircraft.getManufacturingDate()).isEqualTo(LocalDate.of(2022, 1, 15));
        assertThat(savedAircraft.getTotalOperationalHours()).isEqualTo(100.0);
        assertThat(savedAircraft.getTotalFlightHours()).isEqualTo(80.0);
        assertThat(savedAircraft.getMeanRange()).isEqualTo(2500.0);
        assertThat(savedAircraft.getFeatures()).containsExactly("GPS");
    }

    @Test
    void shouldNotSaveAircraftWhenStatusIsInvalid() {
        AircraftSyncEvent event = new AircraftSyncEvent("CS-ERR", "A320",
                "INVALID_STATUS", LocalDate.of(2022, 1, 15),
                100.0, 80.0, 2500.0, List.of("GPS"));

        when(aircraftQueryRepository.findById("CS-ERR")).thenReturn(Optional.empty());
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> projector.syncAircraftToRedis(event));
        verify(aircraftQueryRepository, never()).save(any());
    }
}