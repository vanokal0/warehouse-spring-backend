package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.*;
import com.vanokal0.warehouse_backend.dto.geocoding.GeocodeLocation;
import com.vanokal0.warehouse_backend.model.*;
import com.vanokal0.warehouse_backend.repositories.EmployeeRepository;
import com.vanokal0.warehouse_backend.repositories.LoadingDockRepository;
import com.vanokal0.warehouse_backend.repositories.ParkingLotRepository;
import com.vanokal0.warehouse_backend.repositories.RoomRepository;
import com.vanokal0.warehouse_backend.utils.GeocodingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.vanokal0.warehouse_backend.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseMapperTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private LoadingDockRepository loadingDockRepository;
    @Mock
    private ParkingLotRepository parkingLotRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private GeocodingService geocodingService;
    @Mock
    private EmployeeMapper employeeMapper;
    @Mock
    private ParkingLotMapper parkingLotMapper;
    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private WarehouseMapper mapper;

    @Test
    void mapsWarehouseWithOnlyActiveChildren() {
        Warehouse w = warehouse(1, "MA1", "Main st 1", true);
        Room activeRoom = room(1, "R1", w, true);
        Room inactiveRoom = room(2, "R2", w, false);
        LoadingDock activeDock = dock(10, "D1", w, true);
        LoadingDock inactiveDock = dock(11, "D2", w, false);
        ParkingLot activeLot = parkingLot(100, "L1", activeDock, "Truck", true);
        ParkingLot inactiveLot = parkingLot(101, "L2", activeDock, "Truck", false);
        Employee activeEmp = employee(1000, "Ann", "Lee", "Operator", w, true);
        Employee inactiveEmp = employee(1001, "Bob", "Ray", "Operator", w, false);

        RoomDTO roomDto = new RoomDTO();
        ParkingLotDTO lotDto = new ParkingLotDTO();
        EmployeeDTO empDto = new EmployeeDTO();
        GeocodeLocation location = new GeocodeLocation(50.45, 30.52);

        when(roomRepository.findByWarehouseId(1)).thenReturn(List.of(activeRoom, inactiveRoom));
        when(loadingDockRepository.findByWarehouseId(1)).thenReturn(List.of(activeDock, inactiveDock));
        when(parkingLotRepository.findByLoadingDockId(10)).thenReturn(List.of(activeLot, inactiveLot));
        when(employeeRepository.findByWarehouseId(1)).thenReturn(List.of(activeEmp, inactiveEmp));
        when(roomMapper.toDTO(activeRoom)).thenReturn(roomDto);
        when(parkingLotMapper.toDTO(activeLot)).thenReturn(lotDto);
        when(employeeMapper.toDTO(activeEmp)).thenReturn(empDto);
        when(geocodingService.getGeocodingAPIResponse("Main st 1")).thenReturn(location);

        WarehouseDTO dto = mapper.toDTO(w);

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getNumber()).isEqualTo("MA1");
        assertThat(dto.getAddress()).isEqualTo("Main st 1");
        assertThat(dto.getLocation()).isSameAs(location);
        assertThat(dto.getRooms()).containsExactly(roomDto);
        assertThat(dto.getEmployees()).containsExactly(empDto);
        assertThat(dto.getLoadingDocks()).hasSize(1);
        assertThat(dto.getLoadingDocks().get(0).getNumber()).isEqualTo("D1");
        assertThat(dto.getLoadingDocks().get(0).getParkingLots()).containsExactly(lotDto);

        verify(parkingLotRepository, never()).findByLoadingDockId(11);
        verify(roomMapper, never()).toDTO(inactiveRoom);
        verify(parkingLotMapper, never()).toDTO(inactiveLot);
        verify(employeeMapper, never()).toDTO(inactiveEmp);
    }

    @Test
    void mapsEmptyWarehouse_withNullLocationWhenGeocodingFails() {
        Warehouse w = warehouse(2, "MA2", "Nowhere", true);
        when(roomRepository.findByWarehouseId(2)).thenReturn(List.of());
        when(loadingDockRepository.findByWarehouseId(2)).thenReturn(List.of());
        when(employeeRepository.findByWarehouseId(2)).thenReturn(List.of());
        when(geocodingService.getGeocodingAPIResponse("Nowhere")).thenReturn(null);

        WarehouseDTO dto = mapper.toDTO(w);

        assertThat(dto.getLocation()).isNull();
        assertThat(dto.getRooms()).isEmpty();
        assertThat(dto.getEmployees()).isEmpty();
        assertThat(dto.getLoadingDocks()).isEmpty();
        verify(parkingLotRepository, never()).findByLoadingDockId(anyInt());
    }
}
