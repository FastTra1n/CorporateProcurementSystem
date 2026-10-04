package com.team.corporate.services.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

import java.io.IOException;

public class DialogService {
    private final Callback<Class<?>, Object> controllerFactory;

    public DialogService(Callback<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    @SuppressWarnings("unchecked")
    public <T> void showModal(ViewType viewType, Window owner, T payload) {
        // Перегрузка модалки с предварительной передачей данных
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewType.getFxmlPath()));
            loader.setControllerFactory(controllerFactory);

            Parent root = loader.load();

            var controller = loader.getController();
            if (payload != null && controller instanceof DataReceiver) {
                ((DataReceiver<T>) controller).receiveData(payload);
            }

            Stage stage = new Stage();
            stage.setTitle(viewType.getTitle());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(owner);
            stage.setScene(new Scene(root));

            stage.showAndWait();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить окно:" + viewType, e);
        }
    }

    public void showModal(ViewType viewType, Window owner) {
        // Перегрузка модалки без передаваемых данных
        showModal(viewType, owner, null);
    }
}
