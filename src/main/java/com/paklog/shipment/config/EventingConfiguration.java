package com.paklog.shipment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ShipmentEventProperties.class, OutboxProperties.class, TrackingJobProperties.class})
public class EventingConfiguration {
}
