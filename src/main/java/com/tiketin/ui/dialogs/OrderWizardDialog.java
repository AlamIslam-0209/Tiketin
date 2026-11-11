package com.tiketin.ui.dialogs;

import com.tiketin.dao.OrderDAO;
import com.tiketin.dao.PaymentDAO;
import com.tiketin.model.Event;
import com.tiketin.model.Payment;
import com.tiketin.model.User;
import com.tiketin.ui.components.ImageDropZonePanel;
import com.tiketin.ui.components.ToastNotification;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;
import com.tiketin.util.ProofStorageUtil;
import com.tiketin.util.SessionManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;

/**
 * Clean 2-step ticket order modal without wordy marketing text.
 */
public class OrderWizardDialog extends JDialog {

    private final Event event;
    private final Component parentComponent;
    private final OrderDAO orderDAO = new OrderDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardsPanel = new JPanel(cardLayout);

    private int quantity = 1;
    private final int maxQuantity;
    private String selectedPaymentMethod = "";
    private File selectedProofFile = null;
    private boolean orderSuccess = false;

    private JLabel qtyLabel;
    private JButton minusBtn;
    private JButton plusBtn;
    private JLabel step1TotalLabel;
    private JComboBox<String> paymentCombo;

    private JLabel step2MethodLabel;
    private JLabel step2TotalLabel;
    private JPanel paymentDetailSlot;
    private JPanel qrisCard;
    private JLabel qrisCodeLabel;
    private JPanel bankCard;
    private JLabel bankNameLabel;
    private JLabel bankAccNumberLabel;
    private JLabel bankAccNameLabel;
    private ImageDropZonePanel proofDropZone;
    private JButton confirmBtn;

    private JLabel step1Indicator;
    private JLabel step2Indicator;

    public static boolean showWizard(Component parent, Event event) {
        Window owner = parent instanceof Window
                ? (Window) parent
                : (parent != null ? SwingUtilities.getWindowAncestor(parent) : null);
        OrderWizardDialog dialog = new OrderWizardDialog(owner, parent, event);
        dialog.setVisible(true);
        return dialog.orderSuccess;
    }

    public OrderWizardDialog(Window owner, Component parentComponent, Event event) {
        super(owner, "Pesan Tiket", ModalityType.APPLICATION_MODAL);
        this.parentComponent = parentComponent;
        this.event = event;
        this.maxQuantity = Math.max(1, Math.min(10, event != null ? event.getSlots() : 1));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(520, 680);
        setMinimumSize(new Dimension(480, 580));
        setLocationRelativeTo(owner);

        initUI();
        TiketinTheme.registerEscapeClose(this);
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(TiketinTheme.BG_APP);
        root.setBorder(new EmptyBorder(18, 22, 18, 22));

        root.add(createStepperHeader(), BorderLayout.NORTH);

        cardsPanel.setOpaque(false);
        cardsPanel.add(createStep1Panel(), "STEP_1");
        cardsPanel.add(createStep2Panel(), "STEP_2");
        root.add(cardsPanel, BorderLayout.CENTER);

        setContentPane(root);
        goToStep(1);
    }

    private JPanel createStepperHeader() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setOpaque(false);

        step1Indicator = new JLabel("1. Jumlah & Metode", SwingConstants.CENTER);
        step2Indicator = new JLabel("2. Pembayaran & Bukti", SwingConstants.CENTER);

