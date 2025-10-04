package com.paklog.shipment.config;

import com.paklog.shipment.domain.services.CarrierSelectionService;
import com.paklog.shipment.domain.CarrierSelectionStrategy;
import com.paklog.shipment.domain.services.DefaultCarrierSelectionStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CarrierConfiguration {

    @Bean
    public CarrierSelectionStrategy carrierSelectionStrategy() {
        return new DefaultCarrierSelectionStrategy();
    }

    @Bean
    public CarrierSelectionService carrierSelectionService(CarrierSelectionStrategy strategy) {
        return new CarrierSelectionService(strategy);
    }
}
