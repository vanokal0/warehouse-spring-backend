package com.vanokal0.warehouse_backend.repositories;

import com.vanokal0.warehouse_backend.model.ConcreteProduct;
import com.vanokal0.warehouse_backend.model.Order;
import com.vanokal0.warehouse_backend.model.OrderProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderProductRepository extends JpaRepository<OrderProduct, Integer> {
    List<OrderProduct> findOrderProductsByOrderId(Integer orderId);
    Optional<OrderProduct> findByConcreteProductAndOrder(ConcreteProduct concreteProduct, Order order);
}
