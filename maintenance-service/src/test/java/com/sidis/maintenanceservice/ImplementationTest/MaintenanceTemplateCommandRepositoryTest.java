package com.sidis.maintenanceservice.ImplementationTest;

import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateCommandRepository;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//runtime
class MaintenanceTemplateCommandRepositoryTest {
    @Mock
    private MaintenanceTemplateCommandRepository repository;

    @Mock
    private MaintenanceTemplate maintenanceTemplate;

    @Test
    void shouldFindTemplateById() {
        when(repository.findById(1L)).thenReturn(Optional.of(maintenanceTemplate));
        Optional<MaintenanceTemplate> result = repository.findById(1L);
        assertTrue(result.isPresent());
        assertSame(maintenanceTemplate, result.get());
        verify(repository).findById(1L);
    }

    @Test
    void shouldReturnEmptyWhenTemplateDoesNotExist() {
        when(repository.findById(999L)).thenReturn(Optional.empty());
        Optional<MaintenanceTemplate> result = repository.findById(999L);
        assertTrue(result.isEmpty());
        verify(repository).findById(999L);
    }

    @Test
    void shouldFindTemplateByTemplateId() {
        when(repository.findByTemplateId(1L)).thenReturn(Optional.of(maintenanceTemplate));
        Optional<MaintenanceTemplate> result = repository.findByTemplateId(1L);
        assertTrue(result.isPresent());
        assertSame(maintenanceTemplate, result.get());
        verify(repository).findByTemplateId(1L);
    }

    @Test
    void shouldSaveTemplate() {
        when(repository.save(maintenanceTemplate)).thenReturn(maintenanceTemplate);
        MaintenanceTemplate savedTemplate = repository.save(maintenanceTemplate);
        assertNotNull(savedTemplate);
        assertSame(maintenanceTemplate, savedTemplate);
        verify(repository).save(maintenanceTemplate);
    }

    @Test
    void shouldDeleteTemplate() {
        repository.delete(maintenanceTemplate);
        verify(repository).delete(maintenanceTemplate);
    }

    @Test
    void shouldCheckIfTemplateExistsById() {
        when(repository.existsById(1L)).thenReturn(true);
        boolean exists = repository.existsById(1L);
        assertTrue(exists);
        verify(repository).existsById(1L);
    }

    @Test
    void shouldFindAllTemplates() {
        MaintenanceTemplate secondTemplate = mock(MaintenanceTemplate.class);
        when(repository.findAll()).thenReturn(List.of(maintenanceTemplate, secondTemplate));
        List<MaintenanceTemplate> result = repository.findAll();
        assertEquals(2, result.size());
        assertSame(maintenanceTemplate, result.get(0));
        assertSame(secondTemplate, result.get(1));
        verify(repository).findAll();
    }
}
