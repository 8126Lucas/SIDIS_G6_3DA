package com.sidis.routeservice.Route.application;

import com.sidis.routeservice.Route.domain.RouteReadModel;

public record RouteChangedEvent(RouteReadModel route) {
}
