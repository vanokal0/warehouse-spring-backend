package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.LotBooking;
import com.vanokal0.warehouse_backend.model.OrderProduct;
import com.vanokal0.warehouse_backend.model.ProductPlacement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductPlacementRepository extends JpaRepository<ProductPlacement, Integer>, JpaSpecificationExecutor<ProductPlacement> {
    Optional<ProductPlacement> findByPlacementNumber(String placementNumber);
   boolean existsProductPlacementByPlacementNumber(String placementNumber);
   boolean existsByLotBooking(LotBooking lotBooking);
   boolean existsByOrderProduct(OrderProduct orderProduct);
   List<ProductPlacement> findByOrderProduct(OrderProduct orderProduct);
}

