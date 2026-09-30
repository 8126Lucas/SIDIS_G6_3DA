package com.sidis.routeservice.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "company_routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long routeId;

    @Version
    private Long version;

    @Column(nullable = false)
    private String routeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RouteType type;

    @Embedded
    @AttributeOverride(
            name = "iataCode",
            column = @Column(
                    name = "origin_airport_iata",
                    nullable = false,
                    length = 3
            )
    )
    private AirportReference originAirport;

    @Embedded
    @AttributeOverride(
            name = "iataCode",
            column = @Column(
                    name = "destination_airport_iata",
                    nullable = false,
                    length = 3
            )
    )
    private AirportReference destinationAirport;


    @Embedded
    private RouteHistory routeHistory;

    @Column(nullable = false)
    private Double estimatedFlightTimeHours;

    @Column(nullable = false)
    private Double distanceKm;

    @Embedded
    private RouteRequirements routeRequirements;

    protected Route() {
    }

    public Route(RouteRequirements routeRequirements, RouteStatus status, RouteType type,
            RouteHistory routeHistory, Double estimatedFlightTimeHours, AirportReference originAirport,
                 AirportReference destinationAirport,String routeName) {
        this(routeRequirements, status, type, routeHistory, estimatedFlightTimeHours,
                1.0, originAirport, destinationAirport, routeName);
    }

    public Route(RouteRequirements routeRequirements, RouteStatus status, RouteType type,
            RouteHistory routeHistory, Double estimatedFlightTimeHours, Double distanceKm,
                 AirportReference originAirport, AirportReference destinationAirport,String routeName) {
        this.routeRequirements = routeRequirements;
        this.status = status;
        this.type = type;
        this.routeHistory = routeHistory;
        this.estimatedFlightTimeHours = estimatedFlightTimeHours;
        this.distanceKm = distanceKm;
        this.originAirport = originAirport;
        this.destinationAirport = destinationAirport;
        this.routeName = routeName;
    }

    public void activateRoute() {
        if (status == RouteStatus.INACTIVE) {
            throw new IllegalStateException(
                    "Retired routes cannot be activated"
            );
        }
        this.status = RouteStatus.ACTIVE;
    }

    public void suspendRoute() {
        if (status == RouteStatus.ARCHIVED) {
            throw new IllegalStateException(
                    "Archived routes cannot be suspended"
            );
        }

        this.status = RouteStatus.INACTIVE;
    }

    public void retireRoute() {
        this.status = RouteStatus.ARCHIVED;
    }

    public void updateEstimatedFlightTime(Double estimatedFlightTimeHours) {
        if (estimatedFlightTimeHours == null
                || estimatedFlightTimeHours <= 0) {

            throw new IllegalArgumentException(
                    "Estimated flight time must be positive"
            );
        }
        this.estimatedFlightTimeHours =
                estimatedFlightTimeHours;
    }

    public void changeDestination(AirportReference newDestination) {
        if (newDestination == null) {
            throw new IllegalArgumentException(
                    "Destination airport cannot be null"
            );
        }
        if (newDestination.equals(originAirport)) {
            throw new IllegalArgumentException(
                    "Destination airport cannot equal origin airport"
            );
        }
        this.destinationAirport = newDestination;
    }

    public void updateRoute(String routeName, RouteStatus status, RouteType type,
            RouteHistory routeHistory, Double estimatedFlightTimeHours, Double distanceKm,
                            AirportReference originAirport, AirportReference destinationAirport,
            RouteRequirements routeRequirements) {
        if (originAirport == null) {
            throw new IllegalArgumentException("Origin airport cannot be null");
        }
        if (destinationAirport == null) {
            throw new IllegalArgumentException("Destination airport cannot be null");
        }
        if (originAirport.equals(destinationAirport)) {
            throw new IllegalArgumentException("Origin and destination airports cannot be equal");
        }
        if (estimatedFlightTimeHours == null || estimatedFlightTimeHours <= 0) {
            throw new IllegalArgumentException("Estimated flight time must be positive");
        }
        this.routeName = routeName;
        this.status = status;
        this.type = type;
        this.routeHistory = routeHistory;
        this.estimatedFlightTimeHours = estimatedFlightTimeHours;
        this.distanceKm = distanceKm;
        this.originAirport = originAirport;
        this.destinationAirport = destinationAirport;
        this.routeRequirements = routeRequirements;
    }

    public boolean isOperational() {
        return status == RouteStatus.ACTIVE;
    }

    public Long getRouteId() {
        return routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public RouteStatus getStatus() {
        return status;
    }

    public RouteType getType() {
        return type;
    }

    public AirportReference getOriginAirport() {
        return originAirport;
    }

    public AirportReference getDestinationAirport() {
        return destinationAirport;
    }

    public RouteHistory getRouteHistory() {
        return routeHistory;
    }

    public Double getEstimatedFlightTimeHours() {
        return estimatedFlightTimeHours;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public RouteRequirements getRouteRequirements() {
        return routeRequirements;
    }

    public void changeRouteRange(Double distanceKm){
        this.distanceKm = distanceKm;
    }
}
