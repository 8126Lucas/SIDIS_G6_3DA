package com.sidis.maintenanceservice.ImplementationTest;

import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.domain.MaintenanceTemplateRedis;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateQueryRepository;

import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceTemplateRedisProjector;
import com.sidis.maintenanceservice.infrastructure.messaging.MaintenanceTemplateSyncEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceTemplateRedisProjectorTest {
    @Mock
    private MaintenanceTemplateQueryRepository maintenanceQueryRepository;

    @InjectMocks
    private MaintenanceTemplateRedisProjector projector;

    private MaintenanceTemplateSyncEvent createEvent() {
        return new MaintenanceTemplateSyncEvent(1L, "Engine Inspection", 3.5,
                Map.of("Check engine", true, "Check oil", false), MaintenanceType.values()[0], MaintenanceAttribute.values()[0]);
    }

    @Test
    void shouldUpdateExistingTemplateInRedis() {
        MaintenanceTemplateSyncEvent event = createEvent();
        MaintenanceTemplateRedis existingTemplate = mock(MaintenanceTemplateRedis.class);

        when(maintenanceQueryRepository.findById(event.templateId())).thenReturn(Optional.of(existingTemplate));
        projector.syncAircraftToRedis(event);

        verify(maintenanceQueryRepository).findById(event.templateId());
        verify(existingTemplate).setName(event.name());
        verify(existingTemplate).setOperation(event.operation());
        verify(existingTemplate).setExpectedDuration(event.expectedDuration());
        verify(existingTemplate).setTemplateChecklist(event.templateChecklist());
        verify(existingTemplate).setAttribute(event.attribute());
        verify(maintenanceQueryRepository).save(existingTemplate);
    }

    @Test
    void shouldCreateTemplateWhenItDoesNotExistInRedis() {
        MaintenanceTemplateSyncEvent event = createEvent();

        when(maintenanceQueryRepository.findById(event.templateId())).thenReturn(Optional.empty());
        projector.syncAircraftToRedis(event);
        ArgumentCaptor<MaintenanceTemplateRedis> captor = ArgumentCaptor.forClass(MaintenanceTemplateRedis.class);
        verify(maintenanceQueryRepository).save(captor.capture());
        MaintenanceTemplateRedis savedTemplate = captor.getValue();

        assertNotNull(savedTemplate);
        assertEquals(event.name(), savedTemplate.getName());
        assertEquals(event.operation(), savedTemplate.getOperation());
        assertEquals(event.expectedDuration(), savedTemplate.getExpectedDuration());
        assertEquals(event.templateChecklist(), savedTemplate.getTemplateChecklist());
        assertEquals(event.attribute(), savedTemplate.getAttribute());
    }


    @Test
    void shouldNotSaveTemplateWhenRepositoryThrowsException() {
        MaintenanceTemplateSyncEvent event = createEvent();

        when(maintenanceQueryRepository.findById(event.templateId())).thenThrow(new RuntimeException("Redis connection error"));
        assertThrows(RuntimeException.class, () -> projector.syncAircraftToRedis(event));
        verify(maintenanceQueryRepository, never()).save(any(MaintenanceTemplateRedis.class));
    }
}
