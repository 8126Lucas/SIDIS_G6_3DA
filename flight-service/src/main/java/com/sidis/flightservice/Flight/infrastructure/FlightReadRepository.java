package com.sidis.flightservice.Flight.infrastructure;

import com.sidis.flightservice.Flight.domain.FlightReadModel;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface FlightReadRepository extends CrudRepository<FlightReadModel, Long> {

    List<FlightReadModel> findByAircraftRegistrationNumber(String registrationNumber);
}
