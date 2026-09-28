package com.sidis.maintenanceservice;

//import com.sidis.maintenanceservice.Aircraft.HangarController;
//import com.sidis.maintenanceservice.Aircraft.application.AircraftSearchService;
//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
//import com.sidis.maintenanceservice.AircraftCatalog.ModelCatalogController;
//import com.sidis.maintenanceservice.AircraftCatalog.domain.AircraftSpecs;
import com.sidis.maintenanceservice.application.*;
import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.domain.MaintenanceStatus;
import com.sidis.maintenanceservice.domain.MaintenanceTemplate;
import com.sidis.maintenanceservice.domain.MaintenanceType;
import com.sidis.maintenanceservice.exceptions.DomainException;
import com.sidis.maintenanceservice.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/maintenance")
public class MaintenanceRecordController {

    private final AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase;
    private final AddMaintenanceRecordUseCase addMaintenanceRecordUseCase;
//    private final AircraftSearchService aircraftSearchService;
    private final MaintenanceTemplateSearchService maintenanceTemplateSearchService;
    private final MaintenanceRecordProgressService maintenanceRecordProgressService;
    private final MaintenanceReportService maintenanceReportService;
    private final MaintenanceAlertService maintenanceAlertService;
    private final MaintenanceRecordSearchService maintenanceRecordSearchService;
    private final PartsInventoryService partsInventoryService;

    public MaintenanceRecordController(AddMaintenanceTemplateUseCase addMaintenanceTemplateUseCase,
                                       AddMaintenanceRecordUseCase addMaintenanceRecordUseCase,
                                       /*AircraftSearchService aircraftSearchService,*/
                                       MaintenanceTemplateSearchService maintenanceTemplateSearchService,
                                       MaintenanceRecordProgressService maintenanceRecordProgressService,
                                       MaintenanceReportService maintenanceReportService,
                                       MaintenanceAlertService maintenanceAlertService,
                                       MaintenanceRecordSearchService maintenanceRecordSearchService,
                                       PartsInventoryService partsInventoryService) {
        this.addMaintenanceTemplateUseCase = addMaintenanceTemplateUseCase;
        this.addMaintenanceRecordUseCase = addMaintenanceRecordUseCase;
//        this.aircraftSearchService = aircraftSearchService;
        this.maintenanceTemplateSearchService = maintenanceTemplateSearchService;
        this.maintenanceRecordProgressService = maintenanceRecordProgressService;
        this.maintenanceReportService = maintenanceReportService;
        this.maintenanceAlertService = maintenanceAlertService;
        this.maintenanceRecordSearchService = maintenanceRecordSearchService;
        this.partsInventoryService = partsInventoryService;
    }

    // WP3 — criar template
    @PostMapping("/templates")
    public MaintenanceTemplateResponse addTemplate(@Valid @RequestBody AddTemplateRequest request) {
        MaintenanceTemplate template = addMaintenanceTemplateUseCase.execute(
                request.name(),
                request.expectedDuration(),
                request.templateChecklist(),
                MaintenanceType.valueOf(request.operation()),
                MaintenanceAttribute.valueOf(request.attribute())
        );
        return MaintenanceTemplateResponse.from(template);
    }

    // US117 — obter template por id
    @GetMapping("/templates/{id}")
    public MaintenanceTemplateResponse getTemplate(@PathVariable Long id) {
        MaintenanceTemplate template = maintenanceTemplateSearchService
                .findByTemplateId(id)
                .orElseThrow(() -> new DomainException("Maintenance Template does not exist"));
        return MaintenanceTemplateResponse.from(template);
    }

    // US117 — listar templates
    @GetMapping("/templates")
    public List<MaintenanceTemplateResponse> listTemplates() {
        return maintenanceTemplateSearchService.findAll()
                .stream()
                .map(MaintenanceTemplateResponse::from)
                .toList();
    }

