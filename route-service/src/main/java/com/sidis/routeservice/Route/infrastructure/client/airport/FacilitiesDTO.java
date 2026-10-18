package com.sidis.routeservice.Route.infrastructure.client.airport;

import java.util.List;

public record FacilitiesDTO(
        Integer terminalCount,
        Integer gateCount,
        List<String> services
){}
