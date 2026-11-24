package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.StoredProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StoredProductRepository extends JpaRepository<StoredProduct, Integer> {

}
