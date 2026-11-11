package com.tiketin.ui.components;

import com.tiketin.ui.dialogs.ProofViewerDialog;
import com.tiketin.ui.theme.TiketinTheme;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Interactive image upload dropzone with Drag & Drop support, native OS photo picker, and live thumbnail preview.
 */
public class ImageDropZonePanel extends JPanel {

    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(
            Arrays.asList("jpg", "jpeg", "png", "webp", "gif")
    );

    private final Consumer<File> onFileChanged;
    private File selectedFile;
    private boolean dragActive = false;
    private boolean hoverActive = false;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    // Empty state widgets
    private JLabel emptyIconLabel;
    private JLabel emptyTitleLabel;

    // Selected state widgets
    private JLabel previewThumbLabel;
    private JLabel fileNameLabel;
    private JLabel fileMetaLabel;

    public ImageDropZonePanel(Consumer<File> onFileChanged) {
        this.onFileChanged = onFileChanged;
        setLayout(new BorderLayout());
        setOpaque(false);

        cards.setOpaque(false);
        cards.add(createEmptyStatePanel(), "EMPTY");
        cards.add(createSelectedStatePanel(), "SELECTED");
        add(cards, BorderLayout.CENTER);

        installDropTarget(this);
        showEmptyState();
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedFile(File file) {
        if (file == null) {
            clear();
            return;
        }
        applyFile(file);
    }

    public void clear() {
        selectedFile = null;
        previewThumbLabel.setIcon(null);
        previewThumbLabel.setText("");
        showEmptyState();
        if (onFileChanged != null) {
            onFileChanged.accept(null);
        }
    }

    private JPanel createEmptyStatePanel() {
        JPanel dropBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                Color fill = dragActive
                        ? TiketinTheme.ACCENT_SOFT
                        : (hoverActive ? TiketinTheme.BG_SUBTLE : TiketinTheme.BG_APP);
                Color strokeColor = dragActive ? TiketinTheme.ACCENT : TiketinTheme.BORDER_STRONG;

                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(1, 1, w - 2, h - 2, 10, 10));

                g2.setColor(strokeColor);
                float[] dash = {5f, 4f};
                g2.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, dash, 0f));
                g2.draw(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 10, 10));
                g2.dispose();
            }
        };
        dropBox.setOpaque(false);
        dropBox.setLayout(new BoxLayout(dropBox, BoxLayout.Y_AXIS));
        dropBox.setBorder(new EmptyBorder(20, 16, 20, 16));
        dropBox.setPreferredSize(new Dimension(0, 114));
        dropBox.setCursor(new Cursor(Cursor.HAND_CURSOR));

        emptyIconLabel = new JLabel(TiketinTheme.getIcon("upload", 22, TiketinTheme.TEXT_MUTED));
        emptyIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        emptyTitleLabel = new JLabel("Seret foto bukti ke sini, atau klik untuk memilih");
        emptyTitleLabel.setFont(TiketinTheme.fontBold(12));
        emptyTitleLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        emptyTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("JPG, PNG, WEBP, GIF · maks. 5 MB");
        subLabel.setFont(TiketinTheme.fontRegular(11));
        subLabel.setForeground(TiketinTheme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        dropBox.add(Box.createVerticalGlue());
        dropBox.add(emptyIconLabel);
        dropBox.add(Box.createVerticalStrut(8));
        dropBox.add(emptyTitleLabel);
        dropBox.add(Box.createVerticalStrut(3));
        dropBox.add(subLabel);
        dropBox.add(Box.createVerticalGlue());

        dropBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverActive = true;
                dropBox.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverActive = false;
                dropBox.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                openFilePicker();
            }
        });

        installDropTarget(dropBox);
        return dropBox;
    }

    private JPanel createSelectedStatePanel() {
        JPanel card = new JPanel(new BorderLayout(14, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (dragActive) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xC2, 0x41, 0x2D, 25));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(TiketinTheme.ACCENT);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                    g2.dispose();
                }
            }
        };
        card.setBackground(TiketinTheme.BG_SURFACE);
        card.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));

        previewThumbLabel = new JLabel("", SwingConstants.CENTER);
        previewThumbLabel.setPreferredSize(new Dimension(136, 92));
        previewThumbLabel.setOpaque(true);
        previewThumbLabel.setBackground(TiketinTheme.BG_SUBTLE);
        previewThumbLabel.setBorder(new LineBorder(TiketinTheme.BORDER, 1, true));
        previewThumbLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        previewThumbLabel.setToolTipText("Klik untuk memperbesar gambar");
        previewThumbLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (selectedFile != null && selectedFile.exists()) {
                    ProofViewerDialog.showProof(ImageDropZonePanel.this, selectedFile);
                }
            }
        });
        card.add(previewThumbLabel, BorderLayout.WEST);

        JPanel infoCol = new JPanel(new GridBagLayout());
        infoCol.setOpaque(false);

        fileNameLabel = new JLabel("-");
        fileNameLabel.setFont(TiketinTheme.fontBold(13));
        fileNameLabel.setForeground(TiketinTheme.TEXT_PRIMARY);

        fileMetaLabel = new JLabel("-");
        fileMetaLabel.setFont(TiketinTheme.fontRegular(11));
        fileMetaLabel.setForeground(TiketinTheme.TEXT_MUTED);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionRow.setOpaque(false);

        JButton changeBtn = TiketinTheme.createSecondaryButton("Ganti", "upload");
        changeBtn.setFont(TiketinTheme.fontBold(11));
        changeBtn.addActionListener(e -> openFilePicker());

        JButton removeBtn = TiketinTheme.createSecondaryButton("Hapus", "trash");
        removeBtn.setFont(TiketinTheme.fontBold(11));
        removeBtn.addActionListener(e -> clear());

        actionRow.add(changeBtn);
        actionRow.add(removeBtn);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 3, 0);
        infoCol.add(fileNameLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 10, 0);
        infoCol.add(fileMetaLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        infoCol.add(actionRow, gbc);

        card.add(infoCol, BorderLayout.CENTER);
        installDropTarget(card);
        return card;
    }

    private void showEmptyState() {
        cardLayout.show(cards, "EMPTY");
        revalidate();
        repaint();
    }

    private void showSelectedState() {
        cardLayout.show(cards, "SELECTED");
        revalidate();
        repaint();
    }

    public void openFilePicker() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac") || os.contains("win")) {
            openNativeFileDialog();
        } else {
            openSwingPreviewChooser();
        }
    }

    private void openNativeFileDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        FileDialog fd;
        if (owner instanceof Dialog) {
            fd = new FileDialog((Dialog) owner, "Pilih Bukti Pembayaran", FileDialog.LOAD);
        } else if (owner instanceof Frame) {
            fd = new FileDialog((Frame) owner, "Pilih Bukti Pembayaran", FileDialog.LOAD);
        } else {
            fd = new FileDialog((Frame) null, "Pilih Bukti Pembayaran", FileDialog.LOAD);
        }

        System.setProperty("apple.awt.fileDialogForDirectories", "false");
        fd.setMultipleMode(false);
        fd.setFilenameFilter((dir, name) -> isSupportedExtension(name));
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            fd.setFile("*.jpg;*.jpeg;*.png;*.webp;*.gif");
        }
        fd.setVisible(true);

        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            applyFile(new File(dir, file));
        }
    }

    private void openSwingPreviewChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Pilih Bukti Pembayaran");
        chooser.setFileFilter(new FileNameExtensionFilter("Gambar (JPG, PNG, WEBP, GIF)", "jpg", "jpeg", "png", "webp", "gif"));

        JLabel accessory = new JLabel("Pilih gambar", SwingConstants.CENTER);
        accessory.setPreferredSize(new Dimension(180, 160));
        accessory.setBorder(new CompoundBorder(
                new EmptyBorder(0, 8, 0, 0),
                new LineBorder(TiketinTheme.BORDER, 1, true)
        ));
        chooser.setAccessory(accessory);
        chooser.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, evt -> {
            File f = (File) evt.getNewValue();
            if (f != null && f.isFile() && isSupportedExtension(f.getName())) {
                ImageIcon thumb = buildScaledIcon(f, 164, 144);
                if (thumb != null) {
                    accessory.setIcon(thumb);
                    accessory.setText("");
                    return;
                }
            }
            accessory.setIcon(null);
            accessory.setText("Pratinjau");
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            applyFile(chooser.getSelectedFile());
        }
    }

    private void installDropTarget(Component comp) {
        new DropTarget(comp, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                handleDrag(dtde);
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                handleDrag(dtde);
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                setDragState(false);
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                setDragState(false);
                try {
                    Transferable t = dtde.getTransferable();
                    if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                        dtde.dropComplete(true);
                        if (files != null && !files.isEmpty()) {
                            SwingUtilities.invokeLater(() -> applyFile(files.get(0)));
                        }
                        return;
                    }
                    if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        String raw = (String) t.getTransferData(DataFlavor.stringFlavor);
                        dtde.dropComplete(true);
                        File uriFile = parseFirstFileUri(raw);
                        if (uriFile != null) {
                            SwingUtilities.invokeLater(() -> applyFile(uriFile));
                        } else {
                            ToastNotification.error(ImageDropZonePanel.this, "File yang dijatuhkan tidak valid.");
                        }
                        return;
                    }
                    dtde.rejectDrop();
                } catch (Exception ex) {
                    try {
                        dtde.dropComplete(false);
                    } catch (Exception ignored) {}
                    ToastNotification.error(ImageDropZonePanel.this, "Gagal membaca file yang dijatuhkan.");
                }
            }
        }, true);
    }

    private void handleDrag(DropTargetDragEvent dtde) {
        Transferable t = dtde.getTransferable();
        boolean supported = t != null && (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                || t.isDataFlavorSupported(DataFlavor.stringFlavor));
        if (supported) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY);
            setDragState(true);
        } else {
            dtde.rejectDrag();
            setDragState(false);
        }
    }

    private void setDragState(boolean active) {
        if (this.dragActive == active) {
            return;
        }
        this.dragActive = active;
        if (emptyIconLabel != null) {
            emptyIconLabel.setIcon(TiketinTheme.getIcon("upload", 22,
                    active ? TiketinTheme.ACCENT : TiketinTheme.TEXT_MUTED));
        }
        if (emptyTitleLabel != null) {
            emptyTitleLabel.setText(active
                    ? "Lepaskan foto di sini"
                    : "Seret foto bukti ke sini, atau klik untuk memilih");
        }
        repaint();
    }

    private void applyFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            ToastNotification.error(this, "File tidak ditemukan.");
            return;
        }
        if (!isSupportedExtension(file.getName())) {
            ToastNotification.error(this, "Format harus JPG, PNG, WEBP, atau GIF.");
            return;
        }
        if (file.length() > MAX_FILE_BYTES) {
            ToastNotification.error(this, "Ukuran file maksimal 5 MB.");
            return;
        }

        String ext = getExtension(file.getName()).toUpperCase(Locale.ROOT);
        ImageIcon thumb = buildScaledIcon(file, 132, 88);
        if (thumb == null && !"WEBP".equals(ext)) {
            ToastNotification.error(this, "File gambar rusak atau tidak dapat dibaca.");
            return;
        }

        this.selectedFile = file;
        fileNameLabel.setText(file.getName());
        fileNameLabel.setToolTipText(file.getAbsolutePath());
        long kb = Math.max(1, file.length() / 1024);
        fileMetaLabel.setText(kb + " KB · " + ext + (thumb != null ? " · Klik gambar untuk perbesar" : ""));

        if (thumb != null) {
            previewThumbLabel.setIcon(thumb);
            previewThumbLabel.setText("");
        } else {
            previewThumbLabel.setIcon(null);
            previewThumbLabel.setText(ext);
            previewThumbLabel.setFont(TiketinTheme.fontBold(12));
            previewThumbLabel.setForeground(TiketinTheme.TEXT_SECONDARY);
        }

        showSelectedState();
        if (onFileChanged != null) {
            onFileChanged.accept(file);
        }
    }

    private static ImageIcon buildScaledIcon(File file, int maxW, int maxH) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                return null;
            }
            double scale = Math.min((double) maxW / img.getWidth(), (double) maxH / img.getHeight());
            if (scale > 1.0) {
                scale = 1.0;
            }
            int tw = Math.max(1, (int) Math.round(img.getWidth() * scale));
            int th = Math.max(1, (int) Math.round(img.getHeight() * scale));

            BufferedImage scaled = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(img, 0, 0, tw, th, null);
            g2.dispose();
            return new ImageIcon(scaled);
        } catch (Exception e) {
            return null;
        }
    }

    private static File parseFirstFileUri(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (String line : raw.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("file://")) {
                try {
                    return new File(new URI(trimmed));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static boolean isSupportedExtension(String filename) {
        return ALLOWED_EXTENSIONS.contains(getExtension(filename));
    }

    private static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
