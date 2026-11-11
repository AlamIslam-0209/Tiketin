package com.tiketin;

import com.tiketin.config.DatabaseConfig;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class Main {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            TiketinTheme.setup();

            MainFrame frame = new MainFrame();
            frame.setVisible(true);

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    return DatabaseConfig.testConnection();
                }

                @Override
                protected void done() {
                    try {
                        if (!Boolean.TRUE.equals(get())) {
                            ToastNotification.error(frame, "Tidak dapat terhubung ke MySQL (tiketin_db2).");
                        }
                    } catch (Exception ignored) {}
                }
            }.execute();
        });
    }
}
