package com.vanokal0.warehouse_backend.repositories;


import com.vanokal0.warehouse_backend.model.LotBooking;
import com.vanokal0.warehouse_backend.model.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LotBookingRepository extends JpaRepository<LotBooking, Integer>, JpaSpecificationExecutor<LotBooking> {
    Optional<LotBooking> findByBookingNumber(String bookingNumber);
    boolean existsByBookingNumber(String bookingNumber);
    List<LotBooking> findByParkingLot(ParkingLot parkingLot);

    @Query("""
            select lb.bookingNumber from LotBooking lb
            where lb.parkingLot.loadingDock.warehouse.number = :warehouseNumber
            """)
    List<String> findBookingNumbersByWarehouseNumber(@Param("warehouseNumber") String warehouseNumber);
}
