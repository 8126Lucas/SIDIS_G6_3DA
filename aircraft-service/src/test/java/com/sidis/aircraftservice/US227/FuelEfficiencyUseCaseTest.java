package com.sidis.aircraftservice.US227;

//import com.sidis.aircraftservice.Aircraft.application.FuelEfficiencyService;
import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import com.sidis.aircraftservice.Aircraft.infrastructure.AircraftRepository;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftModel;
import com.sidis.aircraftservice.AircraftCatalog.domain.AircraftSpecs;
//import com.sidis.aircraftservice.Airport.domain.Airport;
//import com.sidis.aircraftservice.Flight.domain.Flight;
//import com.sidis.aircraftservice.Flight.infrastructure.FlightRepository;
//import com.sidis.aircraftservice.Route.domain.Route;
//import com.sidis.aircraftservice.Route.domain.RouteHistory;
//import com.sidis.aircraftservice.Route.domain.RouteRequirements;
//import com.sidis.aircraftservice.Route.domain.RouteStatus;
//import com.sidis.aircraftservice.Route.domain.RouteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FuelEfficiencyUseCaseTest {

    private AircraftRepository aircraftRepository;
//    private FlightRepository flightRepository;
//    private FuelEfficiencyService service;

    private AircraftModel model;
    private Aircraft aircraft;

/*    @BeforeEach
    void setUp() {
        aircraftRepository = mock(AircraftRepository.class);
        flightRepository = mock(FlightRepository.class);
        service = new FuelEfficiencyService(aircraftRepository, flightRepository);

        AircraftSpecs specs = new AircraftSpecs(20000.0, 5000.0, 900.0, 180);
        model = new AircraftModel("Boeing 737", "Boeing", specs, List.of());
        aircraft = new Aircraft("CS-TVA", model, LocalDate.of(2018, 1, 1), 0.0, 100.0);
    }*/

/*    @Test
    void shouldReturnEmptyWhenNoAircraft() {
        when(aircraftRepository.findAll()).thenReturn(List.of());

        List<FuelEfficiencyService.AircraftFuelEfficiency> result = service.getFuelEfficiencyPerAircraft();

        assertTrue(result.isEmpty());
    }*/

/*    @Test
    void shouldCalculateFuelConsumptionRatePerAircraft() {
        when(aircraftRepository.findAll()).thenReturn(List.of(aircraft));

        List<FuelEfficiencyService.AircraftFuelEfficiency> result = service.getFuelEfficiencyPerAircraft();

        assertEquals(1, result.size());
        assertEquals("CS-TVA", result.get(0).registrationNumber());
        assertEquals("Boeing 737", result.get(0).modelName());
        assertEquals(20000.0, result.get(0).fuelCapacityLiters());
        assertEquals(5000.0, result.get(0).maxRangeKm());
        assertEquals(4.0, result.get(0).fuelConsumptionLitPerKm());
    }*/

/*    @Test
    void shouldReturnEmptyRouteEfficiencyWhenNoFlights() {
        when(flightRepository.findAll()).thenReturn(List.of());

        List<FuelEfficiencyService.RouteFuelEfficiency> result = service.getFuelEfficiencyPerRoute();

        assertTrue(result.isEmpty());
    }*/

/*    @Test
    void shouldCalculateRouteEfficiencyFromFlights() {
        Airport origin = mock(Airport.class);
        Airport destination = mock(Airport.class);
        when(origin.getIataCode()).thenReturn("LIS");
        when(destination.getIataCode()).thenReturn("OPO");

        Route route = new Route(
                new RouteRequirements(1000.0, 150),
                RouteStatus.ACTIVE,
                RouteType.DIRECT,
                new RouteHistory(),
                1.5,
                300.0,
                origin,
                destination,
                "LIS-OPO"
        );

        Flight flight = new Flight(route, aircraft,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));

        when(flightRepository.findAll()).thenReturn(List.of(flight));

        List<FuelEfficiencyService.RouteFuelEfficiency> result = service.getFuelEfficiencyPerRoute();

        assertEquals(1, result.size());
        assertEquals("LIS-OPO", result.get(0).routeName());
        assertEquals("LIS", result.get(0).originIata());
        assertEquals("OPO", result.get(0).destinationIata());
        assertEquals(300.0, result.get(0).distanceKm());
        assertEquals(4.0, result.get(0).avgFuelConsumptionLitPerKm());
        assertEquals(1200.0, result.get(0).estimatedFuelLiters());
    }*/

/*    @Test
    void shouldHandleAircraftWithNullSpecs() {
        AircraftModel modelWithNullSpecs = new AircraftModel("TestModel", "TestMfr", null, List.of());
        Aircraft aircraftNullSpecs = new Aircraft("CS-NULL", modelWithNullSpecs,
                LocalDate.of(2020, 1, 1), 0.0, 0.0);

        when(aircraftRepository.findAll()).thenReturn(List.of(aircraftNullSpecs));

        List<FuelEfficiencyService.AircraftFuelEfficiency> result = service.getFuelEfficiencyPerAircraft();

        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).fuelConsumptionLitPerKm());
    }*/

/*    @Test
    void shouldSortAircraftByFuelConsumptionAscending() {
        AircraftSpecs economicalSpecs = new AircraftSpecs(10000.0, 5000.0, 800.0, 150);
        AircraftModel economicalModel = new AircraftModel("EcoJet", "EcoCorp", economicalSpecs, List.of());
        Aircraft economicalAircraft = new Aircraft("CS-ECO", economicalModel,
                LocalDate.of(2020, 1, 1), 0.0, 0.0);

        when(aircraftRepository.findAll()).thenReturn(List.of(aircraft, economicalAircraft));

        List<FuelEfficiencyService.AircraftFuelEfficiency> result = service.getFuelEfficiencyPerAircraft();

        assertEquals(2, result.size());
        assertTrue(result.get(0).fuelConsumptionLitPerKm() <= result.get(1).fuelConsumptionLitPerKm());
    }*/
}
