package com.sidis.maintenanceservice.application;

//import com.sidis.maintenanceservice.Aircraft.domain.Aircraft;
import com.sidis.maintenanceservice.domain.MaintenanceAttribute;
import com.sidis.maintenanceservice.domain.MaintenanceRecord;
import com.sidis.maintenanceservice.infrastructure.MaintenanceRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class MaintenanceRecordSearchService {
    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceRecordSearchService(MaintenanceRecordRepository maintenanceRecordRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    /*public Page<MaintenanceRecord> findRecordsMatchingFilter(
            Aircraft aircraft,
            String attribute,
            LocalDate initialDate,
            LocalDate finalDate,
            Pageable pageable) {

        Specification<MaintenanceRecord> spec = Specification.where(null);

        if (aircraft != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("aircraft"), aircraft));
        }

        if (attribute != null && !attribute.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(
                            root.get("maintenanceTemplate")
                                    .get("attribute"),
                            MaintenanceAttribute.valueOf(attribute)
                    ));
        }

        if (initialDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            root.get("startDate"),
                            initialDate
                    ));
        }

        if (finalDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            root.get("startDate"),
                            finalDate
                    ));
        }

        return maintenanceRecordRepository.findAll(spec, pageable);
    }*/
}
