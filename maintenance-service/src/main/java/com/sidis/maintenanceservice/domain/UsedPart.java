package com.sidis.maintenanceservice.domain;

import jakarta.persistence.*;

@Embeddable
public class UsedPart {

    @Column(nullable = false)
    private String partSerialNumber;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double price;

    protected UsedPart() {}

    public UsedPart(String partSerialNumber, Integer quantity, Double price) {
        this.partSerialNumber = partSerialNumber;
        this.quantity = quantity;
        this.price = price;
    }

    public String getPartSerialNumber() {
        return partSerialNumber;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Double getPrice() {
        return price;
    }
}
