package com.paklog.shipment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShipmentTransportationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShipmentTransportationApplication.class, args);
    }
}