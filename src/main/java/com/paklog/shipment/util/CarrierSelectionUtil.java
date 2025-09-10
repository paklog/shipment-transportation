package com.paklog.shipment.util;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Package;

public class CarrierSelectionUtil {
    public static CarrierName selectBestCarrier(Package packageDetail) {
        // Simplified carrier selection logic
        if (packageDetail.getWeight() < 5) {
            return CarrierName.FEDEX;
        } else {
            return CarrierName.UPS;
        }
    }
}