package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.ActionLogDTO;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.vanokal0.warehouse_backend.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActionLogMapperTest {

    private static final int USER_ID = 110;
    private static final int ENTITY_ID = 42;

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ParkingLotRepository parkingLotRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private ConcreteProductRepository concreteProductRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductPlacementRepository productPlacementRepository;
    @Mock
    private ProductShipmentRepository productShipmentRepository;
    @Mock
    private LotBookingRepository lotBookingRepository;

    @InjectMocks
    private ActionLogMapper mapper;

    private final Warehouse warehouse = warehouse(1, "MA1", "Addr", true);

    private static ActionLog log(String type) {
        ActionLog log = new ActionLog();
        log.setId("abc");
        log.setUserId(USER_ID);
        log.setAction("UPDATE");
        log.setEntityType(type);
        log.setEntityId(ENTITY_ID);
        log.setTimestamp(LocalDateTime.of(2025, 5, 1, 9, 30));
        log.setCancelled(true);
        return log;
    }

    @BeforeEach
    void givenInitiator() {
        lenient().when(employeeRepository.findById(USER_ID))
                .thenReturn(Optional.of(employee(USER_ID, "John", "Smith", "Warehouse Manager", warehouse, true)));
    }

    @Test
    void mapsCommonFieldsAndInitiator() {
        ActionLogDTO dto = mapper.toDTO(log("Unknown"));

        assertThat(dto.getId()).isEqualTo("abc");
        assertThat(dto.getInitiatorFullName()).isEqualTo("John Smith");
        assertThat(dto.getTimestamp()).isEqualTo(LocalDateTime.of(2025, 5, 1, 9, 30));
        assertThat(dto.isCancelled()).isTrue();
        assertThat(dto.getOperationType()).isEqualTo("UPDATE");
        assertThat(dto.getEntityType()).isEqualTo("Unknown");
    }

    @Test
    void unknownEntityType_fallsBackToEntityId() {
        assertThat(mapper.toDTO(log("Unknown")).getEntityName()).isEqualTo("42");
    }

    @Test
    void throws_whenInitiatorMissing() {
        ActionLog log = log("Room");
        log.setUserId(999);

        assertThatThrownBy(() -> mapper.toDTO(log)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void warehouseName() {
        when(warehouseRepository.findById(ENTITY_ID)).thenReturn(Optional.of(warehouse(ENTITY_ID, "MA42", "A", true)));
        assertThat(mapper.toDTO(log("Warehouse")).getEntityName()).isEqualTo("MA42");
    }

    @Test
    void employeeName() {
        when(employeeRepository.findById(ENTITY_ID))
                .thenReturn(Optional.of(employee(ENTITY_ID, "Ann", "Lee", "Operator", warehouse, true)));
        assertThat(mapper.toDTO(log("Employee")).getEntityName()).isEqualTo("Ann Lee");
    }

    @Test
    void roomName() {
        when(roomRepository.findById(ENTITY_ID)).thenReturn(Optional.of(room(ENTITY_ID, "R-42", warehouse, true)));
        assertThat(mapper.toDTO(log("Room")).getEntityName()).isEqualTo("R-42");
    }

    @Test
    void parkingLotName() {
        when(parkingLotRepository.findById(ENTITY_ID))
                .thenReturn(Optional.of(parkingLot(ENTITY_ID, "L-42", dock(1, "D1", warehouse, true), "Truck", true)));
        assertThat(mapper.toDTO(log("ParkingLot")).getEntityName()).isEqualTo("L-42");
    }

    @Test
    void concreteProductName() {
        when(concreteProductRepository.findById(ENTITY_ID)).thenReturn(Optional.of(concreteProduct(ENTITY_ID, "Apple")));
        assertThat(mapper.toDTO(log("ConcreteProduct")).getEntityName()).isEqualTo("Apple");
    }

    @Test
    void orderName() {
        when(orderRepository.findById(ENTITY_ID)).thenReturn(Optional.of(order(ENTITY_ID, "ACM-042", "CREATED")));
        assertThat(mapper.toDTO(log("Order")).getEntityName()).isEqualTo("ACM-042");
    }

    @Test
    void productPlacementName() {
        ProductPlacement p = new ProductPlacement();
        p.setPlacementNumber("PL-42");
        when(productPlacementRepository.findById(ENTITY_ID)).thenReturn(Optional.of(p));
        assertThat(mapper.toDTO(log("ProductPlacement")).getEntityName()).isEqualTo("PL-42");
    }

    @Test
    void productShipmentName() {
        ProductShipment s = new ProductShipment();
        s.setShipmentNumber("SH-42");
        when(productShipmentRepository.findById(ENTITY_ID)).thenReturn(Optional.of(s));
        assertThat(mapper.toDTO(log("ProductShipment")).getEntityName()).isEqualTo("SH-42");
    }

    @Test
    void lotBookingName() {
        LotBooking b = new LotBooking();
        b.setBookingNumber("BK-42");
        when(lotBookingRepository.findById(ENTITY_ID)).thenReturn(Optional.of(b));
        assertThat(mapper.toDTO(log("LotBooking")).getEntityName()).isEqualTo("BK-42");
    }

    @Test
    void missingEntity_isReportedAsDeleted() {
        when(roomRepository.findById(ENTITY_ID)).thenReturn(Optional.empty());
        assertThat(mapper.toDTO(log("Room")).getEntityName()).isEqualTo("deleted");
    }

    @Test
    void stringifiesStateValues_includingNulls() {
        ActionLog log = log("Unknown");
        Map<String, Object> prev = new HashMap<>();
        prev.put("cost", 10.5);
        prev.put("note", null);
        log.setPreviousState(prev);
        log.setNewState(Map.of("cost", 12, "active", true));

        ActionLogDTO dto = mapper.toDTO(log);

        assertThat(dto.getPrevAttributes()).containsExactlyInAnyOrderEntriesOf(Map.of("cost", "10.5", "note", "null"));
        assertThat(dto.getNewAttributes()).containsExactlyInAnyOrderEntriesOf(Map.of("cost", "12", "active", "true"));
    }

    @Test
    void nullStates_becomeEmptyMaps() {
        ActionLogDTO dto = mapper.toDTO(log("Unknown"));

        assertThat(dto.getPrevAttributes()).isEmpty();
        assertThat(dto.getNewAttributes()).isEmpty();
    }
}
