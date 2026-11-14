package com.tiketin;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.ToastNotification;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainFrame mainFrame = new MainFrame();

        Scene scene = new Scene(mainFrame, 1320, 840);
        com.tiketin.ui.theme.TiketinTheme.applyStylesheet(scene);

        KeyCombination shortcutK = new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN);
        scene.getAccelerators().put(shortcutK, mainFrame::triggerSearchFocus);

        primaryStage.setTitle("Tiketin — Desktop Event & Ticketing");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        primaryStage.show();

        com.tiketin.ui.theme.TiketinTheme.runAsync(() -> {
            return DatabaseConfig.testConnection();
        }, connected -> {
            if (!connected) {
                ToastNotification.error(mainFrame, "Peringatan: Gagal terhubung ke database MySQL.");
            } else {
                ToastNotification.success(mainFrame, "Terhubung ke database Tiketin.");
            }
        }, err -> {
            ToastNotification.error(mainFrame, "Database error: " + err.getMessage());
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
