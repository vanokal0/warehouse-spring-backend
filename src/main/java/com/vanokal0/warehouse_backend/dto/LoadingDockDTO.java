package com.vanokal0.warehouse_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoadingDockDTO {
    String number;
    List<ParkingLotDTO> parkingLots;
}
