package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.ConcreteProductDTO;
import com.vanokal0.warehouse_backend.dto.EmployeeDTO;
import com.vanokal0.warehouse_backend.dto.ParkingLotDTO;
import com.vanokal0.warehouse_backend.dto.RoomDTO;
import com.vanokal0.warehouse_backend.model.LoadingDock;
import com.vanokal0.warehouse_backend.model.Warehouse;
import com.vanokal0.warehouse_backend.repositories.EmployeePositionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.vanokal0.warehouse_backend.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SimpleMappersTest {

    private final Warehouse warehouse = warehouse(1, "MA1", "Addr", true);

    @Test
    void concreteProductMapper_flattensProductTypeAndManufacturer() {
        ConcreteProductDTO dto = new ConcreteProductMapper().toDTO(concreteProduct(3, "Apple"));

        assertThat(dto).isEqualTo(new ConcreteProductDTO(
                3, "Apple", "Generic Apple", "Food", new BigDecimal("12.50"), 1.5, "Maker Inc"));
    }

    @Test
    void roomMapper_flattensLookups() {
        RoomDTO dto = new RoomMapper().toDTO(room(1, "R-01", warehouse, true));

        assertThat(dto).isEqualTo(new RoomDTO("R-01", "MA1", 100, "COLD", 40, "Food"));
    }

    @Test
    void employeeMapper_buildsFullNameAndPosition() {
        EmployeeDTO dto = new EmployeeMapper(mock(EmployeePositionRepository.class))
                .toDTO(employee(9, "Ann", "Lee", "Operator", warehouse, true));

        assertThat(dto).isEqualTo(new EmployeeDTO(9, "Ann", "Lee", "Ann Lee", "Operator", "MA1"));
    }

    @Test
    void parkingLotMapper_resolvesWarehouseThroughDock() {
        LoadingDock dock = dock(10, "D-01", warehouse, true);

        ParkingLotDTO dto = new ParkingLotMapper().toDTO(parkingLot(5, "L-01", dock, "Truck", true));

        assertThat(dto).isEqualTo(new ParkingLotDTO("L-01", "MA1", "D-01", "Truck"));
    }
}
