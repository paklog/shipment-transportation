package com.paklog.shipment.adapter;

import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.exception.CarrierException;

public interface ICarrierAdapter {
    CarrierInfo createShipment(String packageId, String orderId) throws CarrierException;
    TrackingUpdate getTrackingStatus(TrackingNumber trackingNumber) throws CarrierException;
}