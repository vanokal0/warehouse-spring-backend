package com.vanokal0.warehouse_backend.repositories;

import com.vanokal0.warehouse_backend.model.LoadingDock;
import com.vanokal0.warehouse_backend.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Integer> {
    List<ParkingLot> findByLoadingDockId(Integer loadingDockId);
    Optional<ParkingLot> findByLotNumber(String lotNumber);

    Integer countByLoadingDock(LoadingDock loadingDock);
}
