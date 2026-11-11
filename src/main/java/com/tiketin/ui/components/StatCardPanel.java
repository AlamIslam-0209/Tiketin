package com.tiketin.ui.components;

import com.tiketin.ui.theme.TiketinTheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Compact metric tile: muted label above, full-width numeric value below.
 */
public class StatCardPanel extends JPanel {

    private final JLabel valueLabel;
    private final JLabel titleLabel;

    public StatCardPanel(String value, String label) {
        setLayout(new BorderLayout(0, 6));
        setBackground(TiketinTheme.BG_SURFACE);
        setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        titleLabel = new JLabel(label != null ? label : "");
        titleLabel.setFont(TiketinTheme.fontRegular(12));
        titleLabel.setForeground(TiketinTheme.TEXT_MUTED);
        add(titleLabel, BorderLayout.NORTH);

        valueLabel = new JLabel(value != null ? value : "0");
        valueLabel.setFont(TiketinTheme.fontBold(22));
        valueLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        add(valueLabel, BorderLayout.CENTER);
    }

    public void setValue(String value) {
        valueLabel.setText(value != null ? value : "0");
        revalidate();
        repaint();
    }

    public void setLabel(String label) {
        titleLabel.setText(label != null ? label : "");
        revalidate();
        repaint();
    }
}