        panel.add(step1Indicator);
        panel.add(step2Indicator);
        return panel;
    }

    private void styleStepIndicator(JLabel label, boolean active) {
        label.setOpaque(true);
        label.setFont(active ? TiketinTheme.fontBold(12) : TiketinTheme.fontRegular(12));
        label.setBackground(active ? TiketinTheme.ACCENT_SOFT : TiketinTheme.BG_SURFACE);
        label.setForeground(active ? TiketinTheme.ACCENT_TEXT : TiketinTheme.TEXT_MUTED);
        label.setBorder(new CompoundBorder(
                new LineBorder(active ? TiketinTheme.ACCENT : TiketinTheme.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
    }

    private void goToStep(int step) {
        styleStepIndicator(step1Indicator, step == 1);
        styleStepIndicator(step2Indicator, step == 2);
        if (step == 1) {
            cardLayout.show(cardsPanel, "STEP_1");
        } else {
            updateStep2Data();
            cardLayout.show(cardsPanel, "STEP_2");
        }
    }

    private JPanel createStep1Panel() {
        JPanel step1 = new JPanel(new BorderLayout(0, 14));
        step1.setOpaque(false);

        JPanel summaryCard = TiketinTheme.createCardPanel(14, 16);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));

        JLabel eventTitle = new JLabel(event.getName());
        eventTitle.setFont(TiketinTheme.fontBold(16));
        eventTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        eventTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        String infoText = DateUtil.formatTanggalId(event.getDate())
                + (event.getTime() != null ? " · " + event.getTime() : "")
                + (event.getLocation() != null ? " · " + event.getLocation() : "");
        JLabel eventInfo = new JLabel(infoText);
        eventInfo.setFont(TiketinTheme.fontRegular(12));
        eventInfo.setForeground(TiketinTheme.TEXT_MUTED);
        eventInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel priceSlotRow = new JPanel(new BorderLayout());
        priceSlotRow.setOpaque(false);
        priceSlotRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel unitPriceLabel = new JLabel(CurrencyUtil.formatRupiah(event.getPrice()) + " / tiket");
        unitPriceLabel.setFont(TiketinTheme.fontBold(13));
        unitPriceLabel.setForeground(TiketinTheme.TEXT_PRIMARY);

        JLabel slotLeftLabel = new JLabel(event.getSlots() + " tersedia");
        slotLeftLabel.setFont(TiketinTheme.fontRegular(12));
        slotLeftLabel.setForeground(TiketinTheme.SUCCESS_FG);

        priceSlotRow.add(unitPriceLabel, BorderLayout.WEST);
        priceSlotRow.add(slotLeftLabel, BorderLayout.EAST);

        summaryCard.add(eventTitle);
        summaryCard.add(Box.createVerticalStrut(4));
        summaryCard.add(eventInfo);
        summaryCard.add(Box.createVerticalStrut(10));
        summaryCard.add(priceSlotRow);

        JPanel formCard = TiketinTheme.createCardPanel(16, 16);
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));

        JLabel qtyTitle = new JLabel("Jumlah tiket (maks. " + maxQuantity + ")");
        qtyTitle.setFont(TiketinTheme.fontBold(12));
        qtyTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        qtyTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel stepperBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        stepperBox.setOpaque(false);
        stepperBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        minusBtn = TiketinTheme.createSecondaryButton("−");
        minusBtn.setPreferredSize(new Dimension(40, 34));
        minusBtn.setEnabled(false);

        qtyLabel = new JLabel(String.valueOf(quantity), SwingConstants.CENTER);
        qtyLabel.setFont(TiketinTheme.fontBold(15));
        qtyLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        qtyLabel.setPreferredSize(new Dimension(48, 34));
        qtyLabel.setBorder(new LineBorder(TiketinTheme.BORDER, 1, true));

        plusBtn = TiketinTheme.createSecondaryButton("+");
        plusBtn.setPreferredSize(new Dimension(40, 34));
        plusBtn.setEnabled(quantity < maxQuantity);

        minusBtn.addActionListener(e -> {
            if (quantity > 1) {
                quantity--;
                updateQuantityState();
            }
        });
        plusBtn.addActionListener(e -> {
            if (quantity < maxQuantity) {
                quantity++;
                updateQuantityState();
            }
        });

        stepperBox.add(minusBtn);
        stepperBox.add(qtyLabel);
        stepperBox.add(plusBtn);

        paymentCombo = new JComboBox<>();
        paymentCombo.setFont(TiketinTheme.fontRegular(13));
        populatePaymentOptions();

        JPanel totalBox = new JPanel(new BorderLayout());
        totalBox.setOpaque(false);
        totalBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalBox.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, TiketinTheme.BORDER),
                new EmptyBorder(12, 0, 0, 0)
        ));

        JLabel totalDesc = new JLabel("Total pembayaran");
        totalDesc.setFont(TiketinTheme.fontRegular(13));
        totalDesc.setForeground(TiketinTheme.TEXT_SECONDARY);

        step1TotalLabel = new JLabel(CurrencyUtil.formatRupiah((long) event.getPrice() * quantity));
        step1TotalLabel.setFont(TiketinTheme.fontBold(18));
        step1TotalLabel.setForeground(TiketinTheme.TEXT_PRIMARY);

        totalBox.add(totalDesc, BorderLayout.WEST);
        totalBox.add(step1TotalLabel, BorderLayout.EAST);

        formCard.add(qtyTitle);
        formCard.add(stepperBox);
        formCard.add(Box.createVerticalStrut(10));
        formCard.add(TiketinTheme.createFieldGroup("Metode pembayaran", paymentCombo));
        formCard.add(Box.createVerticalStrut(14));
        formCard.add(totalBox);

        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);
        summaryCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerBox.add(summaryCard);
        centerBox.add(Box.createVerticalStrut(12));
        centerBox.add(formCard);

        step1.add(centerBox, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JButton cancelBtn = TiketinTheme.createSecondaryButton("Batal");
        cancelBtn.addActionListener(e -> dispose());

        JButton nextBtn = TiketinTheme.createPrimaryButton("Lanjut", "arrow-right");
        nextBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        nextBtn.addActionListener(e -> {
            selectedPaymentMethod = (String) paymentCombo.getSelectedItem();
            if (selectedPaymentMethod == null || selectedPaymentMethod.isBlank()) {
                selectedPaymentMethod = "QRIS";
            }
            goToStep(2);
        });

        actions.add(cancelBtn, BorderLayout.WEST);
        actions.add(nextBtn, BorderLayout.EAST);
        step1.add(actions, BorderLayout.SOUTH);

        return step1;
    }

    private void updateQuantityState() {
        qtyLabel.setText(String.valueOf(quantity));
        minusBtn.setEnabled(quantity > 1);
        plusBtn.setEnabled(quantity < maxQuantity);
        step1TotalLabel.setText(CurrencyUtil.formatRupiah((long) event.getPrice() * quantity));
    }

    private void populatePaymentOptions() {
        paymentCombo.removeAllItems();
        List<Payment> organizerPayments = paymentDAO.getPaymentsByOrganizer(event.getOwnerId());
        if (organizerPayments != null) {
            for (Payment p : organizerPayments) {
                paymentCombo.addItem(p.getBankName() + " — " + p.getAccountNumber() + " a.n " + p.getAccountName());
            }
        }
        paymentCombo.addItem("QRIS");
        paymentCombo.addItem("Bank BCA — 8271092831 a.n Tiketin Escrow");
        paymentCombo.addItem("Bank BRI — 01928374619 a.n Tiketin Escrow");
        paymentCombo.addItem("Bank Mandiri — 13200984712 a.n Tiketin Escrow");
        paymentCombo.addItem("Bank BSI — 7120938471 a.n Tiketin Escrow");
    }

    private JPanel createStep2Panel() {
        JPanel step2 = new JPanel(new BorderLayout(0, 14));
        step2.setOpaque(false);

        JPanel instructionCard = TiketinTheme.createCardPanel(14, 16);
        instructionCard.setLayout(new BoxLayout(instructionCard, BoxLayout.Y_AXIS));
        instructionCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel instrHeader = new JPanel(new BorderLayout(8, 0));
        instrHeader.setOpaque(false);
        instrHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        instrHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel instrTitle = new JLabel("Detail Pembayaran");
        instrTitle.setFont(TiketinTheme.fontBold(14));
        instrTitle.setForeground(TiketinTheme.TEXT_PRIMARY);

        JButton copyBtn = TiketinTheme.createSecondaryButton("Salin Info", "copy");
        copyBtn.setFont(TiketinTheme.fontBold(11));
        copyBtn.addActionListener(e -> copyPaymentInfo());

        instrHeader.add(instrTitle, BorderLayout.WEST);
        instrHeader.add(copyBtn, BorderLayout.EAST);

        step2MethodLabel = new JLabel("-");
        step2MethodLabel.setFont(TiketinTheme.fontRegular(12));
        step2MethodLabel.setForeground(TiketinTheme.TEXT_SECONDARY);
        step2MethodLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        step2TotalLabel = new JLabel("Rp 0");
        step2TotalLabel.setFont(TiketinTheme.fontBold(18));
        step2TotalLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        step2TotalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // QRIS Box
        qrisCard = new JPanel();
        qrisCard.setLayout(new BoxLayout(qrisCard, BoxLayout.Y_AXIS));
        qrisCard.setBackground(TiketinTheme.BG_SUBTLE);
        qrisCard.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)
        ));
        qrisCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel qrisLabel = new JLabel("Pembayaran QRIS");
        qrisLabel.setFont(TiketinTheme.fontBold(12));
        qrisLabel.setForeground(TiketinTheme.TEXT_PRIMARY);
        qrisLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        qrisCodeLabel = new JLabel("Memuat QR...", SwingConstants.CENTER);
        qrisCodeLabel.setPreferredSize(new Dimension(150, 150));
        qrisCodeLabel.setMaximumSize(new Dimension(150, 150));
        qrisCodeLabel.setBorder(new LineBorder(TiketinTheme.BORDER, 1, true));
        qrisCodeLabel.setOpaque(true);
        qrisCodeLabel.setBackground(Color.WHITE);
        qrisCodeLabel.setFont(TiketinTheme.fontRegular(12));
        qrisCodeLabel.setForeground(TiketinTheme.TEXT_MUTED);
        qrisCodeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel qrisCaption = new JLabel("Scan kode QR di atas, lalu unggah bukti pembayaran.");
        qrisCaption.setFont(TiketinTheme.fontRegular(11));
        qrisCaption.setForeground(TiketinTheme.TEXT_MUTED);
        qrisCaption.setAlignmentX(Component.CENTER_ALIGNMENT);

        qrisCard.add(qrisLabel);
        qrisCard.add(Box.createVerticalStrut(8));
        qrisCard.add(qrisCodeLabel);
        qrisCard.add(Box.createVerticalStrut(8));
        qrisCard.add(qrisCaption);

        // Bank Card
        bankCard = new JPanel(new BorderLayout(8, 6));
        bankCard.setBackground(TiketinTheme.BG_SUBTLE);
        bankCard.setBorder(new CompoundBorder(
                new LineBorder(TiketinTheme.BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
        bankCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        bankCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

        bankNameLabel = new JLabel("Bank Transfer");
        bankNameLabel.setFont(TiketinTheme.fontBold(12));
        bankNameLabel.setForeground(TiketinTheme.TEXT_SECONDARY);

        JPanel bankMid = new JPanel(new BorderLayout(8, 0));
        bankMid.setOpaque(false);
        bankAccNumberLabel = new JLabel("-");
        bankAccNumberLabel.setFont(TiketinTheme.fontMono(16, Font.BOLD));
        bankAccNumberLabel.setForeground(TiketinTheme.TEXT_PRIMARY);

        JButton copyAccBtn = TiketinTheme.createSecondaryButton("Salin", "copy");
        copyAccBtn.setFont(TiketinTheme.fontBold(11));
        copyAccBtn.addActionListener(e -> {
            String acc = bankAccNumberLabel.getText();
            if (acc != null && !acc.isBlank() && !"-".equals(acc)) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(acc), null);
                ToastNotification.info(this, "Nomor rekening disalin.");
            }
        });
        bankMid.add(bankAccNumberLabel, BorderLayout.CENTER);
        bankMid.add(copyAccBtn, BorderLayout.EAST);

        bankAccNameLabel = new JLabel("a.n -");
        bankAccNameLabel.setFont(TiketinTheme.fontRegular(12));
        bankAccNameLabel.setForeground(TiketinTheme.TEXT_MUTED);

        bankCard.add(bankNameLabel, BorderLayout.NORTH);
        bankCard.add(bankMid, BorderLayout.CENTER);
        bankCard.add(bankAccNameLabel, BorderLayout.SOUTH);

        paymentDetailSlot = new JPanel(new BorderLayout());
        paymentDetailSlot.setOpaque(false);
        paymentDetailSlot.setAlignmentX(Component.LEFT_ALIGNMENT);

        instructionCard.add(instrHeader);
        instructionCard.add(Box.createVerticalStrut(6));
        instructionCard.add(step2MethodLabel);
        instructionCard.add(Box.createVerticalStrut(2));
        instructionCard.add(step2TotalLabel);
        instructionCard.add(Box.createVerticalStrut(10));
        instructionCard.add(paymentDetailSlot);

        // Upload Proof Card (Drag & Drop + Native OS Picker)
        JPanel uploadCard = TiketinTheme.createCardPanel(14, 16);
        uploadCard.setLayout(new BorderLayout(0, 10));
        uploadCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel uploadHeader = new JPanel(new BorderLayout(8, 0));
        uploadHeader.setOpaque(false);

        JLabel uploadTitle = new JLabel("Bukti Pembayaran");
        uploadTitle.setFont(TiketinTheme.fontBold(13));
        uploadTitle.setForeground(TiketinTheme.TEXT_PRIMARY);
        uploadHeader.add(uploadTitle, BorderLayout.WEST);

        JButton pickBtn = TiketinTheme.createSecondaryButton("Pilih Gambar", "upload");
        pickBtn.setFont(TiketinTheme.fontBold(11));
        pickBtn.addActionListener(e -> proofDropZone.openFilePicker());
        uploadHeader.add(pickBtn, BorderLayout.EAST);

        uploadCard.add(uploadHeader, BorderLayout.NORTH);

        proofDropZone = new ImageDropZonePanel(file -> {
            selectedProofFile = file;
            pickBtn.setVisible(file == null);
        });
        uploadCard.add(proofDropZone, BorderLayout.CENTER);

        JPanel centerBox = new JPanel(new GridBagLayout());
        centerBox.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 12, 0);
        centerBox.add(instructionCard, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        centerBox.add(uploadCard, gbc);

        JPanel northWrap = TiketinTheme.createScrollableVerticalPanel();
        northWrap.setLayout(new BorderLayout());
        northWrap.setBackground(TiketinTheme.BG_APP);
        northWrap.add(centerBox, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(northWrap);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(TiketinTheme.BG_APP);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        step2.add(scrollPane, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);

        JButton backBtn = TiketinTheme.createSecondaryButton("Kembali", "arrow-left");
        backBtn.addActionListener(e -> goToStep(1));

        confirmBtn = TiketinTheme.createPrimaryButton("Konfirmasi Pesanan");
        confirmBtn.addActionListener(e -> submitOrder());

        actions.add(backBtn, BorderLayout.WEST);
        actions.add(confirmBtn, BorderLayout.EAST);
        step2.add(actions, BorderLayout.SOUTH);

        return step2;
    }

    private void updateStep2Data() {
        step2MethodLabel.setText(selectedPaymentMethod);
        long total = (long) event.getPrice() * quantity;
        step2TotalLabel.setText(CurrencyUtil.formatRupiah(total));

        boolean isQris = selectedPaymentMethod != null && selectedPaymentMethod.toUpperCase().contains("QRIS");
        paymentDetailSlot.removeAll();
        if (isQris) {
            paymentDetailSlot.add(qrisCard, BorderLayout.CENTER);
            loadQrisCode();
        } else {
            parseAndDisplayBankInfo(selectedPaymentMethod);
            paymentDetailSlot.add(bankCard, BorderLayout.CENTER);
        }
        paymentDetailSlot.invalidate();
        Container p = paymentDetailSlot.getParent();
        while (p != null && p != cardsPanel) {
            p.invalidate();
            p = p.getParent();
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void loadQrisCode() {
        long total = (long) event.getPrice() * quantity;
        String qrData = String.format("TIKETIN | %s | %d Tiket | %s | %d",
                event.getName(), quantity, CurrencyUtil.formatRupiah(total), event.getId());

        qrisCodeLabel.setIcon(null);
        qrisCodeLabel.setText("Memuat QR...");

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                try {
                    String encoded = URLEncoder.encode(qrData, StandardCharsets.UTF_8);
                    URI uri = URI.create("https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=" + encoded);
                    BufferedImage img = ImageIO.read(uri.toURL());
                    if (img != null) {
                        return new ImageIcon(img);
                    }
                } catch (Exception ignored) {}
                return new ImageIcon(generateFallbackQrImage(qrData, 150, 150));
            }

            @Override
            protected void done() {
                try {
                    qrisCodeLabel.setText("");
                    qrisCodeLabel.setIcon(get());
                } catch (Exception e) {
                    qrisCodeLabel.setText("");
                    qrisCodeLabel.setIcon(new ImageIcon(generateFallbackQrImage(qrData, 150, 150)));
                }
            }
        }.execute();
    }

    private void parseAndDisplayBankInfo(String method) {
        String bankName = "Transfer Bank";
        String accNum = "-";
        String accName = "-";

        if (method != null && method.contains("—")) {
            String[] parts = method.split("—", 2);
            bankName = parts[0].trim();
            String right = parts[1].trim();
            if (right.contains("a.n")) {
                String[] accParts = right.split("a\\.n", 2);
                accNum = accParts[0].trim();
                accName = accParts[1].trim();
            } else {
                accNum = right;
            }
        } else if (method != null && !method.isBlank()) {
            bankName = method;
        }

        bankNameLabel.setText(bankName);
        bankAccNumberLabel.setText(accNum);
        bankAccNameLabel.setText("a.n " + accName);
    }

    private BufferedImage generateFallbackQrImage(String data, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        int modules = 25;
        int margin = 12;
        double cellSize = (double) (width - margin * 2) / modules;

        g2.setColor(TiketinTheme.TEXT_PRIMARY);
        drawFinderPattern(g2, margin, margin, cellSize);
        drawFinderPattern(g2, margin + (int) ((modules - 7) * cellSize), margin, cellSize);
        drawFinderPattern(g2, margin, margin + (int) ((modules - 7) * cellSize), cellSize);

        Random rnd = new Random(data != null ? data.hashCode() : 42L);
        for (int r = 0; r < modules; r++) {
            for (int c = 0; c < modules; c++) {
                boolean skip = (r < 8 && c < 8) || (r < 8 && c >= modules - 8) || (r >= modules - 8 && c < 8) || r == 6 || c == 6;
                if (!skip && rnd.nextBoolean()) {
                    g2.fillRect(margin + (int) (c * cellSize), margin + (int) (r * cellSize),
                            (int) Math.ceil(cellSize), (int) Math.ceil(cellSize));
                }
            }
        }
        g2.dispose();
        return image;
    }

    private void drawFinderPattern(Graphics2D g2, int startX, int startY, double cellSize) {
        int s7 = (int) Math.round(7 * cellSize);
        int s5 = (int) Math.round(5 * cellSize);
        int s3 = (int) Math.round(3 * cellSize);
        int o1 = (int) Math.round(cellSize);
        int o2 = (int) Math.round(2 * cellSize);

        g2.setColor(TiketinTheme.TEXT_PRIMARY);
        g2.fillRect(startX, startY, s7, s7);
        g2.setColor(Color.WHITE);
        g2.fillRect(startX + o1, startY + o1, s5, s5);
        g2.setColor(TiketinTheme.TEXT_PRIMARY);
        g2.fillRect(startX + o2, startY + o2, s3, s3);
    }

    private void copyPaymentInfo() {
        String info = "Event: " + event.getName() + "\n"
                + "Metode: " + selectedPaymentMethod + "\n"
                + "Jumlah: " + quantity + " tiket\n"
                + "Total: " + CurrencyUtil.formatRupiah((long) event.getPrice() * quantity);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(info), null);
        ToastNotification.info(this, "Detail pembayaran disalin.");
    }

    private void submitOrder() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            ToastNotification.error(this, "Silakan masuk terlebih dahulu.");
            return;
        }
        if (selectedProofFile == null || !selectedProofFile.exists()) {
            ToastNotification.error(this, "Unggah bukti pembayaran terlebih dahulu.");
            return;
        }

        confirmBtn.setEnabled(false);
        new SwingWorker<String, Void>() {
            private String errorMsg;

            @Override
            protected String doInBackground() {
                try {
                    String savedFilename = ProofStorageUtil.saveProof(selectedProofFile);
                    return orderDAO.createOrderAtomic(
                            user.getId(),
                            event.getId(),
                            quantity,
                            selectedPaymentMethod,
                            savedFilename
                    );
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                confirmBtn.setEnabled(true);
                try {
                    String orderCode = get();
                    if (orderCode != null) {
                        ToastNotification.success(parentComponent != null ? parentComponent : OrderWizardDialog.this,
                                "Pesanan dibuat (" + orderCode + ").");
                        orderSuccess = true;
                        dispose();
                    } else {
                        ToastNotification.error(OrderWizardDialog.this,
                                errorMsg != null ? errorMsg : "Gagal membuat pesanan.");
                    }
                } catch (Exception ex) {
                    ToastNotification.error(OrderWizardDialog.this, "Gagal membuat pesanan: " + ex.getMessage());
                }
            }
        }.execute();
    }
}
