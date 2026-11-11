package com.tiketin.ui.dialogs;

import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.ProofStorageUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Clean modal for previewing uploaded or selected payment proofs.
 */
public class ProofViewerDialog extends JDialog {

    public static void showProof(Component parent, String proofFilename) {
        File resolved = ProofStorageUtil.findProofFile(proofFilename);
        showInternal(parent, proofFilename, resolved);
    }

    public static void showProof(Component parent, File localFile) {
        String name = localFile != null ? localFile.getName() : "-";
        showInternal(parent, name, localFile);
    }

    private static void showInternal(Component parent, String displayName, File targetFile) {
        Window owner = parent instanceof Window
                ? (Window) parent
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        ProofViewerDialog dialog = new ProofViewerDialog(owner, displayName, targetFile);
        dialog.setVisible(true);
    }

    public ProofViewerDialog(Window owner, String displayName, File targetFile) {
        super(owner, "Bukti Pembayaran", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        initUI(displayName, targetFile);
        pack();
        setLocationRelativeTo(owner);
        TiketinTheme.registerEscapeClose(this);
    }

    private void initUI(String displayName, File targetFile) {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(TiketinTheme.BG_APP);
        root.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("Bukti Pembayaran");
        title.setFont(TiketinTheme.fontBold(16));
        title.setForeground(TiketinTheme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        if (displayName != null && !displayName.isBlank()) {
            JLabel fileLabel = new JLabel(displayName);
            fileLabel.setFont(TiketinTheme.fontMono(11, Font.PLAIN));
            fileLabel.setForeground(TiketinTheme.TEXT_MUTED);
            header.add(fileLabel, BorderLayout.EAST);
        }
        root.add(header, BorderLayout.NORTH);

        root.add(createContentPanel(targetFile), BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setOpaque(false);
        JButton closeBtn = TiketinTheme.createSecondaryButton("Tutup");
        closeBtn.addActionListener(e -> dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JComponent createContentPanel(File targetFile) {
        if (targetFile != null && targetFile.exists()) {
            try {
                BufferedImage img = ImageIO.read(targetFile);
                if (img != null) {
                    double scale = Math.min(580.0 / img.getWidth(), 480.0 / img.getHeight());
                    if (scale > 1.0) {
                        scale = 1.0;
                    }
                    int w = Math.max(1, (int) Math.round(img.getWidth() * scale));
                    int h = Math.max(1, (int) Math.round(img.getHeight() * scale));

                    JLabel imgLabel = new JLabel(new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH)));
                    imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

                    JPanel card = TiketinTheme.createCardPanel(12, 12);
                    card.setLayout(new BorderLayout());
                    card.add(imgLabel, BorderLayout.CENTER);
                    return card;
                }
            } catch (Exception ignored) {}
        }

        JPanel empty = new JPanel();
        empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
        empty.setPreferredSize(new Dimension(440, 220));
        empty.setBackground(TiketinTheme.BG_SURFACE);
        empty.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(28, 24, 28, 24)
        ));

        JLabel iconLabel = new JLabel(TiketinTheme.getIcon("ticket", 36, TiketinTheme.TEXT_MUTED));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel msgLabel = new JLabel("File bukti pembayaran tidak ditemukan.");
        msgLabel.setFont(TiketinTheme.fontBold(14));
        msgLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        empty.add(Box.createVerticalGlue());
        empty.add(iconLabel);
        empty.add(Box.createVerticalStrut(10));
        empty.add(msgLabel);
        empty.add(Box.createVerticalGlue());
        return empty;
    }
}
