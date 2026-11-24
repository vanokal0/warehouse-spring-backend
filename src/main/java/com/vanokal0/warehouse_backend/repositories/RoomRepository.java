package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.ProductType;
import com.vanokal0.warehouse_backend.model.Room;
import com.vanokal0.warehouse_backend.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    List<Room> findByProductTypeId(Integer productTypeId);
    List<Room> findByWarehouseId(Integer warehouseId);
    List<Room> findRoomByWarehouseAndProductType(Warehouse warehouse, ProductType productType);
    Optional<Room> findByRoomNumber(String roomNumber);
    Integer countByWarehouse(Warehouse warehouse);
}
