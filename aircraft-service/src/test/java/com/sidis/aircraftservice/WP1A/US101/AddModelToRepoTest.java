package com.sidis.aircraftservice.WP1A.US101;

import com.sidis.aircraftservice.AircraftCatalog.application.AddModelUseCase;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.aircraftservice.AircraftCatalog.infrastructure.AircraftModelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddModelToRepoTest {
    @Mock
    AircraftModelRepository aircraftModelRepository;

    AddModelUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AddModelUseCase(aircraftModelRepository);
    }

    @Test
    void savesExistingAircraftModelObject() {
        AircraftModel model = new AircraftModel(
                "Boeing 777",
                "Boeing",
                new AircraftSpecs(181000.0, 15600.0, 905.0, 396),
                List.of("https://example.com/777.jpg")
        );
        useCase.execute(model);
        verify(aircraftModelRepository).save(model);
    }

    @Test
    void savesAircraftModel() {
        AircraftSpecs specs = new AircraftSpecs(
                24210.0,
                6300.0,
                828.0,
                180
        );

        useCase.execute(
                "Airbus A320",
                "Airbus",
                List.of("https://example.com/a320.jpg"),
                specs
        );

        ArgumentCaptor<AircraftModel> saved =
                ArgumentCaptor.forClass(AircraftModel.class);

        verify(aircraftModelRepository).save(saved.capture());
        AircraftModel captured = saved.getValue();
        assertThat(captured.getModelName())
                .isEqualTo("Airbus A320");

        assertThat(captured.getManufacturer())
                .isEqualTo("Airbus");

        assertThat(captured.getModelImage())
                .hasSize(1);

        assertThat(captured.getAircraftModelSpecs().getFuelCapacity())
                .isEqualTo(24210.0);

        assertThat(captured.getAircraftModelSpecs().getMaximumRange())
                .isEqualTo(6300.0);

        assertThat(captured.getAircraftModelSpecs().getCruisingSpeed())
                .isEqualTo(828.0);

        assertThat(captured.getAircraftModelSpecs().getStandardSeatingCapacity())
                .isEqualTo(180);
    }
}