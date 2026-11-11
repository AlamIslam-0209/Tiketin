package com.tiketin.ui.components;

import com.tiketin.model.Order;
import com.tiketin.ui.dialogs.ProofViewerDialog;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Clean ticket order card with wrapped ticket code pills and proper viewport width tracking.
 */
public class TicketCardPanel extends JPanel {

    public TicketCardPanel(Order order) {
        setLayout(new BorderLayout(0, 12));
        setBackground(TiketinTheme.BG_SURFACE);
        setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)
        ));

        // 1. Top Row: Category + Order Code + Status Badge
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        JPanel leftBadges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftBadges.setOpaque(false);
        String category = order.getEventCategory() != null && !order.getEventCategory().isBlank()
                ? order.getEventCategory().trim()
                : "Umum";
        leftBadges.add(TiketinTheme.createBadge(category, TiketinTheme.BG_SUBTLE, TiketinTheme.TEXT_SECONDARY));

        JLabel orderCodeLabel = new JLabel("#" + (order.getOrderCode() != null ? order.getOrderCode() : "-"));
        orderCodeLabel.setFont(TiketinTheme.fontMono(12, Font.BOLD));
        orderCodeLabel.setForeground(TiketinTheme.TEXT_MUTED);
        leftBadges.add(orderCodeLabel);

        topRow.add(leftBadges, BorderLayout.WEST);
        topRow.add(TiketinTheme.createStatusBadge(order.getStatus()), BorderLayout.EAST);
        add(topRow, BorderLayout.NORTH);

        // 2. Center: Event Title + 4 Info Columns
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        JLabel eventTitle = new JLabel(order.getEventName() != null ? order.getEventName() : "-");
        eventTitle.setFont(TiketinTheme.fontBold(16));
        eventTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        eventTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerPanel.add(eventTitle);

        centerPanel.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        String schedule = DateUtil.formatTanggalId(order.getEventDate())
                + (order.getEventTime() != null && !order.getEventTime().isBlank() ? " · " + order.getEventTime() : "");
        grid.add(createInfoColumn("Jadwal", schedule));
        grid.add(createInfoColumn("Lokasi", order.getEventLocation() != null ? order.getEventLocation() : "-"));

        String paymentInfo = order.getQuantity() + " tiket · " + (order.getPaymentMethod() != null ? order.getPaymentMethod() : "-");
        grid.add(createInfoColumn("Jumlah & Metode", paymentInfo));
        grid.add(createInfoColumn("Total", CurrencyUtil.formatRupiah(order.getTotalPrice())));

        centerPanel.add(grid);
        add(centerPanel, BorderLayout.CENTER);

        // 3. Bottom Row: Wrapped Ticket Codes + Proof Action
        JPanel bottomRow = new JPanel(new BorderLayout(12, 0));
        bottomRow.setOpaque(false);
        bottomRow.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TiketinTheme.BORDER),
                new EmptyBorder(10, 0, 0, 0)
        ));

        JPanel codesPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 6, 4));
        codesPanel.setOpaque(false);

        JLabel codeHeader = new JLabel("Kode tiket:");
        codeHeader.setFont(TiketinTheme.fontRegular(12));
        codeHeader.setForeground(TiketinTheme.TEXT_MUTED);
        codesPanel.add(codeHeader);

        String rawCodes = order.getTicketCodes();
        if (rawCodes != null && !rawCodes.trim().isEmpty()) {
            for (String code : rawCodes.split(",")) {
                String clean = code.trim();
                if (!clean.isEmpty()) {
                    JLabel codePill = new JLabel(clean);
                    codePill.setFont(TiketinTheme.fontMono(11, Font.BOLD));
                    codePill.setForeground(TiketinTheme.TEXT_PRIMARY);
                    codePill.setOpaque(true);
                    codePill.setBackground(TiketinTheme.BG_SUBTLE);
                    codePill.setBorder(new CompoundBorder(
                            new LineBorder(TiketinTheme.BORDER, 1, true),
                            new EmptyBorder(3, 8, 3, 8)
                    ));
                    codesPanel.add(codePill);
                }
            }
        } else {
            JLabel emptyCode = new JLabel("-");
            emptyCode.setFont(TiketinTheme.fontRegular(12));
            emptyCode.setForeground(TiketinTheme.TEXT_MUTED);
            codesPanel.add(emptyCode);
        }

        bottomRow.add(codesPanel, BorderLayout.CENTER);

        JButton proofBtn = TiketinTheme.createSecondaryButton("Lihat Bukti", "eye");
        proofBtn.addActionListener(e -> ProofViewerDialog.showProof(this, order.getPaymentProof()));
        JPanel proofWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 2));
        proofWrap.setOpaque(false);
        proofWrap.add(proofBtn);
        bottomRow.add(proofWrap, BorderLayout.EAST);

        add(bottomRow, BorderLayout.SOUTH);
    }

    private JPanel createInfoColumn(String label, String value) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(TiketinTheme.fontRegular(11));
        lbl.setForeground(TiketinTheme.TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(TiketinTheme.fontBold(12));
        val.setForeground(TiketinTheme.TEXT_PRIMARY);
        val.setToolTipText(value);

        col.add(lbl);
        col.add(Box.createVerticalStrut(3));
        col.add(val);
        return col;
    }
}
