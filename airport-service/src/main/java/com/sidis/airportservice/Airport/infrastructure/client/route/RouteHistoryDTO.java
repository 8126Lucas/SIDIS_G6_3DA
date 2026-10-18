package com.sidis.airportservice.Airport.infrastructure.client.route;

import java.time.LocalDate;

public record RouteHistoryDTO(
        LocalDate routeBegin,
        LocalDate routeFinish,
        Integer routeUsage
){}
