package com.sidis.flightservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RouteReference {
    @Column(name = "route_Id", nullable = false, length = 3)
    private Long routeId;

    protected RouteReference() {}

    public RouteReference(Long routeId){
        this.routeId=routeId;
    }
    public Long getRouteId() {
        return routeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RouteReference other)) return false;
        return routeId.equals(other.routeId);
    }

    @Override
    public int hashCode() {
        return routeId.hashCode();
    }
}
