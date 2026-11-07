package com.sidis.maintenanceservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

public class UsedPartRedis {
    private String partSerialNumber;
    private Integer quantity;
    private Double price;

    protected UsedPartRedis() {}

    public UsedPartRedis(String partSerialNumber, Integer quantity, Double price) {
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
