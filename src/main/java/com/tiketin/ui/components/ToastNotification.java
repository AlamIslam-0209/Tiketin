package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinTheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Floating top-right toast banner with vector SVG icons.
 */
public final class ToastNotification {

    public enum Type {
        SUCCESS,
        ERROR,
        INFO
    }

    private ToastNotification() {}

    public static void success(Component parent, String message) {
        show(parent, message, Type.SUCCESS);
    }

    public static void error(Component parent, String message) {
        show(parent, message, Type.ERROR);
    }

    public static void info(Component parent, String message) {
        show(parent, message, Type.INFO);
    }

    public static void show(Component parent, String message, Type type) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> show(parent, message, type));
            return;
        }

        Window owner = parent instanceof Window
                ? (Window) parent
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);

        JWindow window = new JWindow(owner);

        Color bg;
        Color border;
        Color fg;
        String iconName;

        switch (type) {
            case SUCCESS:
                bg = TiketinTheme.SUCCESS_BG;
                border = TiketinTheme.SUCCESS_FG;
                fg = new Color(0x14, 0x53, 0x2D);
                iconName = "check";
                break;
            case ERROR:
                bg = TiketinTheme.DANGER_BG;
                border = TiketinTheme.DANGER_FG;
                fg = new Color(0x7F, 0x1D, 0x1D);
                iconName = "alert";
                break;
            case INFO:
            default:
                bg = TiketinTheme.BG_SURFACE;
                border = TiketinTheme.BORDER_STRONG;
                fg = TiketinTheme.TEXT_PRIMARY;
                iconName = "alert";
                break;
        }

        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(bg);
        panel.setBorder(new CompoundBorder(
                new LineBorder(border, 1, true),
                new EmptyBorder(10, 14, 10, 12)
        ));

        JLabel iconLabel = new JLabel(TiketinTheme.getIcon(iconName, 16, border));
        panel.add(iconLabel, BorderLayout.WEST);

        JLabel textLabel = new JLabel(message);
        textLabel.setFont(TiketinTheme.fontBold(12));
        textLabel.setForeground(fg);
        panel.add(textLabel, BorderLayout.CENTER);

        JButton closeBtn = new JButton(TiketinTheme.getIcon("close", 14, TiketinTheme.TEXT_MUTED));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(new EmptyBorder(2, 4, 2, 2));
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setToolTipText("Tutup notifikasi");
        panel.add(closeBtn, BorderLayout.EAST);

        window.getContentPane().add(panel);
        window.pack();

        int toastWidth = window.getWidth();
        int x;
        int y;

        if (owner != null && owner.isShowing()) {
            Point loc = owner.getLocationOnScreen();
            x = loc.x + owner.getWidth() - toastWidth - 20;
            y = loc.y + 42;
        } else {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            x = screen.width - toastWidth - 24;
            y = 40;
        }

        window.setLocation(x, y);
        window.setAlwaysOnTop(true);
        window.setVisible(true);

        Timer timer = new Timer(3200, e -> {
            window.setVisible(false);
            window.dispose();
        });
        timer.setRepeats(false);
        timer.start();

        closeBtn.addActionListener(e -> {
            timer.stop();
            window.setVisible(false);
            window.dispose();
        });
    }
}
