package com.tiketin.ui.dialogs;

import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * Modal for entering mandatory rejection reason when declining an organizer application.
 */
public class RejectReasonDialog extends JDialog {

    private String submittedReason = null;
    private final JTextArea reasonArea;

    public static String showDialog(Component parent, String organizationName) {
        Window owner = parent instanceof Window
                ? (Window) parent
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        RejectReasonDialog dialog = new RejectReasonDialog(owner, organizationName);
        dialog.setVisible(true);
        return dialog.submittedReason;
    }

    public RejectReasonDialog(Window owner, String organizationName) {
        super(owner, "Tolak Pengajuan", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(TiketinTheme.BG_SURFACE);
        content.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel titleLabel = new JLabel("Tolak Pengajuan");
        titleLabel.setFont(TiketinTheme.fontBold(16));
        titleLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLabel);

        content.add(Box.createVerticalStrut(4));
        String orgText = organizationName != null && !organizationName.isBlank() ? organizationName : "-";
        JLabel orgLabel = new JLabel(orgText + " — alasan akan ditampilkan ke pemohon.");
        orgLabel.setFont(TiketinTheme.fontRegular(12));
        orgLabel.setForeground(TiketinTheme.TEXT_MUTED);
        orgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(orgLabel);

        content.add(Box.createVerticalStrut(12));

        reasonArea = new JTextArea(4, 34);
        reasonArea.setFont(TiketinTheme.fontRegular(13));
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        reasonArea.setBorder(new EmptyBorder(8, 10, 8, 10));

        JScrollPane scrollPane = new JScrollPane(reasonArea);
        scrollPane.setBorder(new LineBorder(TiketinTheme.BORDER_STRONG, 1, true));
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(scrollPane);

        content.add(Box.createVerticalStrut(16));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancelBtn = TiketinTheme.createSecondaryButton("Batal");
        cancelBtn.addActionListener(e -> {
            submittedReason = null;
            dispose();
        });

        JButton rejectBtn = TiketinTheme.createDangerButton("Tolak Pengajuan");
        rejectBtn.addActionListener(e -> {
            String text = reasonArea.getText().trim();
            if (text.isEmpty()) {
                ToastNotification.error(this, "Alasan penolakan wajib diisi.");
                reasonArea.requestFocus();
                return;
            }
            submittedReason = text;
            dispose();
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(rejectBtn);
        content.add(buttonPanel);

        setContentPane(content);
        pack();
        setLocationRelativeTo(owner);
        TiketinTheme.registerEscapeClose(this);
    }
}
