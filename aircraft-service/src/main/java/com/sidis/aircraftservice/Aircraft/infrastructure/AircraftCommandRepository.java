package com.sidis.aircraftservice.Aircraft.infrastructure;

import com.sidis.aircraftservice.Aircraft.domain.Aircraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;


@Repository
public interface AircraftCommandRepository extends JpaRepository<Aircraft, String>, JpaSpecificationExecutor<Aircraft> {

}