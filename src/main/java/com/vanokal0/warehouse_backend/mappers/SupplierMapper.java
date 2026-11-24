package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.SupplierDTO;
import com.vanokal0.warehouse_backend.model.Supplier;
import com.vanokal0.warehouse_backend.repositories.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper implements ModelDTOMapper<Supplier, SupplierDTO> {
    @Override
    public SupplierDTO toDTO(Supplier supplier) {
        return new SupplierDTO(supplier.getCompanyName(), supplier.getEmail());
    }
}
