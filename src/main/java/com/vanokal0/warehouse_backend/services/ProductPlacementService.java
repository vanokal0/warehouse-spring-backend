package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.ProductPlacementDTO;
import com.vanokal0.warehouse_backend.dto.ProductShipmentDTO;
import com.vanokal0.warehouse_backend.mappers.ProductPlacementMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import com.vanokal0.warehouse_backend.services.BaseSpecificationService;
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
public class ProductPlacementService extends BaseSpecificationService<ProductPlacement> {

    private final Integer OPERATOR_ID = 117;
    private final ActionLogService actionLogService;
    private final LotBookingRepository lotBookingRepository;
    private final EmployeeRepository employeeRepository;
    private final RoomRepository roomRepository;
    private final ProductPlacementRepository productPlacementRepository;
    private final ProductPlacementMapper productPlacementMapper;
    private final OrderProductRepository orderProductRepository;
    private final StoredProductRepository storedProductRepository;
    private final OrderRepository orderRepository;
    private final ConcreteProductRepository concreteProductRepository;
    private final OrderStatusRepository orderStatusRepository;

    @Autowired
    public ProductPlacementService(ActionLogService actionLogService, LotBookingRepository lotBookingRepository, EmployeeRepository employeeRepository, RoomRepository roomRepository, ProductPlacementRepository productPlacementRepository,
                                   ProductPlacementMapper productPlacementMapper, OrderProductRepository orderProductRepository,
                                   StoredProductRepository storedProductRepository, OrderRepository orderRepository,
                                   ConcreteProductRepository concreteProductRepository,
                                   OrderStatusRepository orderStatusRepository) {
        this.actionLogService = actionLogService;
        this.lotBookingRepository = lotBookingRepository;
        this.employeeRepository = employeeRepository;
        this.roomRepository = roomRepository;
        this.productPlacementRepository = productPlacementRepository;
        this.productPlacementMapper = productPlacementMapper;
        this.orderProductRepository = orderProductRepository;

        this.storedProductRepository = storedProductRepository;
        this.orderRepository = orderRepository;
        this.concreteProductRepository = concreteProductRepository;
        this.orderStatusRepository = orderStatusRepository;
    }

    @Override
    protected Map<String, String> getFieldMapping() {
        return Map.of(
                "number", "placementNumber",
                "product", "storedProduct.concreteProduct.productName",
                "roomNumber", "storedProduct.room.roomNumber",
                "lotNumber", "lotBooking.parkingLot.lotNumber",
                "lotBookingNumber", "lotBooking.bookingNumber",
                "employee", "employee.firstName"
        );
    }

    public List<ProductPlacementDTO> getByOrderAndProduct(String orderNumber, String productName) {
        Specification<ProductPlacement> spec = Specification
                .where(fieldEquals("orderProduct.order.orderNumber", orderNumber))
                .and(fieldEquals("orderProduct.concreteProduct.productName", productName));

        return productPlacementRepository.findAll(spec).stream()
                .map(productPlacementMapper::toDTO)
                .toList();
    }

    @Transactional
    public ProductPlacementDTO createPlacement(ProductPlacementDTO dto) {
        LotBooking booking = lotBookingRepository.findByBookingNumber(dto.getLotBookingNumber())
                .orElseThrow(() -> new NoSuchElementException("Бронювання не знайдено"));

        List<String> splitName = Arrays.stream(dto.getEmployee().split(" ")).toList();
        Employee employee = employeeRepository.findByFirstNameAndLastName(splitName.getFirst(), splitName.getLast())
                .orElseThrow(() -> new NoSuchElementException("Оператора не знайдено"));


        Order order = orderRepository.findByOrderNumber(dto.getOrderNumber()).orElseThrow(() -> new NoSuchElementException("Замовлення не знайдено"));
        ConcreteProduct concreteProduct = concreteProductRepository.findByProductName(dto.getProduct()).orElseThrow(()-> new NoSuchElementException("Продукт не знайдено"));

        Room room = roomRepository.findByRoomNumber(dto.getRoomNumber())
                .orElseThrow(() -> new NoSuchElementException("Кімнату не знайдено"));
        if(!room.getProductType().getTypeName().equals(concreteProduct.getProduct().getProductType().getTypeName())){
            throw new NoSuchElementException("Обрана кімната не відповідає типу продукції");
        }

        OrderProduct orderProduct = orderProductRepository.findByConcreteProductAndOrder(concreteProduct, order).orElseThrow(() -> new NoSuchElementException("Продукту в замовлені не знайдено"));
        if(orderProduct.getProductQuantity()< dto.getQuantity()){
            throw new  NoSuchElementException("Кількість продуктів отримань більше за вказану у замовлені.");
        }

        List<ProductPlacement> placements =  productPlacementRepository.findByOrderProduct(orderProduct);
        int reducedQuantity = placements.stream().reduce(0, (acc, shp)-> acc + shp.getProductQuantity(), Integer::sum);
        if(reducedQuantity+dto.getQuantity()>orderProduct.getProductQuantity()){
            throw new NoSuchElementException("Сумарна кількість продуктів отримань більше за вказану у замовлені.");
        }

        StoredProduct storedProduct = new StoredProduct();
        storedProduct.setRoom(room);
        storedProduct.setConcreteProduct(orderProduct.getConcreteProduct());
        storedProduct.setProductQuantity(dto.getQuantity());

        ProductPlacement placement = new ProductPlacement();
        placement.setPlacementNumber(generateUniquePlacementNumber());

        placement.setLotBooking(booking);
        placement.setEmployee(employee);
        placement.setOrderProduct(orderProduct);
        placement.setStoredProduct(storedProductRepository.save(storedProduct));


        placement.setProductQuantity(dto.getQuantity());
        placement.setPlacementDateTime(Instant.now());

        productPlacementRepository.save(placement);

        Map<String, Object> newState = toAuditState(placement);

        actionLogService.logAction(
                OPERATOR_ID,
                "CREATE",
                "ProductPlacement",
                placement.getId(),
                null,
                newState
        );

        if(orderProductRepository.findOrderProductsByOrderId(order.getId()).stream().filter(op->!productPlacementRepository.existsByOrderProduct(op)).toList().isEmpty()){
            order.setOrderStatus(orderStatusRepository.findByStatusName("STORED").orElseThrow());
            orderRepository.save(order);
        }


        dto.setNumber(placement.getPlacementNumber());
        dto.setLotNumber(booking.getParkingLot().getLotNumber());

        return dto;
    }

