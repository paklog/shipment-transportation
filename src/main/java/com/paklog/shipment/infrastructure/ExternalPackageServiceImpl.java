package com.paklog.shipment.infrastructure;

import com.paklog.shipment.adapter.ExternalPackageService;
import com.paklog.shipment.domain.Package;
import org.springframework.stereotype.Service;

@Service
public class ExternalPackageServiceImpl implements ExternalPackageService {

    @Override
    public Package getPackageDetails(String packageId) {
        // Simulate an external API call or database lookup
        // In a real application, this would involve HTTP clients, database queries, etc.
        if (packageId.equals("pkg-123")) {
            return new Package(5.0, 20.0, 15.0, 10.0, "BOX");
        } else if (packageId.equals("pkg-456")) {
            return new Package(1.0, 5.0, 5.0, 2.0, "ENVELOPE");
        } else {
            throw new RuntimeException("Package not found: " + packageId);
        }
    }
}