    // WP3 — criar registo curto
    @PostMapping("/record")
    public void addRecord(@Valid @RequestBody AddRecordShortRequest request) {
        /*Aircraft aircraft = aircraftSearchService
                .spotAircraftInHangar(request.registrationNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Aircraft does not exist in hangar"));
*/
        MaintenanceTemplate maintenanceTemplate = maintenanceTemplateSearchService
                .findByTemplateId(request.templateId())
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance Template does not exist"));

        /*addMaintenanceRecordUseCase.executeShort(
                aircraft,
                maintenanceTemplate,
                request.durationHours(),
                request.description(),
                request.startDate()
        );*/
    }

    // US119 — obter registo por id
    @GetMapping("/records/{id}")
    public MaintenanceRecordResponse getRecord(@PathVariable Long id) {
        MaintenanceRecord record = maintenanceRecordProgressService
                .findById(id)
                .orElseThrow(() -> new DomainException("Maintenance Record does not exist"));
        return MaintenanceRecordResponse.from(record);
    }

    // US119 — avançar estado do registo
    @PatchMapping("/records/{id}/status")
    public MaintenanceRecordResponse advanceStatus(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateRecordStatusRequest request) {
        MaintenanceRecord record = maintenanceRecordProgressService.advanceStatus(
                id,
                MaintenanceStatus.valueOf(request.status())
        );
        return MaintenanceRecordResponse.from(record);
    }

    // US221 — tempo médio de manutenção por tipo de aeronave
    /*@GetMapping("/turnaround-average")
    public List<TurnaroundAverageResponse> turnaroundAverage() {
        return maintenanceReportService.averageTurnaroundTimePerAircraftType()
                .stream()
                .map(t -> new TurnaroundAverageResponse(t.aircraftType(), t.averageDays()))
                .toList();
    }*/

    // US219 - atividades de manutencao em curso na frota
    @GetMapping("/ongoing")
    public List<MaintenanceRecordResponse> ongoingMaintenanceActivities() {
        return maintenanceReportService.ongoingMaintenanceActivities()
                .stream()
                .map(MaintenanceRecordResponse::from)
                .toList();
    }

    // US220 - custos de manutencao por aeronave ou modelo
    /*@GetMapping("/reports/costs")
    public List<MaintenanceCostReportResponse> maintenanceCosts(
            @RequestParam(defaultValue = "aircraft") String groupBy) {
        return maintenanceReportService.maintenanceCosts(groupBy)
                .stream()
                .map(r -> new MaintenanceCostReportResponse(r.group(), r.totalCost()))
                .toList();
    }*/

    // US222 — alertas de manutenção por horas de voo ou dias de calendário
    /*@GetMapping("/alerts")
    public List<MaintenanceAlertResponse> maintenanceAlerts(
            @RequestParam(defaultValue = "180") int calendarDaysThreshold,
            @RequestParam(defaultValue = "500") double flightHoursThreshold) {
        return maintenanceAlertService.findAircraftDueForMaintenance(calendarDaysThreshold, flightHoursThreshold)
                .stream()
                .map(a -> new MaintenanceAlertResponse(
                        a.registrationNumber(),
                        a.aircraftModel(),
                        a.totalFlightHours(),
                        a.lastMaintenanceDate(),
                        a.daysSinceLastMaintenance(),
                        a.alertReason()
                ))
                .toList();
    }*/

    @PatchMapping("/records/{id}")
    public void updateRecord(@PathVariable Long id,@RequestBody ChangeAttributeRequest request) {
        MaintenanceRecord record = maintenanceRecordProgressService
                .findById(id)
                .orElseThrow(() ->
                        new DomainException("Maintenance Record does not exist"));

        maintenanceRecordProgressService.changeRecordAttribute(record,request.attribute());
    }

    @GetMapping("/records")
    public /*List<MaintenanceRecordResponse>*/ void getRecordsWithFiltering(
            @RequestParam(required = false) String aircraft,
            @RequestParam(required = false) String attribute,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate initialDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate finalDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);

//        Aircraft aircraftObj = null;

