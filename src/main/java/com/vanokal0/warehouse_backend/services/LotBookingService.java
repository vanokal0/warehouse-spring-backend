package com.vanokal0.warehouse_backend.services;

import com.vanokal0.warehouse_backend.dto.LotBookingDTO;
import com.vanokal0.warehouse_backend.mappers.LotBookingMapper;
import com.vanokal0.warehouse_backend.model.LotBooking;
import com.vanokal0.warehouse_backend.model.ParkingLot;
import com.vanokal0.warehouse_backend.model.Warehouse;
import com.vanokal0.warehouse_backend.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class LotBookingService extends BaseSpecificationService<LotBooking> {
    private final Integer LOGIST_ID = 111;

    private final ParkingLotRepository parkingLotRepository;
    private final SupplierRepository supplierRepository;
    private final LotBookingRepository lotBookingRepository;
    private final ProductShipmentRepository productShipmentRepository;
    private final ProductPlacementRepository productPlacementRepository;
    private final LotBookingMapper lotBookingMapper;
    private final ActionLogService actionLogService;

    @Autowired
    public LotBookingService(
            ParkingLotRepository parkingLotRepository,
            SupplierRepository supplierRepository,
            LotBookingRepository lotBookingRepository,
            ProductShipmentRepository productShipmentRepository,
            ProductPlacementRepository productPlacementRepository,
            LotBookingMapper lotBookingMapper,
            ActionLogService actionLogService
    ) {
        this.parkingLotRepository = parkingLotRepository;
        this.supplierRepository = supplierRepository;
        this.lotBookingRepository = lotBookingRepository;
        this.productShipmentRepository = productShipmentRepository;
        this.productPlacementRepository = productPlacementRepository;
        this.lotBookingMapper = lotBookingMapper;
        this.actionLogService = actionLogService;
    }

    @Override
    protected Map<String, String> getFieldMapping() {
        return Map.of(
                "number", "bookingNumber",
                "lotNumber", "parkingLot.lotNumber",
                "supplierName", "supplier.companyName",
                "bookingDateTime", "bookingDateTime",
                "startDateTime", "startDateTime",
                "endDateTime", "endDateTime"
        );
    }

    public Map<String, Object> getPagedLotBookings(
            int page, int pageSize,
            List<String> sortFields, List<String> sortOrders,
            List<String> filterFields, List<String> filterOperators, List<String> filterValues, boolean all
    ) {
        Pageable pageable = all ? Pageable.unpaged() : PageRequest.of(page, pageSize, buildSort(sortFields, sortOrders));
        Specification<LotBooking> spec = buildSpec(filterFields, filterOperators, filterValues);

        Page<LotBooking> pageResult = lotBookingRepository.findAll(spec, pageable);
        List<LotBookingDTO> dtos = pageResult.getContent().stream()
                .map(lotBookingMapper::toDTO)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("items", dtos);
        response.put("total", pageResult.getTotalElements());
        return response;
    }

    public Map<String, Object> getPagedLotBookingsForLot(
            int page, int pageSize,
            List<String> sortFields, List<String> sortOrders,
            List<String> filterFields, List<String> filterOperators, List<String> filterValues, String lotNumber,boolean all
    ) {
        Pageable pageable = all ? Pageable.unpaged() : PageRequest.of(page, pageSize, buildSort(sortFields, sortOrders));
        Specification<LotBooking> filtersSpec = buildSpec(filterFields, filterOperators, filterValues);
        Specification<LotBooking> lotSpec = (root, query, cb) ->
                cb.equal(root.get("parkingLot").get("lotNumber"), lotNumber);
        Specification<LotBooking> combinedSpec = filtersSpec.and(lotSpec);

        Page<LotBooking> pageResult = lotBookingRepository.findAll(combinedSpec, pageable);
        List<LotBookingDTO> dtos = pageResult.getContent().stream()
                .map(lotBookingMapper::toDTO)
                .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("items", dtos);
        response.put("total", pageResult.getTotalElements());
        return response;
    }

    public List<String> getBookingNumbersByWarehouseNumber(String warehouseNumber) {
        return lotBookingRepository.findAll().stream()
                .filter(lb -> {
                    if (lb.getParkingLot() == null || lb.getParkingLot().getLoadingDock() == null) return false;
                    Warehouse w = lb.getParkingLot().getLoadingDock().getWarehouse();
                    return w != null && warehouseNumber.equals(w.getNumber());
                })
                .map(LotBooking::getBookingNumber)
                .toList();
    }

    public LotBookingDTO createLotBooking(LotBookingDTO dto) {
        LotBooking booking = new LotBooking();

        Instant bookingTime = dto.getBookingDateTime();
        Instant startTime = dto.getStartDateTime();
        Instant endTime = dto.getEndDateTime();
        if (bookingTime.isAfter(startTime) || bookingTime.equals(startTime)) {
            throw new IllegalArgumentException("Час бронювання має бути раніше за час початку.");
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new IllegalArgumentException("Час початку має бути раніше за час завершення.");
        }

        booking.setBookingDateTime(dto.getBookingDateTime());
        booking.setStartDateTime(dto.getStartDateTime());
        booking.setEndDateTime(dto.getEndDateTime());

        booking.setSupplier(
                supplierRepository.findByCompanyName(dto.getSupplierName())
                        .orElseThrow(() -> new NoSuchElementException("Постачальника не знайдено."))
        );

        ParkingLot parkingLot =                 parkingLotRepository.findByLotNumber(dto.getLotNumber())
                .orElseThrow(() -> new NoSuchElementException("Лот не знайдено."));
        booking.setParkingLot(
                parkingLot

        );

        booking.setBookingNumber(generateUniqueBookingNumber());


        List<LotBooking> existingBookings = lotBookingRepository.findByParkingLot(parkingLot);
        for (LotBooking existing : existingBookings) {
            boolean overlaps =
                    startTime.isBefore(existing.getEndDateTime()) &&
                            endTime.isAfter(existing.getStartDateTime());

            if (overlaps) {
                throw new IllegalStateException("Часове перетинання з іншим бронюванням лоту.");
            }
        }

        LotBooking saved = lotBookingRepository.save(booking);

        Map<String, Object> newState = new HashMap<>();
        newState.put("bookingNumber", saved.getBookingNumber());
        newState.put("supplier", saved.getSupplier().getCompanyName());
        newState.put("lotNumber", saved.getParkingLot().getLotNumber());
        newState.put("bookingDateTime", saved.getBookingDateTime());
        newState.put("startDateTime", saved.getStartDateTime());
        newState.put("endDateTime", saved.getEndDateTime());

        actionLogService.logAction(LOGIST_ID, "CREATE", "LotBooking", saved.getId(), null, newState);

        return lotBookingMapper.toDTO(saved);
    }

    public LotBookingDTO updateLotBooking(String number, LotBookingDTO dto) {
        LotBooking booking = lotBookingRepository.findByBookingNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Бронювання не знайдено."));

        Map<String, Object> prevState = new HashMap<>();
        prevState.put("bookingNumber", booking.getBookingNumber());
        prevState.put("supplier", booking.getSupplier().getCompanyName());
        prevState.put("lotNumber", booking.getParkingLot().getLotNumber());
        prevState.put("bookingDateTime", booking.getBookingDateTime());
        prevState.put("startDateTime", booking.getStartDateTime());
        prevState.put("endDateTime", booking.getEndDateTime());

        booking.setBookingDateTime(dto.getBookingDateTime());
        booking.setStartDateTime(dto.getStartDateTime());
        booking.setEndDateTime(dto.getEndDateTime());

        booking.setSupplier(
                supplierRepository.findByCompanyName(dto.getSupplierName())
                        .orElseThrow(() -> new NoSuchElementException("Постачальника не знайдено."))
        );

        booking.setParkingLot(
                parkingLotRepository.findByLotNumber(dto.getLotNumber())
                        .orElseThrow(() -> new NoSuchElementException("Лот не знайдено."))
        );

        LotBooking saved = lotBookingRepository.save(booking);

        Map<String, Object> newState = new HashMap<>();
        newState.put("bookingNumber", saved.getBookingNumber());
        newState.put("supplier", saved.getSupplier().getCompanyName());
        newState.put("lotNumber", saved.getParkingLot().getLotNumber());
        newState.put("bookingDateTime", saved.getBookingDateTime());
        newState.put("startDateTime", saved.getStartDateTime());
        newState.put("endDateTime", saved.getEndDateTime());

        actionLogService.logAction(LOGIST_ID, "UPDATE", "LotBooking", saved.getId(), prevState, newState);

        return lotBookingMapper.toDTO(saved);
    }

    public void deleteLotBooking(String number) {
        LotBooking booking = lotBookingRepository.findByBookingNumber(number)
                .orElseThrow(() -> new NoSuchElementException("Бронювання не знайдено."));

        if (productPlacementRepository.existsByLotBooking(booking)
                || productShipmentRepository.existsByLotBooking(booking)) {
            throw new NoSuchElementException("Бронювання має зв'язані операції.");
        }

        Map<String, Object> prevState = new HashMap<>();
        prevState.put("bookingNumber", booking.getBookingNumber());
        prevState.put("supplier", booking.getSupplier().getCompanyName());
        prevState.put("lotNumber", booking.getParkingLot().getLotNumber());
        prevState.put("bookingDateTime", booking.getBookingDateTime());
        prevState.put("startDateTime", booking.getStartDateTime());
        prevState.put("endDateTime", booking.getEndDateTime());

        lotBookingRepository.delete(booking);

        actionLogService.logAction(LOGIST_ID, "DELETE", "LotBooking", booking.getId(), prevState, null);
    }

    private String generateUniqueBookingNumber() {
        Random random = new Random();
        String booking;
        do {
            booking = "BK-" + (100000 + random.nextInt(900000));
        } while (lotBookingRepository.existsByBookingNumber(booking));
        return booking;
    }
}
