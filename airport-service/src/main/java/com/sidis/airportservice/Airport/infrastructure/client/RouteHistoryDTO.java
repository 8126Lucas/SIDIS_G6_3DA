package com.sidis.airportservice.Airport.infrastructure.client;

import java.time.LocalDate;

public record RouteHistoryDTO(
        LocalDate routeBegin,
        LocalDate routeFinish,
        Integer routeUsage
){}
