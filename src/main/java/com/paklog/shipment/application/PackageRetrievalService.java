package com.paklog.shipment.application;

import com.paklog.shipment.domain.Package;
import org.springframework.stereotype.Service;

@Service
public class PackageRetrievalService {

    private final com.paklog.shipment.adapter.ExternalPackageService externalPackageService;

    public PackageRetrievalService(com.paklog.shipment.adapter.ExternalPackageService externalPackageService) {
        this.externalPackageService = externalPackageService;
    }

    public Package getPackageDetails(String packageId) {
        return externalPackageService.getPackageDetails(packageId);
    }
}
