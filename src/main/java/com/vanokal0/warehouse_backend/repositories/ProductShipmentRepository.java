package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.LotBooking;
import com.vanokal0.warehouse_backend.model.OrderProduct;
import com.vanokal0.warehouse_backend.model.ProductPlacement;
import com.vanokal0.warehouse_backend.model.ProductShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductShipmentRepository extends JpaRepository<ProductShipment, Integer>, JpaSpecificationExecutor<ProductShipment> {
    boolean existsByShipmentNumber(String shipmentNumber);
    Optional<ProductShipment> findByShipmentNumber(String shipmentNumber);
    boolean existsByLotBooking(LotBooking lotBooking);
    boolean existsByOrderProduct(OrderProduct orderProduct);
    List<ProductShipment> findByOrderProduct(OrderProduct orderProduct);
}
