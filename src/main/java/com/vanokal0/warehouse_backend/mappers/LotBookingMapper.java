package com.vanokal0.warehouse_backend.mappers;

import com.vanokal0.warehouse_backend.dto.LotBookingDTO;
import com.vanokal0.warehouse_backend.model.LotBooking;
import org.springframework.stereotype.Component;

@Component
public class LotBookingMapper implements ModelDTOMapper<LotBooking, LotBookingDTO>{

    @Override
    public LotBookingDTO toDTO(LotBooking lotBooking) {
        return new LotBookingDTO(
               lotBooking.getBookingNumber(),
               lotBooking.getParkingLot().getLotNumber(),
               lotBooking.getSupplier().getCompanyName(),
                lotBooking.getBookingDateTime(),
                lotBooking.getStartDateTime(),
                lotBooking.getEndDateTime()
        );
    }
}
