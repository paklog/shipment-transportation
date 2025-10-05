package com.paklog.shipment.infrastructure.config;

import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.repository.ILoadRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    private static final LoadId UNASSIGNED_LOAD_ID = LoadId.of("00000000-0000-0000-0000-000000000000");

    // @Bean
    // public ApplicationRunner systemInitializer(ILoadRepository loadRepository) {
    //     return args -> {
    //         if (loadRepository.findById(UNASSIGNED_LOAD_ID).isEmpty()) {
    //             // Load unassignedLoad = new Load(UNASSIGNED_LOAD_ID);
    //             // loadRepository.save(unassignedLoad);
    //         }
    //     };
    // }
}
