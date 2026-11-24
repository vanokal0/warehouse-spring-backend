package com.vanokal0.warehouse_backend.repositories;

import com.vanokal0.warehouse_backend.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    Optional<Warehouse> findByNumber(String warehouseNumber);
    boolean existsByNumber(String number);

}
