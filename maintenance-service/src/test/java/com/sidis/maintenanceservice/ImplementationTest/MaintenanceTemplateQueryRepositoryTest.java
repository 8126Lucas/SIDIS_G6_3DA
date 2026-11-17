package com.sidis.maintenanceservice.ImplementationTest;

import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.domain.MaintenanceTemplateRedis;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateQueryRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceTemplateQueryRepositoryTest {
    @Mock
    private MaintenanceTemplateQueryRepository repository;

    private MaintenanceTemplateRedis createTemplate() {
        return new MaintenanceTemplateRedis("Engine Inspection", 3.5, Map.of("Check engine", true, "Check oil", false), MaintenanceType.values()[0], MaintenanceAttribute.values()[0]);
    }

    @Test
    void shouldFindTemplateById() {
        MaintenanceTemplateRedis template = createTemplate();
        template.setTemplateId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(template));
        Optional<MaintenanceTemplateRedis> result = repository.findById(1L);
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getTemplateId());
        assertEquals("Engine Inspection", result.get().getName());
        verify(repository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenTemplateDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        Optional<MaintenanceTemplateRedis> result = repository.findById(999L);
        assertTrue(result.isEmpty());
        verify(repository).findById(999L);
    }

    @Test
    void shouldFindTemplateByTemplateId() {
        MaintenanceTemplateRedis template = createTemplate();
        template.setTemplateId(2L);
        when(repository.findByTemplateId(2L)).thenReturn(Optional.of(template));
        Optional<MaintenanceTemplateRedis> result = repository.findByTemplateId(2L);
        assertTrue(result.isPresent());
        assertEquals(2L, result.get().getTemplateId());
        verify(repository).findByTemplateId(2L);
    }

    @Test
    void shouldSaveTemplate() {
        MaintenanceTemplateRedis template = createTemplate();
        when(repository.save(template)).thenReturn(template);
        MaintenanceTemplateRedis savedTemplate = repository.save(template);
        assertNotNull(savedTemplate);
        assertEquals("Engine Inspection", savedTemplate.getName());
        verify(repository).save(template);
    }

    @Test
    void shouldDeleteTemplate() {
        MaintenanceTemplateRedis template = createTemplate();
        template.setTemplateId(3L);
        repository.delete(template);
        verify(repository).delete(template);
    }

    @Test
    void shouldFindAllTemplates() {
        MaintenanceTemplateRedis first = createTemplate();
        MaintenanceTemplateRedis second = createTemplate();
        second.setName("Landing Gear Inspection");
        when(repository.findAll()).thenReturn(List.of(first, second));
        List<MaintenanceTemplateRedis> result = repository.findAll();
        assertEquals(2, result.size());
        assertEquals("Engine Inspection", result.get(0).getName());
        assertEquals("Landing Gear Inspection", result.get(1).getName());
        verify(repository).findAll();
    }
}
