package com.sidis.aircraftservice.WP1A.US104;

import com.sidis.aircraftservice.Aircraft.HangarController;
import com.sidis.aircraftservice.Aircraft.application.AddAircraftUseCase;
import com.sidis.aircraftservice.Aircraft.application.AircraftLifeCycleUpdaterService;
import com.sidis.aircraftservice.Aircraft.application.AircraftSearchService;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.domain.AircraftAvailability;
import com.sidis.aircraftservice.Aircraft.infrastructure.CalculationsService;
import com.sidis.aircraftservice.AircraftCatalog.application.AircraftModelSearchService;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
//import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
//import com.sidis.aircraftservice.Flight.application.FlightSearchService;
//import com.sidis.aircraftservice.Route.application.RouteSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GetAircraftWithFilteringFromRepoTest {

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
    void shouldReturnFilteredAircrafts() {

        AircraftModel model = new AircraftModel(
                "737-800",
                "Boeing",
                new AircraftSpecs(70500.0, 5436.0, 842.0, 189),
                List.of("b737-800-front.jpg", "b737-800-side.jpg", "b737-800-cabin.jpg")
        );

        Aircraft aircraft = new Aircraft(
                "CS-TST",
                model,
                LocalDate.of(2020, 1, 1),
                1000.0,
                800.0
        );

        aircraftLifeCycleUpdaterService.changeAvailability(
                aircraft,
                AircraftAvailability.AVAILABLE
        );

        when(aircraftSearchService.findAircraftsMatchingFilter(
                eq("Boeing 737-800"),
                eq("AVAILABLE"),
                eq(LocalDate.of(2020, 1, 1)),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(aircraft)));

        List<HangarController.AircraftResponse> response =
                hangarController.getAircraftsWithFiltering(
                        "Boeing 737-800",
                        "AVAILABLE",
                        LocalDate.of(2020, 1, 1),
                        0,
                        10
                );

        assertNotNull(response);
        assertEquals(1, response.size());

        HangarController.AircraftResponse aircraftResponse =
                response.get(0);

        assertEquals("CS-TST",
                aircraftResponse.registrationNumber());

        assertEquals("737-800",
                aircraftResponse.modelName());

        assertEquals(AircraftAvailability.AVAILABLE,
                aircraftResponse.status());

        assertEquals(LocalDate.of(2020, 1, 1),
                aircraftResponse.manufacturingDate());

        assertEquals(1000.0,
                aircraftResponse.totalOperationalHours());

        assertEquals(800.0,
                aircraftResponse.totalFlightHours());

        verify(aircraftSearchService, times(1))
                .findAircraftsMatchingFilter(
                        eq("Boeing 737-800"),
                        eq("AVAILABLE"),
                        eq(LocalDate.of(2020, 1, 1)),
                        any(Pageable.class)
                );
    }
}