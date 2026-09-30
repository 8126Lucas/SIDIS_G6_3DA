package com.sidis.routeservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

@Embeddable
public class RouteHistory {
    @Column(nullable = false)
    private LocalDate routeBegin;

    private LocalDate routeFinish;

    private Integer routeUsage;

    public RouteHistory() {}

    public RouteHistory(LocalDate routeBegin, Integer routeUsage) {
        this.routeBegin = routeBegin;
        this.routeUsage = routeUsage;
    }

    public void endRoute(LocalDate routeFinish) {
        this.routeFinish = routeFinish;
    }

    public void updateRouteUsage(Integer routeUsage) {
        this.routeUsage += routeUsage;
    }

    public LocalDate getRouteBegin() {
        return routeBegin;
    }

    public LocalDate getRouteFinish() {
        return routeFinish;
    }

    public Integer getRouteUsage() {
        return routeUsage;
    }
}
