package com.sidis.flightservice.domain;

import com.sidis.flightservice.domain.AircraftReference;
import com.sidis.flightservice.domain.RouteReference;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "company_flights")
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long flightId;

    @Version
    private Long version;

    @JoinColumn(name = "route_id")
    private RouteReference routeId;

    @JoinColumn(name = "aircraft_registration")
    private AircraftReference aircraftRegistrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FlightStatus flightStatus;

    @Column(nullable = false)
    private LocalDateTime scheduledDeparture;

    @Column(nullable = false)
    private LocalDateTime scheduledArrival;

    @Column
    private LocalDateTime timeOfDeparture;

    @Column
    private LocalDateTime timeOfArrival;

    protected Flight() {
    }

    public Flight(RouteReference route, AircraftReference aircraft, LocalDateTime scheduledDeparture,
            LocalDateTime scheduledArrival) {
        this.flightStatus=FlightStatus.SCHEDULED;
        this.routeId = route;
        this.aircraftRegistrationNumber = aircraft;
        this.scheduledDeparture = scheduledDeparture;
        this.scheduledArrival = scheduledArrival;
    }

    public void updateAircraft(AircraftReference aircraft) {
        if (aircraft == null) {
            throw new IllegalArgumentException(
                    "Aircraft cannot be null"
            );
        }
        if (this.flightStatus == FlightStatus.ARRIVED) {
            throw new IllegalStateException(
                    "Cannot change aircraft of completed flight"
            );
        }
        this.aircraftRegistrationNumber = aircraft;
    }

    public void changeRoute(RouteReference route) {
        if (route == null) {
            throw new IllegalArgumentException(
                    "Route cannot be null"
            );
        }
        if (this.flightStatus == FlightStatus.ARRIVED) {
            throw new IllegalStateException(
                    "Cannot change route of completed flight"
            );
        }
        this.routeId = route;
    }

    public void startFlight() {
        if (flightStatus != FlightStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Flight cannot be started"
            );
        }
        this.flightStatus = FlightStatus.IN_PROGRESS;
        this.timeOfDeparture = LocalDateTime.now();
    }

    public void completeFlight() {
        if (this.flightStatus != FlightStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Flight is not in progress"
            );
        }
        this.flightStatus = FlightStatus.ARRIVED;
        this.timeOfArrival = LocalDateTime.now();
        Double hours=getFlightHours();
        //this.aircraft.updateOperationalHours(hours);
        //this.aircraft.updateFlightHours(hours);
        //this.aircraft.updateMeanRange(this.route.getDistanceKm());
    }

    public void cancelFlight() {
        if (flightStatus == FlightStatus.ARRIVED) {
            throw new IllegalStateException(
                    "Completed flight cannot be cancelled"
            );
        }
        this.flightStatus = FlightStatus.CANCELED;
    }

    public boolean isActive() {
        return flightStatus == FlightStatus.IN_PROGRESS;
    }

    public Long getFlightId() {
        return flightId;
    }

    public RouteReference getRoute() {
        return routeId;
    }

    public AircraftReference getAircraft() {
        return aircraftRegistrationNumber;
    }

    public FlightStatus getFlightStatus() {
        return flightStatus;
    }

    public LocalDateTime getScheduledDeparture() {
        return scheduledDeparture;
    }

    public LocalDateTime getScheduledArrival() {
        return scheduledArrival;
    }

    public Double getFlightHours() {
        if(this.flightStatus == FlightStatus.ARRIVED) {
            Long minutes=ChronoUnit.MINUTES.between(this.timeOfDeparture, this.timeOfArrival);
            if(minutes<10){
                return ChronoUnit.MINUTES.between(this.scheduledDeparture, this.scheduledArrival)/60.0;
            }
            return minutes/60.0;
        }
        return null;
    }
}