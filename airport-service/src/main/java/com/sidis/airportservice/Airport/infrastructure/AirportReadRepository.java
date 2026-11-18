package com.sidis.airportservice.Airport.infrastructure;

import com.sidis.airportservice.Airport.domain.AirportReadModel;
import com.sidis.airportservice.Airport.domain.AirportStatus;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AirportReadRepository
        extends CrudRepository<AirportReadModel, String> {

    List<AirportReadModel> findByStatus(AirportStatus status);

    List<AirportReadModel> findByLocation_Country(String country);

    List<AirportReadModel> findByLocation_City(String city);
}