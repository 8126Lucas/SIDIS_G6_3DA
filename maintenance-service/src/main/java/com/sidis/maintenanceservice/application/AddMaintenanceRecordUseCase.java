package com.sidis.maintenanceservice.application;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import com.sidis.maintenanceservice.UseCase;


/**
 * Use case: a Maintenance Technician adds a new maintenance record.
 *
 * <p>aircraft
 * registration, maintenance type (according to a maintenance template), description, start date, expected duration and
 * its checklist (defined by the maintenance template)
 *
 */

@UseCase
public class AddMaintenanceRecordUseCase {
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public AddMaintenanceRecordUseCase(MaintenanceRecordRepository maintenanceRecordRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    /**
     * Looks up the model for the given modelName, creates a {@link com.sidis.maintenanceservice.domain.MaintenanceRecord}
     *with the given {@link com.sidis.maintenanceservice.Aircraft.domain.Aircraft},manufacturer,name and persists it.
     * @param aircraft, the aircraft to do the maintenance
     * @param maintenanceTemplate, the Template of maintenance;
     * @param description the description of the maintenance;
     * @param startDate, the day of maintenance starts;
     * @param durationHours the expected duration of the maintenance;
     */
    /*public void executeShort(Aircraft aircraft,MaintenanceTemplate maintenanceTemplate,Double durationHours, String description, LocalDate startDate) {
        maintenanceRecordRepository.save(new MaintenanceRecord(aircraft,maintenanceTemplate,durationHours,description,startDate));
    }*/

    /**
     * Adds a record to persist
     * @param maintenanceRecord the model object to be persisted
     */
    public void execute(MaintenanceRecord maintenanceRecord) {
        maintenanceRecordRepository.save(maintenanceRecord);
    }
}


