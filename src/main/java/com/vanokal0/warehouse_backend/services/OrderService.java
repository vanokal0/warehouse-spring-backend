package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.OrderDTO;
import com.vanokal0.warehouse_backend.dto.OrderProductDTO;
import com.vanokal0.warehouse_backend.mappers.OrderMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.OperationNotSupportedException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService extends BaseSpecificationService<Order> {
    private  final Integer MANAGER_ID = 110;
    private final ActionLogService actionLogService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final SupplierRepository supplierRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final PaymentTypeRepository paymentTypeRepository;
    private final ConcreteProductRepository concreteProductRepository;
    private final OrderProductRepository orderProductRepository;
    private final ProductPlacementRepository productPlacementRepository;
    private final ProductShipmentRepository productShipmentRepository;

    public OrderService(ActionLogService actionLogService, OrderRepository orderRepository, OrderMapper orderMapper, SupplierRepository supplierRepository, OrderStatusRepository orderStatusRepository, PaymentTypeRepository paymentTypeRepository, ConcreteProductRepository concreteProductRepository, OrderProductRepository orderProductRepository,
                        ProductPlacementRepository productPlacementRepository,
                        ProductShipmentRepository productShipmentRepository){
        this.actionLogService = actionLogService;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.supplierRepository = supplierRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.paymentTypeRepository = paymentTypeRepository;
        this.concreteProductRepository = concreteProductRepository;
        this.orderProductRepository = orderProductRepository;
        this.productPlacementRepository = productPlacementRepository;
        this.productShipmentRepository = productShipmentRepository;
    }

    @Override
    protected Map<String, String> getFieldMapping() {
        return Map.of(
                "number", "orderNumber",
                "cost", "orderCost",
                "orderDateTime", "orderDateTime",
                "estimatedArrivalDate", "estimatedArrivalDate",
                "estimatedShipmentDate", "estimatedShipmentDate",
                "paymentType", "paymentType.typeName",
                "orderStatus", "orderStatus.statusName",
                "supplier", "supplier.companyName"
        );
    }

    public Map<String, Object> getPagedOrders(
            int page, int pageSize,
            List<String> sortFields, List<String> sortOrders,
            List<String> filterFields, List<String> filterOperators, List<String> filterValues, boolean all
    ) {
        Pageable pageable = all ? Pageable.unpaged() : PageRequest.of(page, pageSize, buildSort(sortFields, sortOrders));
        Specification<Order> spec = buildSpec(filterFields, filterOperators, filterValues);

        Page<Order> pageResult = orderRepository.findAll(spec, pageable);
        List<OrderDTO> dtos = pageResult.getContent().stream().map(orderMapper::toDTO).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("items", dtos);
        response.put("total", pageResult.getTotalElements());
        return response;
    }

    @Transactional
    public OrderDTO createOrder(OrderDTO dto) {
        Supplier supplier = supplierRepository.findByCompanyName(dto.getSupplier())
                .orElseGet(() -> {
                    Supplier newSupplier = new Supplier();
                    newSupplier.setCompanyName(dto.getSupplier());
                    newSupplier.setEmail("unknown");
                    return supplierRepository.save(newSupplier);
                });

        OrderStatus orderStatus = orderStatusRepository.findByStatusName(dto.getOrderStatus())
                .orElseThrow(() -> new NoSuchElementException("Статус замовлення не знайдено"));



        PaymentType paymentType = paymentTypeRepository.findByTypeName(dto.getPaymentType())
                    .orElseGet(() -> {
                        PaymentType newPaymentType = new PaymentType();
                        newPaymentType.setTypeName(dto.getPaymentType());
                        return paymentTypeRepository.save(newPaymentType);
                    });


        Order order = new Order();
        order.setOrderNumber(generateOrderNumber(supplier.getCompanyName()));
        order.setOrderCost(dto.getCost());
        order.setOrderDateTime(dto.getOrderDateTime());
        order.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
        order.setEstimatedShipmentDate(dto.getEstimatedShipmentDate());
        order.setSupplier(supplier);
        order.setOrderStatus(orderStatus);
        order.setPaymentType(paymentType);

        Order savedOrder = orderRepository.save(order);

        for (OrderProductDTO prod : dto.getProducts()) {
            ConcreteProduct concreteProduct = concreteProductRepository.findByProductName(prod.getConcreteProduct().getConcreteProductName())
                    .orElseThrow(() -> new NoSuchElementException("Продукт не знайдено: " + prod.getConcreteProduct().getConcreteProductName()));

            OrderProduct orderProduct = new OrderProduct();
            orderProduct.setOrder(savedOrder);
            orderProduct.setConcreteProduct(concreteProduct);
            orderProduct.setProductQuantity(prod.getQuantity());

            orderProductRepository.save(orderProduct);
        }

        Map<String, Object> newState = toAuditState(savedOrder);

        actionLogService.logAction(
                MANAGER_ID,
                "CREATE",
                "Order",
                savedOrder.getId(),
                null,
                newState
        );

        return orderMapper.toDTO(savedOrder);
    }

    // OperationNotSupportedException is checked, so rollback must be enabled for it explicitly
    @Transactional(rollbackFor = OperationNotSupportedException.class)
    public void updateOrder(String orderNumber, OrderDTO dto) throws OperationNotSupportedException {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("Замовлення не знайдено: " + orderNumber));

        Map<String, Object> prevState = toAuditState(order);

        Supplier supplier = supplierRepository.findByCompanyName(dto.getSupplier())
                .orElseGet(() -> {
                    Supplier newSupplier = new Supplier();
                    newSupplier.setCompanyName(dto.getSupplier());
                    newSupplier.setEmail("unknown");
                    return supplierRepository.save(newSupplier);
                });

        OrderStatus orderStatus = orderStatusRepository.findByStatusName(dto.getOrderStatus())
                .orElseThrow(() -> new NoSuchElementException("Статус замовлення не знайдено"));

        PaymentType paymentType = paymentTypeRepository.findByTypeName(dto.getPaymentType())
                .orElseGet(() -> {
                    PaymentType newPaymentType = new PaymentType();
                    newPaymentType.setTypeName(dto.getPaymentType());
                    return paymentTypeRepository.save(newPaymentType);
                });

        order.setOrderCost(dto.getCost());
        order.setOrderDateTime(dto.getOrderDateTime());
        order.setEstimatedArrivalDate(dto.getEstimatedArrivalDate());
        order.setEstimatedShipmentDate(dto.getEstimatedShipmentDate());
        order.setSupplier(supplier);
        order.setOrderStatus(orderStatus);
        order.setPaymentType(paymentType);



        orderRepository.save(order);

        Map<String, Object> newState = toAuditState(order);

        actionLogService.logAction(
                MANAGER_ID,
                "UPDATE",
                "Order",
                order.getId(),
                prevState,
                newState
        );


        List<OrderProduct> existingProducts = orderProductRepository.findOrderProductsByOrderId(order.getId());

        Map<String, OrderProduct> existingProductMap = existingProducts.stream()
                .collect(Collectors.toMap(
                        op -> op.getConcreteProduct().getProductName(),
                        Function.identity()
                ));

        Set<String> incomingProductNames = dto.getProducts().stream()
                .map(p -> p.getConcreteProduct().getConcreteProductName())
                .collect(Collectors.toSet());


        for (OrderProductDTO prodDTO : dto.getProducts()) {
            String concreteName = prodDTO.getConcreteProduct().getConcreteProductName();

            ConcreteProduct concreteProduct = concreteProductRepository.findByProductName(concreteName)
                    .orElseThrow(() -> new NoSuchElementException("Продукт не знайдено: " + concreteName));

            if (existingProductMap.containsKey(concreteName)) {
                OrderProduct existing = existingProductMap.get(concreteName);
                existing.setProductQuantity(prodDTO.getQuantity());
                orderProductRepository.save(existing);
            } else {
                OrderProduct newProduct = new OrderProduct();
                newProduct.setOrder(order);
                newProduct.setConcreteProduct(concreteProduct);
                newProduct.setProductQuantity(prodDTO.getQuantity());
                orderProductRepository.save(newProduct);
            }
        }

        for (OrderProduct existing : existingProducts) {
            String productName = existing.getConcreteProduct().getProductName();
            if (!incomingProductNames.contains(productName)) {
                if(productPlacementRepository.existsByOrderProduct(existing) || productShipmentRepository.existsByOrderProduct(existing) ){
                    throw new OperationNotSupportedException("Неможливо видалити продукт з наявними операціями.");
                }
                orderProductRepository.delete(existing);
            }
        }
    }

    @Transactional(rollbackFor = OperationNotSupportedException.class)
    public void deleteOrder(String orderNumber) throws OperationNotSupportedException {
        Order  order = orderRepository.findByOrderNumber(orderNumber).orElseThrow();

        if(!Objects.equals(order.getOrderStatus().getStatusName(), "CREATED")){
            throw new OperationNotSupportedException("Неможливо видалити замовлення з статусом відмінним від CREATED.");
        }

        List<OrderProduct> orderProducts =  orderProductRepository.findOrderProductsByOrderId(order.getId());
            orderProducts.forEach(orderProduct-> {
                if(productShipmentRepository.existsByOrderProduct(orderProduct) || productPlacementRepository.existsByOrderProduct(orderProduct)){

                    throw new IllegalStateException("Неможливо видалити замовлення з пов'язаними операціями");

                }
            });

        Map<String, Object> prevState = toAuditState(order);

        orderProductRepository.deleteAll(orderProducts);
        orderRepository.delete(order);

//        actionLogService.logAction(
//                MANAGER_ID,
//                "DELETE",
//                "Order",
//                order.getId(),
//                prevState,
//                null
//        );
    }

    private Map<String, Object> toAuditState(Order order) {
        Map<String, Object> state = new HashMap<>();
        state.put("number", order.getOrderNumber());
        state.put("cost", order.getOrderCost());
        state.put("status", order.getOrderStatus().getStatusName());
        state.put("supplier", order.getSupplier().getCompanyName());
        state.put("paymentType", order.getPaymentType().getTypeName());
        state.put("orderDateTime", order.getOrderDateTime());
        state.put("estimatedArrivalDate", order.getEstimatedArrivalDate());
        state.put("estimatedShipmentDate", order.getEstimatedShipmentDate());
        return state;
    }


    private String generateOrderNumber(String supplierCompanyName) {
        Random random = new Random();
        if (supplierCompanyName == null || supplierCompanyName.length() < 2) {
            throw new IllegalArgumentException("Supplier company name must have at least 2 characters");
        }

        String firstTwoLetters = supplierCompanyName.substring(0, 3).toUpperCase();
        String orderNumber;

        do {
            int randomDigits = 100 + random.nextInt(900);
            orderNumber = firstTwoLetters + "-" + randomDigits;
        } while (orderRepository.existsByOrderNumber(orderNumber));

        return orderNumber;
    }

}
