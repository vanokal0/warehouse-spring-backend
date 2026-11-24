package com.vanokal0.warehouse_backend;

import com.vanokal0.warehouse_backend.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Warehouse warehouse(Integer id, String number, String address, boolean active) {
        Warehouse w = new Warehouse();
        w.setId(id);
        w.setNumber(number);
        w.setAddress(address);
        w.setIsActive(active);
        return w;
    }

    public static EmployeePosition position(String name) {
        EmployeePosition p = new EmployeePosition();
        p.setPositionName(name);
        return p;
    }

    public static Employee employee(Integer id, String first, String last, String position, Warehouse warehouse, boolean active) {
        Employee e = new Employee();
        e.setId(id);
        e.setFirstName(first);
        e.setLastName(last);
        e.setPosition(position(position));
        e.setWarehouse(warehouse);
        e.setIsActive(active);
        return e;
    }

    public static ProductType productType(String name) {
        ProductType t = new ProductType();
        t.setTypeName(name);
        return t;
    }

    public static Room room(Integer id, String number, Warehouse warehouse, boolean active) {
        RoomTemperature temperature = new RoomTemperature();
        temperature.setTemperature("COLD");
        RoomHumidity humidity = new RoomHumidity();
        humidity.setHumidity(40);
        Room r = new Room();
        r.setId(id);
        r.setRoomNumber(number);
        r.setWarehouse(warehouse);
        r.setAvailableSpace(100);
        r.setTemperature(temperature);
        r.setHumidity(humidity);
        r.setProductType(productType("Food"));
        r.setIsActive(active);
        return r;
    }

    public static LoadingDock dock(Integer id, String number, Warehouse warehouse, boolean active) {
        LoadingDock d = new LoadingDock();
        d.setId(id);
        d.setDockNumber(number);
        d.setWarehouse(warehouse);
        d.setIsActive(active);
        return d;
    }

    public static ParkingLot parkingLot(Integer id, String number, LoadingDock dock, String vehicleType, boolean active) {
        VehicleType vt = new VehicleType();
        vt.setTypeName(vehicleType);
        ParkingLot p = new ParkingLot();
        p.setId(id);
        p.setLotNumber(number);
        p.setLoadingDock(dock);
        p.setVehicleType(vt);
        p.setIsActive(active);
        return p;
    }

    public static ConcreteProduct concreteProduct(Integer id, String name) {
        ProductManufacturer manufacturer = new ProductManufacturer();
        manufacturer.setCompanyName("Maker Inc");
        Product product = new Product();
        product.setProductName("Generic " + name);
        product.setProductType(productType("Food"));
        ConcreteProduct cp = new ConcreteProduct();
        cp.setId(id);
        cp.setProductName(name);
        cp.setEstimatedValue(new BigDecimal("12.50"));
        cp.setWeight(1.5);
        cp.setProduct(product);
        cp.setManufacturer(manufacturer);
        return cp;
    }

    public static Supplier supplier(String name) {
        Supplier s = new Supplier();
        s.setCompanyName(name);
        s.setEmail("contact@" + name.toLowerCase().replace(" ", "") + ".com");
        return s;
    }

    public static OrderStatus orderStatus(String name) {
        OrderStatus s = new OrderStatus();
        s.setStatusName(name);
        return s;
    }

    public static PaymentType paymentType(String name) {
        PaymentType p = new PaymentType();
        p.setTypeName(name);
        return p;
    }

    public static Order order(Integer id, String number, String status) {
        Order o = new Order();
        o.setId(id);
        o.setOrderNumber(number);
        o.setOrderCost(new BigDecimal("1000.00"));
        o.setOrderDateTime(Instant.parse("2025-03-01T10:00:00Z"));
        o.setEstimatedArrivalDate(LocalDate.of(2025, 3, 10));
        o.setEstimatedShipmentDate(LocalDate.of(2025, 3, 5));
        o.setSupplier(supplier("Acme Corp"));
        o.setOrderStatus(orderStatus(status));
        o.setPaymentType(paymentType("CARD"));
        return o;
    }

    public static OrderProduct orderProduct(Integer id, Order order, ConcreteProduct product, int quantity) {
        OrderProduct op = new OrderProduct();
        op.setId(id);
        op.setOrder(order);
        op.setConcreteProduct(product);
        op.setProductQuantity(quantity);
        return op;
    }
}
