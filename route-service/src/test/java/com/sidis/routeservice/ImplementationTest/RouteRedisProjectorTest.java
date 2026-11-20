package com.sidis.routeservice.ImplementationTest;

import com.sidis.routeservice.Route.domain.RouteReadModel;
import com.sidis.routeservice.Route.infrastructure.RouteReadRepository;
import com.sidis.routeservice.infrastructure.messaging.RouteRedisProjector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteRedisProjectorTest {
    @Mock
    private RouteReadRepository routeReadRepository;

    @InjectMocks
    private RouteRedisProjector routeRedisProjector;

    private RouteReadModel route;

    @BeforeEach
    void setUp() {
        route = mock(RouteReadModel.class);
    }

    @Test
    void shouldThrowExceptionWhenRouteVersionIsNull() {
        when(route.version()).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> routeRedisProjector.project(route));
        verifyNoInteractions(routeReadRepository);
    }

    @Test
    void shouldSaveRouteWhenItDoesNotExist() {
        when(route.routeId()).thenReturn(1L);
        when(route.version()).thenReturn(1L);
        when(routeReadRepository.findById(1L)).thenReturn(Optional.empty());
        routeRedisProjector.project(route);
        verify(routeReadRepository).save(route);
    }

    @Test
    void shouldNotSaveRouteWhenStoredVersionIsEqual() {
        when(route.routeId()).thenReturn(1L);
        when(route.version()).thenReturn(1L);
        RouteReadModel existingRoute = mock(RouteReadModel.class);
        when(existingRoute.version()).thenReturn(1L);
        when(routeReadRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
        routeRedisProjector.project(route);
        verify(routeReadRepository, never()).save(any());
    }

    @Test
    void shouldNotSaveRouteWhenStoredVersionIsHigher() {
        when(route.routeId()).thenReturn(1L);
        when(route.version()).thenReturn(1L);
        RouteReadModel existingRoute = mock(RouteReadModel.class);
        when(existingRoute.version()).thenReturn(2L);
        when(routeReadRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
        routeRedisProjector.project(route);
        verify(routeReadRepository, never()).save(any());
    }

    @Test
    void shouldUpdateRouteWhenReceivedVersionIsHigher() {
        when(route.routeId()).thenReturn(1L);
        when(route.version()).thenReturn(2L);
        RouteReadModel existingRoute = mock(RouteReadModel.class);
        when(existingRoute.version()).thenReturn(1L);
        when(routeReadRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
        routeRedisProjector.project(route);
        verify(routeReadRepository).save(route);
    }

    @Test
    void shouldSaveRouteWhenStoredVersionIsNull() {
        when(route.routeId()).thenReturn(1L);
        when(route.version()).thenReturn(1L);
        RouteReadModel existingRoute = mock(RouteReadModel.class);
        when(existingRoute.version()).thenReturn(null);
        when(routeReadRepository.findById(1L)).thenReturn(Optional.of(existingRoute));
        routeRedisProjector.project(route);
        verify(routeReadRepository).save(route);
    }
}
