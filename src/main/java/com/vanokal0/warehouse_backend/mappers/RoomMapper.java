package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.RoomDTO;
import com.vanokal0.warehouse_backend.model.Room;
import com.vanokal0.warehouse_backend.repositories.ProductTypeRepository;
import com.vanokal0.warehouse_backend.repositories.RoomHumidityRepository;
import com.vanokal0.warehouse_backend.repositories.RoomTemperatureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper implements ModelDTOMapper<Room, RoomDTO>{
    @Override
    public RoomDTO toDTO(Room room) {
        return new RoomDTO(
                room.getRoomNumber(),
                room.getWarehouse().getNumber(),
                room.getAvailableSpace(),
                room.getTemperature().getTemperature(),
                room.getHumidity().getHumidity(),
                room.getProductType().getTypeName()
        );
    }
}
