package com.paklog.shipment.domain.repository;

import com.paklog.shipment.domain.CarrierName;
import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;
import com.paklog.shipment.domain.LoadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ILoadRepository {
    void save(Load load);

    Optional<Load> findById(LoadId loadId);

    Page<Load> findAll(Pageable pageable, LoadStatus status, CarrierName carrierName);

    void delete(Load load);
}
