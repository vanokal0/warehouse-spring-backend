package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.ActionLogDTO;
import com.vanokal0.warehouse_backend.mappers.ActionLogMapper;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static com.vanokal0.warehouse_backend.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionLogServiceTest {

    @Mock
    private ActionLogRepository actionLogRepository;
    @Mock
    private ActionLogMapper actionLogMapper;
    @Mock
    private ParkingLotRepository parkingLotRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private RoomHumidityRepository roomHumidityRepository;
    @Mock
    private RoomTemperatureRepository roomTemperatureRepository;
    @Mock
    private ProductTypeRepository productTypeRepository;
    @Mock
    private VehicleTypeRepository vehicleTypeRepository;
    @Mock
    private LoadingDockRepository loadingDockRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EmployeePositionRepository employeePositionRepository;
    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private ActionLogService service;

    private static ActionLog log(String entityType, String action, Integer entityId, Map<String, Object> prev) {
        ActionLog log = new ActionLog();
        log.setId("log-1");
        log.setUserId(110);
        log.setEntityType(entityType);
        log.setAction(action);
        log.setEntityId(entityId);
        log.setPreviousState(prev);
        log.setTimestamp(LocalDateTime.of(2025, 1, 1, 12, 0));
        return log;
    }

    private ActionLog givenLog(String entityType, String action, Integer entityId, Map<String, Object> prev) {
        ActionLog log = log(entityType, action, entityId, prev);
        when(actionLogRepository.findById("log-1")).thenReturn(Optional.of(log));
        return log;
    }

    private void assertCancelled(ActionLog log) {
        assertThat(log.isCancelled()).isTrue();
        verify(actionLogRepository).save(log);
    }

    @Nested
    class Queries {

        @Test
        void getAllLogs_mapsEveryLog() {
            ActionLog a = log("Room", "CREATE", 1, null);
            ActionLog b = log("Room", "DELETE", 2, null);
            ActionLogDTO da = new ActionLogDTO();
            ActionLogDTO db = new ActionLogDTO();
            when(actionLogRepository.findAll()).thenReturn(List.of(a, b));
            when(actionLogMapper.toDTO(a)).thenReturn(da);
            when(actionLogMapper.toDTO(b)).thenReturn(db);

            assertThat(service.getAllLogs()).containsExactly(da, db);
        }

        @Test
        void getLogsByUserId_mapsUserLogs() {
            ActionLog a = log("Room", "CREATE", 1, null);
            ActionLogDTO da = new ActionLogDTO();
            when(actionLogRepository.findByUserId(110)).thenReturn(List.of(a));
            when(actionLogMapper.toDTO(a)).thenReturn(da);

            assertThat(service.getLogsByUserId(110)).containsExactly(da);
        }

        @Test
        void getLogById_mapsLog() {
            ActionLog a = log("Room", "CREATE", 1, null);
            ActionLogDTO da = new ActionLogDTO();
            when(actionLogRepository.findById("log-1")).thenReturn(Optional.of(a));
            when(actionLogMapper.toDTO(a)).thenReturn(da);

            assertThat(service.getLogById("log-1")).isSameAs(da);
        }

        @Test
        void getLogById_throws_whenMissing() {
            when(actionLogRepository.findById("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLogById("x")).isInstanceOf(NoSuchElementException.class);
        }
    }

    @Test
    void logAction_persistsNonCancelledLogWithCurrentTimestamp() {
        Map<String, Object> prev = Map.of("a", 1);
        Map<String, Object> next = Map.of("a", 2);
        LocalDateTime before = LocalDateTime.now();

        service.logAction(110, "UPDATE", "Room", 5, prev, next);

        ArgumentCaptor<ActionLog> captor = ArgumentCaptor.forClass(ActionLog.class);
        verify(actionLogRepository).save(captor.capture());
        ActionLog saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getUserId()).isEqualTo(110);
        assertThat(saved.getAction()).isEqualTo("UPDATE");
        assertThat(saved.getEntityType()).isEqualTo("Room");
        assertThat(saved.getEntityId()).isEqualTo(5);
        assertThat(saved.getPreviousState()).isSameAs(prev);
        assertThat(saved.getNewState()).isSameAs(next);
        assertThat(saved.isCancelled()).isFalse();
        assertThat(saved.getTimestamp()).isBetween(before, LocalDateTime.now());
    }

    @Nested
    class CancelAction {

        @Test
        void throws_whenLogMissing() {
            when(actionLogRepository.findById("nope")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelAction("nope"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("Action not found");
            verify(actionLogRepository, never()).save(any());
        }

        @Test
        void unknownEntityType_onlyMarksLogCancelled() {
            ActionLog log = givenLog("Order", "CREATE", 1, null);

            assertThat(service.cancelAction("log-1")).isTrue();

            assertCancelled(log);
            verifyNoInteractions(roomRepository, parkingLotRepository, employeeRepository, warehouseRepository);
        }

        @ParameterizedTest(name = "{0} {1} -> active={2}")
        @CsvSource({
                "Room, CREATE, false", "Room, DELETE, true",
                "ParkingLot, CREATE, false", "ParkingLot, DELETE, true",
                "Employee, CREATE, false", "Employee, DELETE, true",
                "Warehouse, CREATE, false", "Warehouse, DELETE, true"
        })
        void createAndDelete_toggleActiveFlag(String type, String action, boolean expectedActive) {
            ActionLog log = givenLog(type, action, 7, null);
            boolean initial = !expectedActive;
            Warehouse w = warehouse(7, "MA1", "Addr", initial);

            switch (type) {
                case "Room" -> {
                    Room r = room(7, "R1", w, initial);
                    when(roomRepository.findById(7)).thenReturn(Optional.of(r));
                    service.cancelAction("log-1");
                    assertThat(r.getIsActive()).isEqualTo(expectedActive);
                    verify(roomRepository).save(r);
                }
                case "ParkingLot" -> {
                    ParkingLot p = parkingLot(7, "L1", dock(1, "D1", w, true), "Truck", initial);
                    when(parkingLotRepository.findById(7)).thenReturn(Optional.of(p));
                    service.cancelAction("log-1");
                    assertThat(p.getIsActive()).isEqualTo(expectedActive);
                    verify(parkingLotRepository).save(p);
                }
                case "Employee" -> {
                    Employee e = employee(7, "Ann", "Lee", "Operator", w, initial);
                    when(employeeRepository.findById(7)).thenReturn(Optional.of(e));
                    service.cancelAction("log-1");
                    assertThat(e.getIsActive()).isEqualTo(expectedActive);
                    verify(employeeRepository).save(e);
                }
                case "Warehouse" -> {
                    when(warehouseRepository.findById(7)).thenReturn(Optional.of(w));
                    service.cancelAction("log-1");
                    assertThat(w.getIsActive()).isEqualTo(expectedActive);
                    verify(warehouseRepository).save(w);
                }
                default -> throw new IllegalArgumentException(type);
            }
            assertCancelled(log);
        }

        @ParameterizedTest
        @CsvSource({"Room, Room not found.", "ParkingLot, Parking lot not found.",
                "Employee, Employee not found.", "Warehouse, Warehouse not found."})
        void throws_whenTargetEntityMissing(String type, String message) {
            ActionLog log = givenLog(type, "CREATE", 7, null);

            assertThatThrownBy(() -> service.cancelAction("log-1"))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage(message);
            assertThat(log.isCancelled()).isFalse();
            verify(actionLogRepository, never()).save(any());
        }

        @Test
        void roomUpdate_restoresPreviousAttributes() {
            Room r = room(7, "R1", warehouse(1, "MA1", "Addr", true), true);
            RoomHumidity humidity = new RoomHumidity();
            humidity.setHumidity(70);
            RoomTemperature temperature = new RoomTemperature();
            temperature.setTemperature("WARM");
            ProductType type = productType("Electronics");
            ActionLog log = givenLog("Room", "UPDATE", 7, Map.of(
                    "availableSpace", 55, "humidity", 70, "temperature", "WARM", "productType", "Electronics"));
            when(roomRepository.findById(7)).thenReturn(Optional.of(r));
            when(roomHumidityRepository.findByHumidity(70)).thenReturn(Optional.of(humidity));
            when(roomTemperatureRepository.findByTemperature("WARM")).thenReturn(Optional.of(temperature));
            when(productTypeRepository.findByTypeName("Electronics")).thenReturn(Optional.of(type));

            service.cancelAction("log-1");

            assertThat(r.getAvailableSpace()).isEqualTo(55);
            assertThat(r.getHumidity()).isSameAs(humidity);
            assertThat(r.getTemperature()).isSameAs(temperature);
            assertThat(r.getProductType()).isSameAs(type);
            verify(roomRepository).save(r);
            assertCancelled(log);
        }

        @Test
        void parkingLotUpdate_restoresVehicleTypeAndDock() {
            Warehouse w = warehouse(1, "MA1", "Addr", true);
            ParkingLot p = parkingLot(7, "L1", dock(1, "D1", w, true), "Truck", true);
            VehicleType van = new VehicleType();
            van.setTypeName("Van");
            LoadingDock oldDock = dock(2, "D2", w, true);
            ActionLog log = givenLog("ParkingLot", "UPDATE", 7, Map.of("vehicleType", "Van", "dockNumber", "D2"));
            when(parkingLotRepository.findById(7)).thenReturn(Optional.of(p));
            when(vehicleTypeRepository.findByTypeName("Van")).thenReturn(Optional.of(van));
            when(loadingDockRepository.findByDockNumber("D2")).thenReturn(Optional.of(oldDock));

            service.cancelAction("log-1");

            assertThat(p.getVehicleType()).isSameAs(van);
            assertThat(p.getLoadingDock()).isSameAs(oldDock);
            verify(parkingLotRepository).save(p);
            assertCancelled(log);
        }

        @Test
        void employeeUpdate_restoresNamesAndPosition() {
            Employee e = employee(7, "New", "Name", "Operator", warehouse(1, "MA1", "Addr", true), true);
            EmployeePosition manager = position("Warehouse Manager");
            ActionLog log = givenLog("Employee", "UPDATE", 7,
                    Map.of("firstName", "Old", "lastName", "Surname", "position", "Warehouse Manager"));
            when(employeeRepository.findById(7)).thenReturn(Optional.of(e));
            when(employeePositionRepository.findByPositionName("Warehouse Manager")).thenReturn(Optional.of(manager));

            service.cancelAction("log-1");

            assertThat(e.getFirstName()).isEqualTo("Old");
            assertThat(e.getLastName()).isEqualTo("Surname");
            assertThat(e.getPosition()).isSameAs(manager);
            verify(employeeRepository).save(e);
            assertCancelled(log);
        }

        @Test
        void warehouseUpdate_restoresNumberAndAddress() {
            Warehouse w = warehouse(7, "NEW", "New addr", true);
            ActionLog log = givenLog("Warehouse", "UPDATE", 7, Map.of("number", "OLD", "address", "Old addr"));
            when(warehouseRepository.findById(7)).thenReturn(Optional.of(w));

            service.cancelAction("log-1");

            assertThat(w.getNumber()).isEqualTo("OLD");
            assertThat(w.getAddress()).isEqualTo("Old addr");
            verify(warehouseRepository).save(w);
            assertCancelled(log);
        }

        @Test
        void update_throws_whenPreviousLookupValueNoLongerExists() {
            Employee e = employee(7, "New", "Name", "Operator", warehouse(1, "MA1", "Addr", true), true);
            givenLog("Employee", "UPDATE", 7, Map.of("firstName", "Old", "lastName", "Surname", "position", "Gone"));
            when(employeeRepository.findById(7)).thenReturn(Optional.of(e));
            when(employeePositionRepository.findByPositionName("Gone")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cancelAction("log-1")).isInstanceOf(NoSuchElementException.class);
            verify(employeeRepository, never()).save(any());
            verify(actionLogRepository, never()).save(any());
        }

        @Test
        void unknownAction_savesEntityUnchangedAndCancelsLog() {
            Room r = room(7, "R1", warehouse(1, "MA1", "Addr", true), true);
            ActionLog log = givenLog("Room", "ARCHIVE", 7, null);
            when(roomRepository.findById(7)).thenReturn(Optional.of(r));

            service.cancelAction("log-1");

            assertThat(r.getIsActive()).isTrue();
            verify(roomRepository).save(r);
            assertCancelled(log);
        }
    }
}
