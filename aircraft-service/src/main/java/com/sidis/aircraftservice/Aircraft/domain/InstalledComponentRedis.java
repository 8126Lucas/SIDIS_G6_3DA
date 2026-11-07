package com.sidis.aircraftservice.Aircraft.domain;

public class InstalledComponentRedis {
    private String serialNumber;
    private String partNumber;
    private String status;

    public InstalledComponentRedis() {}

    public InstalledComponentRedis(String serialNumber, String partNumber, String status) {
        this.serialNumber = serialNumber;
        this.partNumber = partNumber;
        this.status = status;
    }

    public String getSerialNumber() { return this.serialNumber; }
    public String getPartNumber() { return this.partNumber; }
    public String getStatus() { return this.status; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public void setStatus(String status) { this.status = status; }
}