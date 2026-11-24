package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.ParkingLotDTO;
import com.vanokal0.warehouse_backend.model.ParkingLot;
import com.vanokal0.warehouse_backend.repositories.VehicleTypeRepository;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotMapper implements ModelDTOMapper<ParkingLot, ParkingLotDTO>{

    @Override
    public ParkingLotDTO toDTO(ParkingLot parkingLot) {

        return new ParkingLotDTO(
                parkingLot.getLotNumber(),
                parkingLot.getLoadingDock().getWarehouse().getNumber(),
                parkingLot.getLoadingDock().getDockNumber(),
                parkingLot.getVehicleType().getTypeName()
        );
    }
}
