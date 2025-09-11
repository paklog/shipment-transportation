package com.paklog.shipment.adapter;

import com.paklog.shipment.domain.*;
import com.paklog.shipment.domain.exception.CarrierException;

import java.util.Optional;

public interface ICarrierAdapter {

    String createShipment(com.paklog.shipment.domain.Package packageInfo) throws CarrierException;

    Optional<TrackingUpdate> getTrackingStatus(TrackingNumber trackingNumber) throws CarrierException;

    ShippingCost rateLoad(Load load) throws CarrierException;

    boolean tenderLoad(Load load) throws CarrierException;

    String schedulePickup(Load load) throws CarrierException;

    CarrierName getCarrierName();
}