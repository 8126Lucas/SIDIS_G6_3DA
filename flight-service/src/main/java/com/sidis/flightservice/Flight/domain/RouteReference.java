package com.sidis.flightservice.Flight.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class RouteReference {
    @Column(name = "route_Id", nullable = false, length = 3)
    private Long routeId;

    @Column(name = "origin_Iata", nullable = false, length = 3)
    private String originIata;

    @Column(name = "destination_Iata", nullable = false, length = 3)
    private String destinationIata;

    @Column(name = "route_name")
    private String routeName;

    protected RouteReference() {}

    public RouteReference(Long routeId, String originIata, String routeName, String destinationIata) {
        this.routeId = routeId;
        this.originIata = originIata;
        this.routeName = routeName;
        this.destinationIata = destinationIata;
    }

    public Long getRouteId() {
        return routeId;
    }

    public String getDestinationIata() {
        return destinationIata;
    }

    public String getRouteName() {
        return routeName;
    }

    public String getOriginIata() {
        return originIata;
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
