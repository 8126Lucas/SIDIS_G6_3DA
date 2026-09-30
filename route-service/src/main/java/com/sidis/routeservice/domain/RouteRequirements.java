package com.sidis.routeservice.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class RouteRequirements {
    private Double requiredRange;
    private Integer requiredCapacity;

    public RouteRequirements() {}

    public RouteRequirements(Double requiredRange, Integer requiredCapacity) {
        this.requiredRange = requiredRange;
        this.requiredCapacity = requiredCapacity;
    }

    public Double getRequiredRange() {
        return requiredRange;
    }

    public Integer getRequiredCapacity() {
        return requiredCapacity;
    }
}
