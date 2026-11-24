package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTypeRepository extends JpaRepository<PaymentType, Integer> {
    Optional<PaymentType> findByTypeName(String typeName);
}