        /*if (aircraft != null) {
            aircraftObj = aircraftSearchService
                    .spotAircraftInHangar(aircraft)
                    .orElseThrow(() ->
                            new DomainException("Aircraft does not exist"));
        }*/

        /*return maintenanceRecordSearchService
                .findRecordsMatchingFilter(aircraftObj, attribute, initialDate, finalDate, pageable)
                .map(MaintenanceRecordResponse::from)
                .toList();*/
    }

    // US226 — inventário de peças usadas em manutenção
    @GetMapping("/parts/inventory")
    public List<PartUsageSummaryResponse> partsInventory() {
        return partsInventoryService.getPartsUsageSummary()
                .stream()
                .map(s -> new PartUsageSummaryResponse(
                        s.partSerialNumber(), s.totalQuantityUsed(), s.totalCost(), s.timesUsedInRecords()))
                .toList();
    }

    // US226 — alertas de stock baixo de peças
    @GetMapping("/parts/low-stock")
    public List<LowStockAlertResponse> lowStockAlerts(
            @RequestParam(defaultValue = "10") int threshold) {
        return partsInventoryService.getLowStockAlerts(threshold)
                .stream()
                .map(a -> new LowStockAlertResponse(
                        a.partSerialNumber(), a.totalQuantityUsed(), a.timesUsedInRecords(), a.alertMessage()))
                .toList();
    }

    // DTOs
    record AddRecordShortRequest(@NotBlank String registrationNumber,
                                 @NotNull Long templateId,
                                 @NotNull Double durationHours,
                                 @NotBlank String description,
                                 @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate) {}

    record AddTemplateRequest(@NotBlank String name,
                              @NotNull Double expectedDuration,
                              @NotNull Map<String, Boolean> templateChecklist,
                              @NotBlank String operation,
                              @NotBlank String attribute) {}

    public record ChangeAttributeRequest(MaintenanceAttribute attribute) {}

    record UpdateRecordStatusRequest(@NotBlank String status) {}

    // Responses
    record MaintenanceTemplateResponse(Long templateId, String name,
                                       Double expectedDuration,
                                       Map<String, Boolean> templateChecklist,
                                       MaintenanceType operation,
                                       MaintenanceAttribute attribute) {
        static MaintenanceTemplateResponse from(MaintenanceTemplate template) {
            return new MaintenanceTemplateResponse(
                    template.getTemplateId(),
                    template.getName(),
                    template.getExpectedDuration(),
                    template.getTemplateChecklist(),
                    template.getOperation(),
                    template.getAttribute()
            );
        }
    }

    record MaintenanceRecordResponse(Long id,
                                     String aircraftRegistration,
                                     MaintenanceTemplateResponse template,
                                     Double durationHours,
                                     String description,
                                     LocalDate startDate,
                                     MaintenanceStatus status) {
        static MaintenanceRecordResponse from(MaintenanceRecord r) {
            return new MaintenanceRecordResponse(
                    r.getRecordId(),
                    /*r.getAircraft().getRegistrationNumber(),*/ null,
                    MaintenanceTemplateResponse.from(r.getMaintenanceTemplate()),
                    r.getDurationHours(),
                    r.getDescription(),
                    r.getStartDate(),
                    r.getStatus()
            );
        }
    }

    record TurnaroundAverageResponse(String aircraftType, Double averageDays) {}

    record MaintenanceCostReportResponse(String group, Double totalCost) {}

    record MaintenanceAlertResponse(String registrationNumber,
                                    String aircraftModel,
                                    Double totalFlightHours,
                                    LocalDate lastMaintenanceDate,
                                    Long daysSinceLastMaintenance,
                                    String alertReason) {}

    record PartUsageSummaryResponse(String partSerialNumber,
                                    Integer totalQuantityUsed,
                                    Double totalCost,
                                    Long timesUsedInRecords) {}

    record LowStockAlertResponse(String partSerialNumber,
                                 Integer totalQuantityUsed,
                                 Long timesUsedInRecords,
                                 String alertMessage) {}
}
