package com.sidis.maintenanceservice.WP4A.US115.US115b;

import com.sidis.maintenanceservice.application.AddMaintenanceTemplateUseCase;
import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.infrastructure.MaintenanceTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AddMaintenanceTemplateUseCaseToRepoTest {
    private MaintenanceTemplateRepository maintenanceTemplateRepository;
    private AddMaintenanceTemplateUseCase useCase;

    @BeforeEach
    void setUp() {
        maintenanceTemplateRepository = mock(MaintenanceTemplateRepository.class);
        useCase = new AddMaintenanceTemplateUseCase(maintenanceTemplateRepository);
    }

    @Test
    void shouldSaveMaintenanceTemplateWhenExecuteWithFields() {
        Map<String, Boolean> checklist = Map.of(
                "Check oil levels", true,
                "Inspect engine turbine blades", true
        );

        when(maintenanceTemplateRepository.save(any(MaintenanceTemplate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceTemplate result = useCase.execute(
                "A-Check Engine Inspection",
                6.5,
                checklist,
                MaintenanceType.INSPECTION,
                MaintenanceAttribute.ENGINE
        );

        ArgumentCaptor<MaintenanceTemplate> captor =
                ArgumentCaptor.forClass(MaintenanceTemplate.class);
        verify(maintenanceTemplateRepository, times(1)).save(captor.capture());

        MaintenanceTemplate savedTemplate = captor.getValue();
        assertEquals(savedTemplate, result);
        assertEquals("A-Check Engine Inspection", savedTemplate.getName());
        assertEquals(6.5, savedTemplate.getExpectedDuration());
        assertEquals(checklist, savedTemplate.getTemplateChecklist());
        assertEquals(MaintenanceType.INSPECTION, savedTemplate.getOperation());
        assertEquals(MaintenanceAttribute.ENGINE, savedTemplate.getAttribute());
    }

    @Test
    void shouldSaveMaintenanceTemplateWhenExecuteWithTemplateObject() {
        MaintenanceTemplate template = new MaintenanceTemplate(
                "A-Check Engine Inspection",
                6.5,
                Map.of("Check oil levels", true),
                MaintenanceType.INSPECTION,
                MaintenanceAttribute.ENGINE
        );
        when(maintenanceTemplateRepository.save(template)).thenReturn(template);

        MaintenanceTemplate result = useCase.execute(template);

        assertEquals(template, result);
        verify(maintenanceTemplateRepository, times(1)).save(template);
    }
}
