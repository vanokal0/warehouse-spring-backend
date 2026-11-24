package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.ProductManufacturer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductManufacturerRepository extends JpaRepository<ProductManufacturer, Integer> {
    Optional<ProductManufacturer> findByCompanyName(String companyName);
}
