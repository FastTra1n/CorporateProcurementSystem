package com.team.corporate.services.ui;

public enum ViewType {
    CREATE_ORDER("/views/CreateOrder.fxml", "Оформление нового заказа");

    private final String fxmlPath;
    private final String title;

    ViewType(String fxmlPath, String title) {
        this.fxmlPath = fxmlPath;
        this.title = title;
    }

    public String getFxmlPath() { return fxmlPath; }
    public String getTitle() { return title; }
}
