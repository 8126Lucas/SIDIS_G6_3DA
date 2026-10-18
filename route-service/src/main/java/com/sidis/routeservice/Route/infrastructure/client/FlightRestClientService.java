package com.sidis.routeservice.Route.infrastructure.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FlightRestClientService {
    private final RestClient flightClient;

    public FlightRestClientService(@Qualifier("flightRestClient") RestClient flightClient) {
        this.flightClient = flightClient;
    }
}
