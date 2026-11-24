package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.ConcreteProductDTO;
import com.vanokal0.warehouse_backend.mappers.ConcreteProductMapper;
import com.vanokal0.warehouse_backend.model.ConcreteProduct;
import com.vanokal0.warehouse_backend.model.Product;
import com.vanokal0.warehouse_backend.model.ProductManufacturer;
import com.vanokal0.warehouse_backend.model.ProductType;
import com.vanokal0.warehouse_backend.repositories.ConcreteProductRepository;
import com.vanokal0.warehouse_backend.repositories.ProductManufacturerRepository;
import com.vanokal0.warehouse_backend.repositories.ProductRepository;
import com.vanokal0.warehouse_backend.repositories.ProductTypeRepository;
import com.vanokal0.warehouse_backend.services.BaseSpecificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class ConcreteProductService extends BaseSpecificationService<ConcreteProduct> {

    private final ProductTypeRepository productTypeRepository;
    private final ProductRepository productRepository;
    private final ProductManufacturerRepository productManufacturerRepository;
    private final ConcreteProductRepository concreteProductRepository;
    private final ConcreteProductMapper concreteProductMapper;

    public ConcreteProductService(ProductTypeRepository productTypeRepository, ProductRepository productRepository, ProductManufacturerRepository productManufacturerRepository, ConcreteProductRepository repo, ConcreteProductMapper mapper) {
        this.productTypeRepository = productTypeRepository;
        this.productRepository = productRepository;
        this.productManufacturerRepository = productManufacturerRepository;
        this.concreteProductRepository = repo;
        this.concreteProductMapper = mapper;
    }

    @Override
    protected Map<String, String> getFieldMapping() {
        return Map.of(
                "concreteProductName", "productName",
                "productName", "product.productName",
                "type", "product.productType.typeName",
                "value", "estimatedValue",
                "weight", "weight",
                "manufacturer", "manufacturer.companyName"
        );
    }

    public Map<String, Object> getPagedProducts(
            int page, int pageSize,
            List<String> sortFields, List<String> sortOrders,
            List<String> filterFields, List<String> filterOperators, List<String> filterValues
    ) {
        Pageable pageable = PageRequest.of(page, pageSize, buildSort(sortFields, sortOrders));
        Specification<ConcreteProduct> spec = buildSpec(filterFields, filterOperators, filterValues);

        Page<ConcreteProduct> pageResult = concreteProductRepository.findAll(spec, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("items", pageResult.getContent().stream().map(concreteProductMapper::toDTO).toList());
        response.put("total", pageResult.getTotalElements());
        return response;
    }

    public List<ConcreteProductDTO> getAllConcreteProducts() {
        return concreteProductRepository.findAll().stream().map(concreteProductMapper::toDTO).toList();
    }

    public ConcreteProductDTO createConcreteProduct(ConcreteProductDTO dto) {

        ProductType type = productTypeRepository
                .findByTypeName(dto.getType())
                .orElseGet(() -> {
                    ProductType newType = new ProductType();
                    newType.setTypeName(dto.getType());
                    return productTypeRepository.save(newType);
                });


        Product product = productRepository
                .findByProductName(dto.getProductName())
                .orElseGet(() -> {
                    Product newProduct = new Product();
                    newProduct.setProductName(dto.getProductName());
                    newProduct.setProductType(type);
                    return productRepository.save(newProduct);
                });


        ProductManufacturer manufacturer = productManufacturerRepository
                .findByCompanyName(dto.getManufacturer())
                .orElseGet(() -> {
                    ProductManufacturer newMan = new ProductManufacturer();
                    newMan.setCompanyName(dto.getManufacturer());
                    return productManufacturerRepository.save(newMan);
                });


        ConcreteProduct newConcrete = new ConcreteProduct();
        newConcrete.setProductName(dto.getConcreteProductName());
        newConcrete.setEstimatedValue(dto.getValue());
        newConcrete.setWeight(dto.getWeight());
        newConcrete.setProduct(product);
        newConcrete.setManufacturer(manufacturer);

        return concreteProductMapper.toDTO(concreteProductRepository.save(newConcrete));
    }


    public ConcreteProductDTO updateConcreteProduct(Integer id, ConcreteProductDTO dto) {
        ConcreteProduct concreteProduct = concreteProductRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Конкретний продукт не знайдено"));

        // Тип
        ProductType type = productTypeRepository
                .findByTypeName(dto.getType())
                .orElseGet(() -> {
                    ProductType newType = new ProductType();
                    newType.setTypeName(dto.getType());
                    return productTypeRepository.save(newType);
                });

        // Базовий продукт
        Product product = productRepository
                .findByProductName(dto.getProductName())
                .orElseGet(() -> {
                    Product newProduct = new Product();
                    newProduct.setProductName(dto.getProductName());
                    newProduct.setProductType(type);
                    return productRepository.save(newProduct);
                });

        // Виробник
        ProductManufacturer manufacturer = productManufacturerRepository
                .findByCompanyName(dto.getManufacturer())
                .orElseGet(() -> {
                    ProductManufacturer newMan = new ProductManufacturer();
                    newMan.setCompanyName(dto.getManufacturer());
                    return productManufacturerRepository.save(newMan);
                });

        // Оновлення
        concreteProduct.setProductName(dto.getConcreteProductName());
        concreteProduct.setEstimatedValue(dto.getValue());
        concreteProduct.setWeight(dto.getWeight());
        concreteProduct.setProduct(product);
        concreteProduct.setManufacturer(manufacturer);

        return concreteProductMapper.toDTO(concreteProductRepository.save(concreteProduct));
    }
}
