package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType,Integer> {
    Optional<ProductType> findByTypeName(String typeName);
}
