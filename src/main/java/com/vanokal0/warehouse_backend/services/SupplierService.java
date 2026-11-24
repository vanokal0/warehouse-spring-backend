package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.SupplierDTO;
import com.vanokal0.warehouse_backend.mappers.SupplierMapper;
import com.vanokal0.warehouse_backend.model.Supplier;
import com.vanokal0.warehouse_backend.repositories.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierService {
    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Autowired
    public SupplierService(SupplierRepository supplierRepository, SupplierMapper supplierMapper) {
        this.supplierRepository = supplierRepository;
        this.supplierMapper = supplierMapper;
    }

    public List<SupplierDTO> getAllSuppliers(){
        return supplierRepository.findAll().stream().map(supplierMapper::toDTO).toList();
    }

    public List<String> getAllSupplierNames() {
        return supplierRepository.findAll()
                .stream()
                .map(Supplier::getCompanyName)
                .toList();
    }
}
