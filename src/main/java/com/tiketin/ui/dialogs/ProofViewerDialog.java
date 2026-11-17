package com.tiketin.ui.dialogs;

import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.ProofStorageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class ProofViewerDialog {

    public static void showProof(Object owner, String filename) {
        File file = filename != null ? ProofStorageUtil.findProofFile(filename) : null;
        showProof(owner, file);
    }

    public static void showProof(Object owner, File file) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        javafx.stage.Window ownerWin = TiketinTheme.resolveWindow(owner);
        if (ownerWin != null) stage.initOwner(ownerWin);
        stage.setTitle("Bukti Pembayaran");

        VBox content = new VBox(20);
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: #FFFFFF; " +
                         "-fx-border-color: #E3E6E2; " +
                         "-fx-border-width: 1; " +
                         "-fx-border-radius: 14; " +
                         "-fx-background-radius: 14;");

        Label headerLbl = new Label("Bukti Transfer Pembayaran");
        headerLbl.setFont(TiketinTheme.fontBold(16));
        headerLbl.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.TEXT_PRIMARY) + ";");
        content.getChildren().add(headerLbl);

        if (file != null && file.exists()) {
            try {
                Image img = new Image(file.toURI().toString());
                ImageView view = new ImageView(img);
                view.setPreserveRatio(true);
                view.setFitWidth(450);
                view.setFitHeight(450);
                content.getChildren().add(view);
            } catch (Exception e) {
                Label errLbl = new Label("Gagal merender gambar bukti pembayaran.");
                errLbl.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.DANGER_FG) + ";");
                content.getChildren().add(errLbl);
            }
        } else {
            Label notFound = new Label("File bukti pembayaran tidak ditemukan di penyimpanan.");
            notFound.setFont(TiketinTheme.fontRegular(13));
            notFound.setStyle("-fx-text-fill: " + TiketinTheme.toHex(TiketinTheme.TEXT_MUTED) + ";");
            content.getChildren().add(notFound);
        }

        AnimatedButton closeBtn = TiketinTheme.createPrimaryButton("Tutup", "check");
        closeBtn.setOnAction(e -> stage.close());
        content.getChildren().add(closeBtn);

        VBox root = new VBox(content);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #F7F8F6;");

        StackPane modalRoot = new StackPane(root);
        Scene scene = new Scene(modalRoot, 520, 600);
        TiketinTheme.applyStylesheet(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }

    public static void showDialog(Object owner, String filename) {
        showProof(owner, filename);
    }
}
