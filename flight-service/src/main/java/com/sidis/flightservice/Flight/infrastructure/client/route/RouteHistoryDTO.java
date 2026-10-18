package com.sidis.flightservice.Flight.infrastructure.client.route;

import java.time.LocalDate;

public record RouteHistoryDTO(
        LocalDate routeBegin,
        LocalDate routeFinish,
        Integer routeUsage
){
}
