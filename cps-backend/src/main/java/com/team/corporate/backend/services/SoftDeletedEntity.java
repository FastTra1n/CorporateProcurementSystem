package com.team.corporate.backend.services;

import java.util.Arrays;

public enum SoftDeletedEntity {
    USERS("users", "users"),
    CATEGORIES("categories", "categories"),
    PRODUCTS("products", "products"),
    WAREHOUSES("warehouses", "warehouses"),
    INVENTORIES("inventories", "inventory"),
    ORDERS("orders", "orders"),
    ORDER_ITEMS("order-items", "order_items"),
    AUDIT_LOGS("audit-logs", "audit_logs");

    private final String path;
    private final String table;

    SoftDeletedEntity(String path, String table) {
        this.path = path;
        this.table = table;
    }

    public String path() {
        return path;
    }

    public String table() {
        return table;
    }

    public static SoftDeletedEntity fromPath(String path) {
        return Arrays.stream(values())
                .filter(entity -> entity.path.equalsIgnoreCase(path))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Неизвестный тип удалённой записи."));
    }
}
