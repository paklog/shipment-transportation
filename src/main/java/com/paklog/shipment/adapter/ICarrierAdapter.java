package com.paklog.shipment.adapter;

import com.paklog.shipment.domain.CarrierInfo;
import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.OrderId;
import com.paklog.shipment.domain.ShippingCost;
import com.paklog.shipment.domain.TrackingNumber;
import com.paklog.shipment.domain.TrackingUpdate;
import com.paklog.shipment.domain.exception.CarrierException;
import java.util.Optional;

public interface ICarrierAdapter {

    CarrierInfo createShipment(com.paklog.shipment.domain.Package packageInfo,
                               OrderId orderId,
                               String packageId) throws CarrierException;

    Optional<TrackingUpdate> getTrackingStatus(TrackingNumber trackingNumber) throws CarrierException;

    ShippingCost rateLoad(Load load) throws CarrierException;

    boolean tenderLoad(Load load) throws CarrierException;

    String schedulePickup(Load load) throws CarrierException;

    CarrierName getCarrierName();
}