    public ProductPlacementDTO updatePlacement(String number, ProductPlacementDTO dto) {
        ProductPlacement placement = productPlacementRepository.findByPlacementNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Розміщення не знайдено"));

        Map<String, Object> prevState = toAuditState(placement);


        LotBooking booking = lotBookingRepository.findByBookingNumber(dto.getLotBookingNumber())
                .orElseThrow(() -> new NoSuchElementException("Бронювання не знайдено"));

        List<String> splitName = Arrays.stream(dto.getEmployee().split(" ")).toList();
        Employee employee = employeeRepository.findByFirstNameAndLastName(splitName.getFirst(), splitName.getLast())
                .orElseThrow(() -> new NoSuchElementException("Оператора не знайдено"));


        Order order = orderRepository.findByOrderNumber(dto.getOrderNumber()).orElseThrow(() -> new NoSuchElementException("Замовлення не знайдено"));
        ConcreteProduct concreteProduct = concreteProductRepository.findByProductName(dto.getProduct()).orElseThrow(()-> new NoSuchElementException("Продукт не знайдено"));
        OrderProduct orderProduct = orderProductRepository.findByConcreteProductAndOrder(concreteProduct, order).orElseThrow(() -> new NoSuchElementException("Продукту в замовлені не знайдено"));

        if(orderProduct.getProductQuantity()< dto.getQuantity()){
            throw new  NoSuchElementException("Кількість продуктів отримань більше за вказану у замовлені.");
        }

        List<ProductPlacement> placements =  productPlacementRepository.findByOrderProduct(orderProduct);
        int reducedQuantity = placements.stream().reduce(0, (acc, shp)-> acc + shp.getProductQuantity(), Integer::sum);
        if(reducedQuantity+dto.getQuantity()-placement.getProductQuantity()>orderProduct.getProductQuantity()){
            throw new NoSuchElementException("Сумарна кількість продуктів отримань більше за вказану у замовлені.");
        }

        Room room = roomRepository.findByRoomNumber(dto.getRoomNumber())
                .orElseThrow(() -> new NoSuchElementException("Кімнату не знайдено"));
        if(!room.getProductType().getTypeName().equals(concreteProduct.getProduct().getProductType().getTypeName())){
            throw new NoSuchElementException("Обрана кімната не відповідає типу продукції");
        }

        placement.setLotBooking(booking);
        placement.setEmployee(employee);
        placement.setProductQuantity(dto.getQuantity());
        placement.setOrderProduct(orderProduct);

        productPlacementRepository.save(placement);

        Map<String, Object> newState = toAuditState(placement);

        actionLogService.logAction(
                OPERATOR_ID,
                "UPDATE",
                "ProductPlacement",
                placement.getId(),
                prevState,
                newState
        );

        dto.setLotNumber(booking.getParkingLot().getLotNumber());

        return dto;
    }

    public void deletePlacement(String number){

        ProductPlacement placement = productPlacementRepository.findByPlacementNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Відправлення не знайдено"));

        Map<String, Object> prevState = toAuditState(placement);
        productPlacementRepository.delete(placement);
        Order order = placement.getOrderProduct().getOrder();
        if(Objects.equals(order.getOrderStatus().getStatusName(), "STORED")){
            order.setOrderStatus(orderStatusRepository.findByStatusName("CREATED").orElseThrow());
            orderRepository.save(order);
        }
        actionLogService.logAction(
                OPERATOR_ID,
                "DELETE",
                "ProductPlacement",
                placement.getId(),
                prevState,
                null
        );
    }

    private Map<String, Object> toAuditState(ProductPlacement placement) {
        Map<String, Object> state = new HashMap<>();
        state.put("number", placement.getPlacementNumber());
        state.put("employee", placement.getEmployee().getFirstName() + " " + placement.getEmployee().getLastName());
        state.put("room", placement.getStoredProduct().getRoom().getRoomNumber());
        state.put("order", placement.getOrderProduct().getOrder().getOrderNumber());
        state.put("product", placement.getOrderProduct().getConcreteProduct().getProductName());
        state.put("quantity", placement.getProductQuantity());
        state.put("lotBooking", placement.getLotBooking().getBookingNumber());
        return state;
    }

    private String generateUniquePlacementNumber() {
        Random random = new Random();
        String placementNumber;
        do {
            placementNumber = "PL-" + (100000 + random.nextInt(900000)); // Generates a 4-digit number
        } while (productPlacementRepository.existsProductPlacementByPlacementNumber(placementNumber)); // Regenerate if the number already exists
        return placementNumber;
    }
}


