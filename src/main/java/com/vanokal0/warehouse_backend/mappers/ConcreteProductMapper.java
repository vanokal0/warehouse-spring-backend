package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.ConcreteProductDTO;
import com.vanokal0.warehouse_backend.model.ConcreteProduct;
import com.vanokal0.warehouse_backend.model.Product;
import com.vanokal0.warehouse_backend.model.ProductManufacturer;
import com.vanokal0.warehouse_backend.model.ProductType;
import org.springframework.stereotype.Component;

@Component
public class ConcreteProductMapper implements ModelDTOMapper<ConcreteProduct, ConcreteProductDTO>{

    @Override
    public ConcreteProductDTO toDTO(ConcreteProduct concreteProduct) {
        Product product = concreteProduct.getProduct();
        ProductType productType = concreteProduct.getProduct().getProductType();
        ProductManufacturer manufacturer = concreteProduct.getManufacturer();
        return new ConcreteProductDTO(
                concreteProduct.getId(),
                concreteProduct.getProductName(),
                product.getProductName(),
                productType.getTypeName(),
                concreteProduct.getEstimatedValue(),
                concreteProduct.getWeight(),
                manufacturer.getCompanyName()
                )
        ;
    }
}
