package com.sidis.airportservice.Airport.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Embeddable
public class Facilities {
    @Column(nullable = false)
    private Integer terminalCount;

    @Column(nullable = false)
    private Integer gateCount;

    @ElementCollection
    @CollectionTable(
            name = "facility_services",
            joinColumns = @JoinColumn(name = "airport_iata_code")
    )
    @Column(name = "service")
    private List<String> services = new ArrayList<>();

    public Facilities() {}

    public Facilities(Integer terminalCount, List<String> services, Integer gateCount) {
        this.terminalCount = terminalCount;
        this.services = services;
        this.gateCount = gateCount;
    }

    public Integer getTerminalCount() {
        return terminalCount;
    }

    public Integer getGateCount() {
        return gateCount;
    }

    public List<String> getServices() {
        return services;
    }
}
