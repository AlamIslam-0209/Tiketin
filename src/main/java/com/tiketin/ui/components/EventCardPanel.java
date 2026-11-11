package com.tiketin.ui.components;

import com.tiketin.model.Event;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Clean event catalog card without placeholder filler text.
 */
public class EventCardPanel extends JPanel {

    public EventCardPanel(Event event, Consumer<Event> onBookClicked) {
        setLayout(new BorderLayout(0, 12));
        setBackground(TiketinTheme.BG_SURFACE);
        setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        // Top: Category chip + Organizer name
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        String category = event.getCategory() != null && !event.getCategory().isBlank()
                ? event.getCategory().trim()
                : "Umum";
        JLabel categoryBadge = TiketinTheme.createBadge(category, TiketinTheme.BG_SUBTLE, TiketinTheme.TEXT_SECONDARY);
        topRow.add(categoryBadge, BorderLayout.WEST);

        String orgName = event.getOwnerOrg() != null && !event.getOwnerOrg().isBlank()
                ? event.getOwnerOrg().trim()
                : (event.getOwnerName() != null ? event.getOwnerName().trim() : "");
        if (!orgName.isEmpty()) {
            JLabel orgLabel = new JLabel(orgName);
            orgLabel.setFont(TiketinTheme.fontRegular(12));
            orgLabel.setForeground(TiketinTheme.TEXT_MUTED);
            orgLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            topRow.add(orgLabel, BorderLayout.CENTER);
        }
        add(topRow, BorderLayout.NORTH);

        // Center: Title + concise description + metadata rows
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(event.getName() != null ? event.getName() : "-");
        titleLabel.setFont(TiketinTheme.fontBold(15));
        titleLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(titleLabel);

        String desc = event.getDescription() != null ? event.getDescription().trim() : "";
        if (!desc.isEmpty()) {
            if (desc.length() > 64) {
                int cut = desc.lastIndexOf(' ', 61);
                desc = desc.substring(0, cut > 30 ? cut : 61).trim() + "...";
            }
            centerPanel.add(Box.createVerticalStrut(4));
            JLabel descLabel = new JLabel(desc);
            descLabel.setFont(TiketinTheme.fontRegular(12));
            descLabel.setForeground(TiketinTheme.TEXT_MUTED);
            descLabel.setToolTipText(event.getDescription().trim());
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            centerPanel.add(descLabel);
        }

        centerPanel.add(Box.createVerticalStrut(12));

        JPanel metaPanel = new JPanel();
        metaPanel.setLayout(new BoxLayout(metaPanel, BoxLayout.Y_AXIS));
        metaPanel.setOpaque(false);
        metaPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String schedule = DateUtil.formatTanggalId(event.getDate())
                + (event.getTime() != null && !event.getTime().isBlank() ? " · " + event.getTime() : "");
        metaPanel.add(createMetaLine("calendar", schedule, TiketinTheme.TEXT_SECONDARY));
        metaPanel.add(Box.createVerticalStrut(5));
        metaPanel.add(createMetaLine("location", event.getLocation() != null ? event.getLocation() : "-", TiketinTheme.TEXT_SECONDARY));
        metaPanel.add(Box.createVerticalStrut(5));

        boolean hasSlots = event.getSlots() > 0;
        String slotText = hasSlots ? (event.getSlots() + " tiket tersedia") : "Habis";
        Color slotColor = hasSlots ? TiketinTheme.SUCCESS_FG : TiketinTheme.DANGER_FG;
        metaPanel.add(createMetaLine("ticket", slotText, slotColor));

        centerPanel.add(metaPanel);
        add(centerPanel, BorderLayout.CENTER);

        // Footer: Price + Book Action
        JPanel footerRow = new JPanel(new BorderLayout(8, 0));
        footerRow.setOpaque(false);
        footerRow.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TiketinTheme.BORDER),
                new EmptyBorder(12, 0, 0, 0)
        ));

        JLabel priceLabel = new JLabel(CurrencyUtil.formatRupiah(event.getPrice()));
        priceLabel.setFont(TiketinTheme.fontBold(16));
        priceLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        footerRow.add(priceLabel, BorderLayout.WEST);

        JButton bookBtn = TiketinTheme.createPrimaryButton(hasSlots ? "Pesan" : "Habis");
        bookBtn.setEnabled(hasSlots);
        if (hasSlots) {
            bookBtn.addActionListener(e -> {
                if (onBookClicked != null) {
                    onBookClicked.accept(event);
                }
            });
        }
        footerRow.add(bookBtn, BorderLayout.EAST);

        add(footerRow, BorderLayout.SOUTH);
    }

    private JLabel createMetaLine(String iconName, String text, Color textColor) {
        JLabel label = new JLabel(text);
        label.setIcon(TiketinTheme.getIcon(iconName, 14, TiketinTheme.TEXT_MUTED));
        label.setIconTextGap(8);
        label.setFont(TiketinTheme.fontRegular(12));
        label.setForeground(textColor);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }
}
