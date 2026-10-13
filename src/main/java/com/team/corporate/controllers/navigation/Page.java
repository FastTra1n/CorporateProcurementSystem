package com.team.corporate.controllers.navigation;

public enum Page {
    MY_ORDERS("/views/MyOrders.fxml"),
    HUB("/views/Services.fxml"),
    STATISTICS("/views/Statistics.fxml"),
    PROFILE("/views/Profile.fxml");

    private final String fxmlPath;

    Page(String fxmlPath) { this.fxmlPath = fxmlPath; }

    public String getFxmlPath() { return fxmlPath; }
}
