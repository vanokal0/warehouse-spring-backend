package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.ProductShipmentDTO;
import com.vanokal0.warehouse_backend.mappers.ProductShipmentMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ProductShipmentService extends BaseSpecificationService<ProductShipment> {

    private final Integer OPERATOR_ID = 117;
    private final LotBookingRepository lotBookingRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final ProductShipmentRepository productShipmentRepository;
    private final OrderProductRepository orderProductRepository;
    private final StoredProductRepository storedProductRepository;
    private final OrderRepository orderRepository;
    private final ConcreteProductRepository concreteProductRepository;
    private final ProductShipmentMapper productShipmentMapper;
    private final OrderStatusRepository orderStatusRepository;
    private final ActionLogService actionLogService;

    @Autowired
    public ProductShipmentService(
            LotBookingRepository lotBookingRepository,
            EmployeeRepository employeeRepository,
            RoomRepository roomRepository,
            ProductShipmentRepository productShipmentRepository,
            OrderProductRepository orderProductRepository,
            StoredProductRepository storedProductRepository,
            OrderRepository orderRepository,
            ConcreteProductRepository concreteProductRepository,
            ProductShipmentMapper productShipmentMapper,
            OrderStatusRepository orderStatusRepository,
            ActionLogService actionLogService
    ) {
        this.lotBookingRepository = lotBookingRepository;
        this.employeeRepository = employeeRepository;
        this.roomRepository = roomRepository;
        this.productShipmentRepository = productShipmentRepository;
        this.orderProductRepository = orderProductRepository;
        this.storedProductRepository = storedProductRepository;
        this.orderRepository = orderRepository;
        this.concreteProductRepository = concreteProductRepository;
        this.productShipmentMapper = productShipmentMapper;
        this.orderStatusRepository = orderStatusRepository;
        this.actionLogService = actionLogService;
    }

    @Override
    protected Map<String, String> getFieldMapping() {
        return Map.of(
                "number", "shipmentNumber",
                "product", "storedProduct.concreteProduct.productName",
                "roomNumber", "storedProduct.room.roomNumber",
                "lotNumber", "lotBooking.parkingLot.lotNumber",
                "lotBookingNumber", "lotBooking.bookingNumber",
                "employee", "employee.firstName"
        );
    }

    public List<ProductShipmentDTO> getByOrderAndProduct(String orderNumber, String productName) {
        Specification<ProductShipment> spec = Specification
                .where(fieldEquals("orderProduct.order.orderNumber", orderNumber))
                .and(fieldEquals("orderProduct.concreteProduct.productName", productName));

        return productShipmentRepository.findAll(spec).stream()
                .map(productShipmentMapper::toDTO)
                .toList();
    }

    @Transactional
    public ProductShipmentDTO createShipment(ProductShipmentDTO dto) {
        LotBooking booking = lotBookingRepository.findByBookingNumber(dto.getLotBookingNumber())
                .orElseThrow(() -> new NoSuchElementException("Бронювання не знайдено."));


        List<String> splitName = Arrays.stream(dto.getEmployee().split(" ")).toList();
        Employee employee = employeeRepository.findByFirstNameAndLastName(splitName.getFirst(), splitName.getLast())
                .orElseThrow(() -> new NoSuchElementException("Оператора не знайдено."));

        Room room = roomRepository.findByRoomNumber(dto.getRoomNumber())
                .orElseThrow(() -> new NoSuchElementException("Кімнату не знайдено."));

        Order order = orderRepository.findByOrderNumber(dto.getOrderNumber())
                .orElseThrow(() -> new NoSuchElementException("Замовлення не знайдено."));

        if(booking.getSupplier().getCompanyName().equals(order.getSupplier().getCompanyName())){
            throw new NoSuchElementException("Постачальник бронювання не відповідає постачальнику замовлення.");
        }

        ConcreteProduct concreteProduct = concreteProductRepository.findByProductName(dto.getProduct())
                .orElseThrow(() -> new NoSuchElementException("Продукт не знайдено."));

        OrderProduct orderProduct = orderProductRepository.findByConcreteProductAndOrder(concreteProduct, order)
                .orElseThrow(() -> new NoSuchElementException("Продукту в замовлені не знайдено."));

        if(orderProduct.getProductQuantity()< dto.getQuantity()){
            throw new  NoSuchElementException("Кількість продуктів відправлення більше за вказану у замовлені.");
        }

        List<ProductShipment> shipments =  productShipmentRepository.findByOrderProduct(orderProduct);
        int reducedQuantity = shipments.stream().reduce(0, (acc, shp)-> acc + shp.getProductQuantity(), Integer::sum);

        if(reducedQuantity+dto.getQuantity()>orderProduct.getProductQuantity()){
            throw new NoSuchElementException("Сумарна кількість продуктів відправлень більше за вказану у замовлені.");
        }
        if(orderProduct.getProductQuantity()< dto.getQuantity()){
            throw new  NoSuchElementException("Кількість продуктів відправлення більше за вказану у замовлені.");
        }



        StoredProduct storedProduct = new StoredProduct();
        storedProduct.setRoom(room);
        storedProduct.setConcreteProduct(orderProduct.getConcreteProduct());
        storedProduct.setProductQuantity(dto.getQuantity());


        if(!room.getProductType().getTypeName().equals(concreteProduct.getProduct().getProductType().getTypeName())){
            throw new NoSuchElementException("Обрана кімната не містить цієї продукції");
        }
        ProductShipment shipment = new ProductShipment();
        shipment.setShipmentNumber(generateUniqueShipmentNumber());
        shipment.setLotBooking(booking);
        shipment.setEmployee(employee);
        shipment.setOrderProduct(orderProduct);
        shipment.setStoredProduct(storedProductRepository.save(storedProduct));
        shipment.setProductQuantity(dto.getQuantity());
        shipment.setShipmentDateTime(Instant.now());

        productShipmentRepository.save(shipment);

        if (orderProductRepository.findOrderProductsByOrderId(order.getId()).stream()
                .filter(op -> !productShipmentRepository.existsByOrderProduct(op))
                .toList().isEmpty()) {
            order.setOrderStatus(orderStatusRepository.findByStatusName("SHIPPED").orElseThrow());
            orderRepository.save(order);
        }

        Map<String, Object> newState = toAuditState(shipment);

        actionLogService.logAction(
                OPERATOR_ID,
                "CREATE",
                "ProductShipment",
                shipment.getId(),
                null,
                newState
        );

        dto.setNumber(shipment.getShipmentNumber());
        dto.setLotNumber(booking.getParkingLot().getLotNumber());

        return dto;
    }

    public ProductShipmentDTO updateShipment(String number, ProductShipmentDTO dto) {
        ProductShipment shipment = productShipmentRepository.findByShipmentNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Відправлення не знайдено"));

        Map<String, Object> prevState = toAuditState(shipment);

        LotBooking booking = lotBookingRepository.findByBookingNumber(dto.getLotBookingNumber())
                .orElseThrow(() -> new NoSuchElementException("Бронювання не знайдено"));

        List<String> splitName = Arrays.stream(dto.getEmployee().split(" ")).toList();
        Employee employee = employeeRepository.findByFirstNameAndLastName(splitName.getFirst(), splitName.getLast())
                .orElseThrow(() -> new NoSuchElementException("Оператора не знайдено"));

        Order order = orderRepository.findByOrderNumber(dto.getOrderNumber())
                .orElseThrow(() -> new NoSuchElementException("Замовлення не знайдено"));

        if(booking.getSupplier().getCompanyName().equals(order.getSupplier().getCompanyName())){
            throw new NoSuchElementException("Постачальник бронювання не відповідає постачальнику замовлення.");
        }

        ConcreteProduct concreteProduct = concreteProductRepository.findByProductName(dto.getProduct())
                .orElseThrow(() -> new NoSuchElementException("Продукт не знайдено."));

        OrderProduct orderProduct = orderProductRepository.findByConcreteProductAndOrder(concreteProduct, order)
                .orElseThrow(() -> new NoSuchElementException("Продукту в замовлені не знайдено."));

        List<ProductShipment> shipments =  productShipmentRepository.findByOrderProduct(orderProduct);
        int reducedQuantity = shipments.stream().reduce(0, (acc, shp)-> acc + shp.getProductQuantity(), Integer::sum);

        if(reducedQuantity-shipment.getProductQuantity()+dto.getQuantity()>orderProduct.getProductQuantity()){
            throw new NoSuchElementException("Сумарна кількість продуктів відправлень більше за вказану у замовлені.");
        }
        if(orderProduct.getProductQuantity()< dto.getQuantity()){
            throw new  NoSuchElementException("Кількість продуктів відправлення більше за вказану у замовлені.");
        }

        Room room = roomRepository.findByRoomNumber(dto.getRoomNumber())
                .orElseThrow(() -> new NoSuchElementException("Кімнату не знайдено."));
        if(!room.getProductType().getTypeName().equals(concreteProduct.getProduct().getProductType().getTypeName())){
            throw new NoSuchElementException("Обрана кімната не містить цієї продукції");
        }

        shipment.setLotBooking(booking);
        shipment.setEmployee(employee);
        shipment.setProductQuantity(dto.getQuantity());
        shipment.setOrderProduct(orderProduct);

        productShipmentRepository.save(shipment);

        Map<String, Object> newState = toAuditState(shipment);

        actionLogService.logAction(
                OPERATOR_ID,
                "UPDATE",
                "ProductShipment",
                shipment.getId(),
                prevState,
                newState
        );

        dto.setLotNumber(booking.getParkingLot().getLotNumber());
        return dto;
    }

    public void deleteShipment(String number) {
        ProductShipment shipment = productShipmentRepository.findByShipmentNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Відправлення не знайдено"));

        Map<String, Object> prevState = toAuditState(shipment);

        Order order = shipment.getOrderProduct().getOrder();
        if (order.getOrderStatus().getStatusName().equals( "SHIPPED")) {
            order.setOrderStatus(orderStatusRepository.findByStatusName("STORED").orElseThrow());
            orderRepository.save(order);
        }

        productShipmentRepository.delete(shipment);

        actionLogService.logAction(
         OPERATOR_ID,
                "DELETE",
                "ProductShipment",
                shipment.getId(),
                prevState,
                null
        );
    }

    private Map<String, Object> toAuditState(ProductShipment shipment) {
        Map<String, Object> state = new HashMap<>();
        state.put("shipmentNumber", shipment.getShipmentNumber());
        state.put("employee", shipment.getEmployee().getFirstName() + " " + shipment.getEmployee().getLastName());
        state.put("product", shipment.getOrderProduct().getConcreteProduct().getProductName());
        state.put("quantity", shipment.getProductQuantity());
        state.put("room", shipment.getStoredProduct().getRoom().getRoomNumber());
        state.put("lotBooking", shipment.getLotBooking().getBookingNumber());
        return state;
    }

    private String generateUniqueShipmentNumber() {
        Random random = new Random();
        String shipmentNumber;
        do {
            shipmentNumber = "SH-" + (100000 + random.nextInt(900000));
        } while (productShipmentRepository.existsByShipmentNumber(shipmentNumber));
        return shipmentNumber;
    }
}
