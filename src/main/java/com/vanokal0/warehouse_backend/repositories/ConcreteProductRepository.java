package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.ConcreteProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConcreteProductRepository extends JpaRepository<ConcreteProduct, Integer>, JpaSpecificationExecutor<ConcreteProduct> {
    Optional<ConcreteProduct> findByProductName(String productName);
}
