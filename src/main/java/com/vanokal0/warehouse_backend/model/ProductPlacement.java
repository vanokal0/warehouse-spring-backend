package com.vanokal0.warehouse_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
public class ProductPlacement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_placement_id", nullable = false)
    private Integer id;

    @Column(name = "placement_number", nullable = false, length = 10)
    private String placementNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_product_id")
    private OrderProduct orderProduct;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stored_product_id", nullable = false)
    private StoredProduct storedProduct;

    @Column(name = "placement_date_time", nullable = false)
    private Instant placementDateTime;

    @Column(name = "product_quantity", nullable = false)
    private Integer productQuantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_booking_id")
    private LotBooking lotBooking;

}