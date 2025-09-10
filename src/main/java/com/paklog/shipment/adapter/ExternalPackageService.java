package com.paklog.shipment.adapter;

import com.paklog.shipment.domain.Package;

public interface ExternalPackageService {
    Package getPackageDetails(String packageId);
}
