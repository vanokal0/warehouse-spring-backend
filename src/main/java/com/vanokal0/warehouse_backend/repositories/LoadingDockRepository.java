package com.vanokal0.warehouse_backend.repositories;

import com.vanokal0.warehouse_backend.model.LoadingDock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoadingDockRepository extends JpaRepository<LoadingDock, Integer> {
    List<LoadingDock> findByWarehouseId(Integer warehouseId);
    Optional<LoadingDock> findByDockNumber(String dockNumber);
}
