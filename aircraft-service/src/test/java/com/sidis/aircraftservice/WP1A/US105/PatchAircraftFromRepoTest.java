package com.sidis.aircraftservice.WP1A.US105;

import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.AddAircraftUseCase;
import com.sidis.aircraftservice.Aircraft.application.AircraftLifeCycleUpdaterService;
import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.infrastructure.CalculationsService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
//import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
//import com.sidis.aircraftservice.Route.application.RouteSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatchAircraftFromRepoTest {

    private AircraftSearchService aircraftSearchService;
    private AircraftLifeCycleUpdaterService aircraftLifeCycleUpdaterService;
    private HangarController hangarController;

    @BeforeEach
    void setUp() {

        aircraftSearchService = mock(AircraftSearchService.class);

        aircraftLifeCycleUpdaterService =
                new AircraftLifeCycleUpdaterService();

        hangarController = new HangarController(
                mock(AddAircraftUseCase.class),
                mock(AircraftModelSearchService.class),
                aircraftLifeCycleUpdaterService,
                aircraftSearchService/*,
                mock(RouteSearchService.class),
                mock(FlightSearchService.class),
                mock(FuelEfficiencyService.class)*/
        );
    }

    @Test
    void shouldChangeAircraftAvailability() {

        AircraftModel model = mock(AircraftModel.class);
        when(model.getModelName()).thenReturn("Airbus A320");

        Aircraft aircraft = new Aircraft(
                "CS-TST",
                model,
                LocalDate.of(2020, 1, 1),
                1000.0,
                800.0
        );

        when(aircraftSearchService.spotAircraftInHangar("CS-TST"))
                .thenReturn(Optional.of(aircraft));

        hangarController.changeAircraft(
                "CS-TST",
                new HangarController.PatchAircraftAvailabilityRequest(
                        "CS-TST",
                        "AVAILABLE"
                )
        );

        assertEquals(AircraftAvailability.AVAILABLE, aircraft.getStatus());

        verify(aircraftSearchService, times(1))
                .spotAircraftInHangar("CS-TST");
    }
}