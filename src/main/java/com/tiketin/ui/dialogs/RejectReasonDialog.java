package com.tiketin.ui.dialogs;

import com.tiketin.ui.components.AnimatedButton;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Modern JavaFX Double-Bezel modal for entering mandatory rejection reason.
 */
public class RejectReasonDialog {

    private static String submittedReason = null;

    public static String showDialog(Object owner) {
        return showDialog(owner, "Organisasi");
    }

    public static String showDialog(Object owner, String targetLabel) {
        submittedReason = null;

        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Tolak Pengajuan");
        Window ownerWin = TiketinTheme.resolveWindow(owner);
        if (ownerWin != null) {
            dialogStage.initOwner(ownerWin);
        }
        dialogStage.setResizable(false);

        VBox content = new VBox(12);
        content.setPadding(new Insets(20, 22, 20, 22));
        content.setStyle("-fx-background-color: " + TiketinTheme.toHex(TiketinTheme.BG_SURFACE) + "; -fx-border-color: " + TiketinTheme.toHex(TiketinTheme.BORDER) + "; -fx-border-radius: 14px; -fx-background-radius: 14px;");

        Label titleLabel = new Label("Tolak Pengajuan");
        titleLabel.setFont(TiketinTheme.fontBold(16));
        titleLabel.setTextFill(TiketinTheme.TEXT_PRIMARY);

        String orgText = targetLabel != null && !targetLabel.isBlank() ? targetLabel : "-";
        Label subLabel = new Label(orgText + " — alasan akan ditampilkan ke pemohon.");
        subLabel.setFont(TiketinTheme.fontRegular(12));
        subLabel.setTextFill(TiketinTheme.TEXT_MUTED);

        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Masukkan alasan penolakan secara jelas...");
        reasonArea.setPrefRowCount(4);
        reasonArea.setWrapText(true);
        reasonArea.setFont(TiketinTheme.fontRegular(13));
        reasonArea.setStyle("-fx-control-inner-background: #FFFFFF; -fx-background-color: #FFFFFF;");

        HBox buttonPanel = new HBox(8);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);

        AnimatedButton cancelBtn = TiketinTheme.createSecondaryButton("Batal");
        cancelBtn.setOnAction(e -> {
            submittedReason = null;
            dialogStage.close();
        });

        AnimatedButton rejectBtn = TiketinTheme.createDangerButton("Konfirmasi Penolakan", "close");
        rejectBtn.setOnAction(e -> {
            String text = reasonArea.getText().trim();
            if (text.isEmpty()) {
                ToastNotification.error(dialogStage, "Alasan penolakan wajib diisi.");
                reasonArea.requestFocus();
                return;
            }
            submittedReason = text;
            dialogStage.close();
        });

        buttonPanel.getChildren().addAll(cancelBtn, rejectBtn);
        content.getChildren().addAll(titleLabel, subLabel, reasonArea, buttonPanel);

        StackPane root = new StackPane(content);
        root.setPadding(new Insets(12));
        root.setStyle("-fx-background-color: #F7F8F6;");
        Scene scene = new Scene(root);
        TiketinTheme.applyStylesheet(scene);
        dialogStage.setScene(scene);
        dialogStage.sizeToScene();
        dialogStage.showAndWait();

        return submittedReason;
    }
}
