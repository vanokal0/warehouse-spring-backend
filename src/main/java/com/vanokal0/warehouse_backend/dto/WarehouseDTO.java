package com.vanokal0.warehouse_backend.dto;

import com.vanokal0.warehouse_backend.dto.geocoding.GeocodeLocation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WarehouseDTO {
    Integer id;
    String number;
    String address;
    GeocodeLocation location;
    List<RoomDTO> rooms;
    List<EmployeeDTO> employees;
    List<LoadingDockDTO> loadingDocks;
}
