package com.paklog.shipment.util;

public class PackageNameConverter {

    public static com.paklog.shipment.domain.Package getPackageDetails(String packageId) {
        // TODO: Implement actual logic to get package details
        return new com.paklog.shipment.domain.Package(1.0, 10.0, 10.0, 10.0, "BOX");
    }
    public static String toExpectedPackage(String packageName) {
        return "main.java." + packageName;
    }
    
    public static String toActualPackage(String packageName) {
        return packageName.replace("main.java.", "");
    }
}