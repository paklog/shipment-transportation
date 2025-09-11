package com.paklog.shipment.domain.repository;

import com.paklog.shipment.domain.Load;
import com.paklog.shipment.domain.LoadId;

import java.util.Optional;

public interface ILoadRepository {
    void save(Load load);

    Optional<Load> findById(LoadId loadId);
}
