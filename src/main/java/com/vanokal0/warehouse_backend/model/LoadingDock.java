package com.vanokal0.warehouse_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class LoadingDock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loading_dock_id", nullable = false)
    private Integer id;

    @Column(name = "lot_quantity", nullable = false)
    private Integer lotQuantity;

    @Column(name = "dock_number", nullable = false, length = 10)
    private String dockNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

}