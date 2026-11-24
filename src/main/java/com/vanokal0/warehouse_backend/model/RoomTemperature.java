package com.vanokal0.warehouse_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class RoomTemperature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_temperature_id", nullable = false)
    private Integer id;

    @Column(name = "temperature", nullable = false, length = 50)
    private String temperature;

}