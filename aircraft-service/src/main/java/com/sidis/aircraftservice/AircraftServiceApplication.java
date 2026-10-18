package com.sidis.aircraftservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
public class AircraftServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AircraftServiceApplication.class, args);
    }
}
