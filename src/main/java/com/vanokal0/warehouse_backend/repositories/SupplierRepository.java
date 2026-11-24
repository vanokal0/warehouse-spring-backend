package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    Optional<Supplier> findByCompanyName(String companyName);
}
