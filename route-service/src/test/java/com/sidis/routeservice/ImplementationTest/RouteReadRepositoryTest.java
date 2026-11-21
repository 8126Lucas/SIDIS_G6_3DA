package com.sidis.routeservice.ImplementationTest;

import com.sidis.routeservice.Route.domain.RouteReadModel;
import com.sidis.routeservice.Route.domain.RouteStatus;
import com.sidis.routeservice.Route.infrastructure.RouteReadRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteReadRepositoryTest {
    @Mock
    private RouteReadRepository repository;

    @Test
    void shouldFindRouteById() {
        RouteReadModel route = mock(RouteReadModel.class);
        when(repository.findById(1L)).thenReturn(Optional.of(route));
        Optional<RouteReadModel> result = repository.findById(1L);
        assertTrue(result.isPresent());
        assertSame(route, result.get());
        verify(repository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenRouteDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        Optional<RouteReadModel> result = repository.findById(99L);
        assertTrue(result.isEmpty());
        verify(repository).findById(99L);
    }

    @Test
    void shouldFindRoutesByStatus() {
        RouteReadModel route1 = mock(RouteReadModel.class);
        RouteReadModel route2 = mock(RouteReadModel.class);
        List<RouteReadModel> routes = List.of(route1, route2);
        when(repository.findByStatus(RouteStatus.ACTIVE)).thenReturn(routes);
        List<RouteReadModel> result = repository.findByStatus(RouteStatus.ACTIVE);
        assertEquals(2, result.size());
        assertSame(route1, result.get(0));
        assertSame(route2, result.get(1));
        verify(repository).findByStatus(RouteStatus.ACTIVE);
    }

    @Test
    void shouldReturnEmptyWhenNoRoutesHaveStatus() {
        when(repository.findByStatus(RouteStatus.INACTIVE)).thenReturn(List.of());
        List<RouteReadModel> result = repository.findByStatus(RouteStatus.INACTIVE);
        assertTrue(result.isEmpty());
        verify(repository).findByStatus(RouteStatus.INACTIVE);
    }

    @Test
    void shouldFindRoutesByOriginAirportIataCode() {
        RouteReadModel route = mock(RouteReadModel.class);
        when(repository.findByOriginAirport_IataCode("LIS")).thenReturn(List.of(route));
        List<RouteReadModel> result = repository.findByOriginAirport_IataCode("LIS");
        assertEquals(1, result.size());
        assertSame(route, result.get(0));
        verify(repository).findByOriginAirport_IataCode("LIS");
    }

    @Test
    void shouldReturnEmptyWhenOriginAirportDoesNotExist() {
        when(repository.findByOriginAirport_IataCode("XXX")).thenReturn(List.of());
        List<RouteReadModel> result = repository.findByOriginAirport_IataCode("XXX");
        assertTrue(result.isEmpty());
        verify(repository).findByOriginAirport_IataCode("XXX");
    }

    @Test
    void shouldFindRoutesByDestinationAirportIataCode() {
        RouteReadModel route = mock(RouteReadModel.class);
        when(repository.findByDestinationAirport_IataCode("OPO")).thenReturn(List.of(route));
        List<RouteReadModel> result = repository.findByDestinationAirport_IataCode("OPO");
        assertEquals(1, result.size());
        assertSame(route, result.get(0));
        verify(repository).findByDestinationAirport_IataCode("OPO");
    }

    @Test
    void shouldReturnEmptyWhenDestinationAirportDoesNotExist() {
        when(repository.findByDestinationAirport_IataCode("XXX")).thenReturn(List.of());
        List<RouteReadModel> result = repository.findByDestinationAirport_IataCode("XXX");
        assertTrue(result.isEmpty());
        verify(repository).findByDestinationAirport_IataCode("XXX");
    }

    @Test
    void shouldSaveRoute() {
        RouteReadModel route = mock(RouteReadModel.class);
        when(repository.save(route)).thenReturn(route);
        RouteReadModel savedRoute = repository.save(route);
        assertNotNull(savedRoute);
        assertSame(route, savedRoute);
        verify(repository).save(route);
    }

    @Test
    void shouldDeleteRoute() {
        RouteReadModel route = mock(RouteReadModel.class);
        repository.delete(route);
        verify(repository).delete(route);
    }

    @Test
    void shouldCheckIfRouteExistsById() {
        when(repository.existsById(1L)).thenReturn(true);
        boolean exists = repository.existsById(1L);
        assertTrue(exists);
        verify(repository).existsById(1L);
    }
}
