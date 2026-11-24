package com.vanokal0.warehouse_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @Column(name = "room_number", nullable = false, length = 10)
    private String roomNumber;

    @Column(name = "available_space", nullable = false)
    private Integer availableSpace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temperature_id")
    private RoomTemperature temperature;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "humidity_id")
    private RoomHumidity humidity;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

}