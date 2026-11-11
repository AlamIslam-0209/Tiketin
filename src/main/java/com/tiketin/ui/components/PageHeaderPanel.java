package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Consistent single-row page header with title, optional 1-line subtitle, and action slot.
 */
public class PageHeaderPanel extends JPanel {

    private final JPanel actionsPanel;
    private final JLabel subtitleLabel;

    public PageHeaderPanel(String title, String subtitle) {
        setLayout(new BorderLayout(16, 0));
        setOpaque(false);

        JPanel textBox = new JPanel();
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(TiketinTheme.fontBold(20));
        titleLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textBox.add(titleLabel);

        subtitleLabel = new JLabel(subtitle != null ? subtitle : "");
        subtitleLabel.setFont(TiketinTheme.fontRegular(13));
        subtitleLabel.setForeground(TiketinTheme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (subtitle != null && !subtitle.isBlank()) {
            textBox.add(Box.createVerticalStrut(3));
            textBox.add(subtitleLabel);
        }

        add(textBox, BorderLayout.WEST);

        actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);
        add(actionsPanel, BorderLayout.EAST);
    }

    public void addAction(JComponent component) {
        actionsPanel.add(component);
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null ? subtitle : "");
    }
}
