package com.tiketin.ui.components;

import com.tiketin.model.Event;
import com.tiketin.ui.MainFrame;
import com.tiketin.ui.theme.TiketinIcons;
import com.tiketin.ui.theme.TiketinTheme;
import com.tiketin.util.CurrencyUtil;
import com.tiketin.util.DateUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;

import java.time.LocalDate;

public class EventCardPanel extends VBox {
    private static final double DEFAULT_BANNER_HEIGHT = 204.0;

    private final MainFrame mainFrame;
    private final Event event;
    private final Runnable onBooked;

    public EventCardPanel(Event event, Runnable onBooked) {
        this(null, event, onBooked);
    }

    public EventCardPanel(MainFrame mainFrame, Event event, Runnable onBooked) {
        this.mainFrame = mainFrame;
        this.event = event;
        this.onBooked = onBooked;

        setSpacing(0);
        setMinWidth(210);
        setPrefWidth(Region.USE_COMPUTED_SIZE);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("card-editorial");

        String category = event.getCategory() != null && !event.getCategory().isBlank() ? event.getCategory().trim() : "Umum";
        String catLower = category.toLowerCase();

        // Wave 1: Responsive 4:3 Category Vector Banner on Top (upgraded to real 4:3 photo in Wave 2)
        StackPane bannerPane = new StackPane();
        bannerPane.setMinWidth(0);
        bannerPane.setPrefWidth(0);
        bannerPane.setMaxWidth(Double.MAX_VALUE);
        bannerPane.setMinHeight(DEFAULT_BANNER_HEIGHT);
        bannerPane.setPrefHeight(DEFAULT_BANNER_HEIGHT);
        bannerPane.setMaxHeight(DEFAULT_BANNER_HEIGHT);
        bannerPane.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW != null ? newW.doubleValue() : 0;
            if (w <= 20) return;
            double target = Math.round(Math.max(172.0, Math.min(260.0, w * 0.75)));
            if (Math.abs(bannerPane.getPrefHeight() - target) >= 2.0) {
                javafx.application.Platform.runLater(() -> {
                    bannerPane.setMinHeight(target);
                    bannerPane.setPrefHeight(target);
                    bannerPane.setMaxHeight(target);
                });
            }
        });

        bannerPane.setStyle("-fx-background-color: " + resolveBannerBg(catLower) + ";");

        StackPane mediaHolder = new StackPane();
        mediaHolder.setMinWidth(0);
        mediaHolder.setPrefWidth(0);
        mediaHolder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Path topClip = new Path();
        topClip.setFill(Color.BLACK);
        topClip.setStroke(null);
        bannerPane.setClip(topClip);

        Canvas bannerCanvas = new Canvas(320, DEFAULT_BANNER_HEIGHT);
        bannerCanvas.setManaged(false);
        mediaHolder.getChildren().add(bannerCanvas);

        Runnable redrawBanner = () -> {
            double w = bannerPane.getWidth();
            double h = bannerPane.getHeight();
            if (w > 0 && h > 0) {
                updateTopRoundedClipLocal(topClip, w, h, 14.0);
                bannerCanvas.setWidth(w);
                bannerCanvas.setHeight(h);
                drawCategoryVectorArt(bannerCanvas.getGraphicsContext2D(), w, h, catLower);
            }
        };
        bannerPane.widthProperty().addListener((obs, o, n) -> redrawBanner.run());
        bannerPane.heightProperty().addListener((obs, o, n) -> redrawBanner.run());
        drawCategoryVectorArt(bannerCanvas.getGraphicsContext2D(), 320, DEFAULT_BANNER_HEIGHT, catLower);

        // Floating Pills over Banner
        HBox topPillsRow = new HBox(8);
        topPillsRow.setMinWidth(0);
        topPillsRow.setPrefWidth(0);
        topPillsRow.setMaxWidth(Double.MAX_VALUE);
        topPillsRow.setPadding(new Insets(12));
        topPillsRow.setAlignment(Pos.TOP_LEFT);

        HBox catPill = new HBox(6);
        catPill.setAlignment(Pos.CENTER);
        catPill.setPadding(new Insets(4, 10, 4, 8));
        catPill.setMinWidth(Region.USE_PREF_SIZE);
        catPill.setMaxHeight(Region.USE_PREF_SIZE);
        catPill.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 999; -fx-border-color: #E3E6E2; -fx-border-radius: 999; -fx-border-width: 1;");

        Circle dot = new Circle(4, resolveCategoryDotColor(catLower));
        Label catLabel = new Label(category);
        catLabel.setFont(TiketinTheme.fontBold(11.5));
        catLabel.setStyle("-fx-text-fill: #20252A;");
        catPill.getChildren().addAll(dot, catLabel);

        Region pillSpacer = new Region();
        HBox.setHgrow(pillSpacer, Priority.ALWAYS);

        LocalDate parsedDate = DateUtil.parseDateSafe(event.getDate());
        boolean isExpired = parsedDate != null && parsedDate.isBefore(LocalDate.now());
        boolean isSoldOut = event.getSlots() <= 0 || isExpired;

        HBox quotaPill = new HBox(4);
        quotaPill.setAlignment(Pos.CENTER);
        quotaPill.setPadding(new Insets(4, 10, 4, 10));
        quotaPill.setMinWidth(Region.USE_PREF_SIZE);
        quotaPill.setMaxHeight(Region.USE_PREF_SIZE);

        Label quotaLbl = new Label();
        quotaLbl.setFont(TiketinTheme.fontBold(11.5));
        if (isSoldOut) {
            quotaPill.setStyle("-fx-background-color: #FEE2E2; -fx-border-color: #FCA5A5; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-width: 1;");
            quotaLbl.setStyle("-fx-text-fill: #B91C1C;");
            quotaLbl.setText(isExpired ? "Berakhir" : "Habis");
        } else if (event.getSlots() <= 10) {
            quotaPill.setStyle("-fx-background-color: #FFF4DC; -fx-border-color: #FDE68A; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-width: 1;");
            quotaLbl.setStyle("-fx-text-fill: #805B16;");
            quotaLbl.setText("Sisa " + event.getSlots() + " tiket");
        } else {
            quotaPill.setStyle("-fx-background-color: rgba(255,255,255,0.95); -fx-border-color: #E3E6E2; -fx-background-radius: 999; -fx-border-radius: 999; -fx-border-width: 1;");
            quotaLbl.setStyle("-fx-text-fill: #20252A;");
            quotaLbl.setText("Sisa " + event.getSlots() + " tiket");
        }
        quotaPill.getChildren().add(quotaLbl);

        topPillsRow.getChildren().addAll(catPill, pillSpacer, quotaPill);
        bannerPane.getChildren().addAll(mediaHolder, topPillsRow);

        // Body & Footer Content
        VBox bodyContent = new VBox(8);
        bodyContent.setMinWidth(0);
        bodyContent.setPadding(new Insets(16, 18, 18, 18));
        VBox.setVgrow(bodyContent, Priority.ALWAYS);

        Label titleLabel = new Label(event.getName() != null ? event.getName() : "Acara");
        titleLabel.setFont(TiketinTheme.fontBold(16.5));
        titleLabel.setStyle("-fx-text-fill: #20252A;");
        titleLabel.setWrapText(true);
        titleLabel.setMinHeight(42);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        String orgName = event.getOwnerName();
        if (orgName == null || orgName.trim().isEmpty()) {
            orgName = event.getOwnerOrg() != null && !event.getOwnerOrg().trim().isEmpty() ? event.getOwnerOrg() : "Penyelenggara";
        }
        Label orgLbl = new Label("oleh " + orgName);
        orgLbl.setFont(TiketinTheme.fontRegular(12.5));
        orgLbl.setStyle("-fx-text-fill: #525A62;");
        orgLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        orgLbl.setMaxWidth(Double.MAX_VALUE);

        VBox metaBox = new VBox(6);
        metaBox.setPadding(new Insets(6, 0, 4, 0));

        HBox dateRow = new HBox(8);
        dateRow.setAlignment(Pos.CENTER_LEFT);
        Label dateLbl = new Label(DateUtil.formatTanggalId(event.getDate()));
        dateLbl.setFont(TiketinTheme.fontBold(12.5));
        dateLbl.setStyle("-fx-text-fill: #20252A;");
        dateRow.getChildren().addAll(
                TiketinIcons.getIcon("calendar", 13, TiketinTheme.TEXT_SECONDARY),
                dateLbl
        );

        HBox locRow = new HBox(8);
        locRow.setAlignment(Pos.CENTER_LEFT);
        locRow.setMinWidth(0);
        Label locLbl = createMetaLabel(event.getLocation() != null && !event.getLocation().isBlank() ? event.getLocation() : "Lokasi Online/TBA");
        HBox.setHgrow(locLbl, Priority.ALWAYS);
        locRow.getChildren().addAll(
                TiketinIcons.getIcon("location", 13, TiketinTheme.TEXT_SECONDARY),
                locLbl
        );

        metaBox.getChildren().addAll(dateRow, locRow);

        Region vSpacer = new Region();
        VBox.setVgrow(vSpacer, Priority.ALWAYS);

        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setMaxHeight(1);
        divider.setStyle("-fx-background-color: #E3E6E2;");
        VBox.setMargin(divider, new Insets(6, 0, 6, 0));

        HBox footer = new HBox(8);
        footer.setMinWidth(0);
        footer.setAlignment(Pos.CENTER_LEFT);

        long price = event.getPrice();
        HBox priceBox = new HBox(4);
        priceBox.setAlignment(Pos.BASELINE_LEFT);
        priceBox.setMinWidth(Region.USE_PREF_SIZE);
        Label priceLbl = new Label(price <= 0 ? "Gratis" : CurrencyUtil.formatRupiah(price));
        priceLbl.setFont(TiketinTheme.fontBold(15));
        priceLbl.setStyle("-fx-text-fill: #20252A;");

        Label priceUnit = new Label(price <= 0 ? "" : "/ tiket");
        priceUnit.setFont(TiketinTheme.fontRegular(12));
        priceUnit.setStyle("-fx-text-fill: #525A62;");
        priceBox.getChildren().addAll(priceLbl, priceUnit);

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        AnimatedButton bookButton = TiketinTheme.createPrimaryButton("Pesan Tiket", "arrow-right");
        bookButton.setMinWidth(Region.USE_PREF_SIZE);
        if (isSoldOut) {
            bookButton.setDisable(true);
            bookButton.setText(isExpired ? "Berakhir" : "Tiket Habis");
        } else {
            bookButton.setOnAction(e -> {
                MainFrame frame = resolveMainFrame();
                if (frame != null) {
                    frame.openCheckout(event, onBooked);
                }
            });
        }

        footer.getChildren().addAll(priceBox, footerSpacer, bookButton);
        bodyContent.getChildren().addAll(titleLabel, orgLbl, metaBox, vSpacer, divider, footer);
        getChildren().addAll(bannerPane, bodyContent);
    }

    private MainFrame resolveMainFrame() {
        if (mainFrame != null) return mainFrame;
        Node cur = getParent();
        while (cur != null) {
            if (cur instanceof MainFrame) {
                return (MainFrame) cur;
            }
            cur = cur.getParent();
        }
        return null;
    }

    public static String resolveBannerBg(String catLower) {
        if (catLower == null) return "#F3F1ED";
        if (catLower.contains("konser") || catLower.contains("musik")) return "#F9DDD5";
        if (catLower.contains("seminar") || catLower.contains("workshop") || catLower.contains("tech")) return "#E9E4F2";
        if (catLower.contains("wisata") || catLower.contains("travel") || catLower.contains("alam")) return "#E9EFDC";
        return "#F3F1ED";
    }

    public static Color resolveCategoryDotColor(String catLower) {
        if (catLower == null) return Color.web("#525A62");
        if (catLower.contains("konser") || catLower.contains("musik")) return Color.web("#E06338");
        if (catLower.contains("seminar") || catLower.contains("workshop") || catLower.contains("tech")) return Color.web("#6D6380");
        if (catLower.contains("wisata") || catLower.contains("travel") || catLower.contains("alam")) return Color.web("#68774B");
        return Color.web("#525A62");
    }

    public static void drawCategoryVectorArt(GraphicsContext gc, double w, double h, String category) {
        gc.clearRect(0, 0, w, h);
        String cat = category != null ? category.toLowerCase() : "umum";
        gc.setFill(Color.web(resolveBannerBg(cat)));
        gc.fillRect(0, 0, w, h);

        double baseH = 172.0;
        double sy = h / baseH;
        gc.save();
        gc.scale(1.0, sy);

        double cx = w / 2.0;

        if (cat.contains("konser") || cat.contains("musik")) {
            // Stage glow + spotlight beams
            gc.setFill(Color.web("#FCEBE6", 0.72));
            gc.fillOval(cx - 108, -32, 216, 216);
            gc.setFill(Color.web("#FFF5F2", 0.85));
            gc.fillOval(cx - 66, 10, 132, 132);

            gc.setFill(Color.web("#E9785D", 0.18));
            gc.fillPolygon(
                    new double[]{Math.max(16, cx - 116), cx - 30, cx + 30, Math.max(44, cx - 64)},
                    new double[]{0, 128, 128, 0},
                    4
            );
            gc.fillPolygon(
                    new double[]{Math.min(w - 44, cx + 64), cx - 30, cx + 30, Math.min(w - 16, cx + 116)},
                    new double[]{0, 128, 128, 0},
                    4
            );

            // Stage halo arc
            gc.setStroke(Color.web("#E9785D", 0.55));
            gc.setLineWidth(2.2);
            gc.strokeArc(cx - 84, 74, 168, 104, 0, 180, ArcType.OPEN);

            // Stage deck full width
            gc.setFill(Color.web("#B94E3A", 0.24));
            gc.fillRect(0, 122, w, 50);

            // Left & right speaker stacks inset cleanly inside width
            double leftSpkX = Math.max(18, cx - 152);
            double rightSpkX = Math.min(w - 46, cx + 124);
            drawSpeaker(gc, leftSpkX, 72);
            drawSpeaker(gc, rightSpkX, 72);

            // Center performer + sound waves
            gc.setFill(Color.web("#8A4130"));
            gc.fillOval(cx - 7.5, 66.5, 15, 15);
            gc.fillRoundRect(cx - 7, 83, 14, 34, 8, 8);

            gc.setStroke(Color.web("#E06338", 0.72));
            gc.setLineWidth(2);
            gc.strokeArc(cx - 32, 66, 26, 36, 130, 100, ArcType.OPEN);
            gc.strokeArc(cx + 6, 66, 26, 36, 310, 100, ArcType.OPEN);

            // Crowd floor + dynamically spaced crowd silhouettes
            gc.setFill(Color.web("#8A4130"));
            gc.fillRect(0, 146, w, baseH - 146);

            int crowdCount = Math.max(7, (int) Math.round(w / 32.0));
            double step = w / crowdCount;
            for (int i = 0; i < crowdCount; i++) {
                double x = (i + 0.5) * step;
                double y = 138 + ((i * 5) % 8);
                Color c = Color.web(i % 2 == 0 ? "#8A4130" : "#9E4B39");
                gc.setFill(c);
                double r = 9.5 + (i % 3) * 1.2;
                gc.fillOval(x - r, y - r, r * 2, r * 2);
                gc.fillOval(x - 15, y + 4, 30, 24);
                if (i % 3 == 1) {
                    gc.setStroke(Color.web("#8A4130"));
                    gc.setLineWidth(3.0);
                    gc.setLineCap(StrokeLineCap.ROUND);
                    gc.strokeLine(x + 7, y + 6, x + 12, y - 14);
                    gc.setFill(Color.web("#FFF4DC"));
                    gc.fillOval(x + 9.5, y - 19, 5, 5);
                }
            }
        } else if (cat.contains("seminar") || cat.contains("workshop") || cat.contains("tech")) {
            gc.setFill(Color.web("#DDD6EA"));
            gc.fillRect(0, 126, w, baseH - 126);

            double screenW = Math.min(188, w - 96);
            double screenX = cx - screenW / 2.0 + 14;
            if (screenX + screenW > w - 18) screenX = w - 18 - screenW;
            if (screenX < 74) screenX = Math.max(54, w - screenW - 18);

            // Presentation screen
            gc.setFill(Color.WHITE);
            gc.fillRoundRect(screenX, 24, screenW, 90, 10, 10);
            gc.setStroke(Color.web("#C7BDDA"));
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(screenX, 24, screenW, 90, 10, 10);

            gc.setFill(Color.web("#54496A"));
            gc.fillRoundRect(screenX + 14, 36, Math.min(82, screenW * 0.45), 8, 4, 4);
            gc.setFill(Color.web("#D1C7E2"));
            gc.fillRoundRect(screenX + 14, 49, Math.min(54, screenW * 0.30), 5, 3, 3);

            // Chart bars inside screen
            String[] barColors = {"#D1C7E2", "#9B8FB5", "#7E7299", "#E9785D"};
            double[] barHeights = {20, 30, 38, 48};
            for (int i = 0; i < 4; i++) {
                double bx = screenX + 18 + i * 21;
                if (bx + 15 < screenX + screenW - 10) {
                    gc.setFill(Color.web(barColors[i]));
                    gc.fillRoundRect(bx, 102 - barHeights[i], 14, barHeights[i], 3, 3);
                }
            }

            if (screenW >= 150) {
                double linesX = screenX + 114;
                double linesW = Math.max(20, screenX + screenW - linesX - 14);
                gc.setFill(Color.web("#C7BDDA"));
                gc.fillRoundRect(linesX, 58, linesW, 6, 3, 3);
                gc.setFill(Color.web("#D8D0E8"));
                gc.fillRoundRect(linesX, 72, linesW * 0.8, 6, 3, 3);
                gc.fillRoundRect(linesX, 86, linesW, 6, 3, 3);
            }

            // Podium & Presenter
            double podX = Math.max(18, screenX - 46);
            gc.setFill(Color.web("#7E7299"));
            gc.fillRoundRect(podX, 88, 24, 38, 4, 4);
            gc.setFill(Color.web("#54496A"));
            gc.fillOval(podX + 4.5, 58.5, 15, 15);
            gc.fillOval(podX + 1, 75, 22, 18);
            gc.setStroke(Color.web("#54496A"));
            gc.setLineWidth(2.4);
            gc.setLineCap(StrokeLineCap.ROUND);
            gc.strokeLine(podX + 20, 78, screenX - 2, 62);

            // Audience seats responsive across w
            int seats = Math.max(5, (int) Math.round(w / 46.0));
            double step = w / (seats + 0.4);
            for (int i = 0; i < seats; i++) {
                double sx = (i + 0.7) * step;
                gc.setFill(Color.web(i % 2 == 0 ? "#54496A" : "#6D6380"));
                gc.fillOval(sx - 9, 128, 18, 18);
                gc.fillOval(sx - 16, 145, 32, 30);
            }
        } else if (cat.contains("wisata") || cat.contains("travel") || cat.contains("alam")) {
            // Sun
            double sunX = w * 0.76;
            gc.setFill(Color.web("#F3E3B6", 0.5));
            gc.fillOval(sunX - 30, 16, 60, 60);
            gc.setFill(Color.web("#FFF4DC"));
            gc.fillOval(sunX - 20, 26, 40, 40);

            // Clouds
            drawCloud(gc, w * 0.24, 45, 1.0, 0.86);
            drawCloud(gc, w * 0.54, 33, 0.85, 0.78);

            // Back mountain ridge proportional 0..w
            gc.setFill(Color.web("#A8B88C"));
            gc.fillPolygon(
                    new double[]{0, w * 0.16, w * 0.34, w * 0.58, w * 0.81, w, w, 0},
                    new double[]{124, 78, 104, 62, 102, 74, baseH, baseH},
                    8
            );

            // Mid hills
            gc.setFill(Color.web("#68774B"));
            gc.fillPolygon(
                    new double[]{0, w * 0.21, w * 0.46, w * 0.72, w, w, 0},
                    new double[]{138, 98, 132, 90, 128, baseH, baseH},
                    7
            );

            // Foreground ridge
            gc.setFill(Color.web("#4F5C37"));
            gc.fillPolygon(
                    new double[]{0, w * 0.27, w * 0.57, w * 0.84, w, w, 0},
                    new double[]{150, 132, 146, 126, 142, baseH, baseH},
                    7
            );

            // Pine silhouettes
            drawPine(gc, w * 0.13, 140, 10, 26, "#3F4C2B");
            drawPine(gc, w * 0.19, 144, 8, 22, "#4F5C37");
            drawPine(gc, w * 0.88, 136, 12, 30, "#3F4C2B");

            // Coral waypoint pin on scenic ridge
            double px = w * 0.55;
            double py = 92;
            gc.setFill(Color.web("#E06338"));
            gc.fillPolygon(new double[]{px - 6, px + 6, px}, new double[]{py + 4, py + 4, py + 15}, 3);
            gc.fillOval(px - 8, py - 8, 16, 16);
            gc.setFill(Color.WHITE);
            gc.fillOval(px - 3, py - 3, 6, 6);
        } else {
            // Warm Editorial Default
            gc.setFill(Color.web("#E3E6E2", 0.55));
            gc.fillOval(w - 110, -40, 150, 150);
            gc.setFill(Color.web("#E06338", 0.09));
            gc.fillOval(12, 92, 120, 120);

            gc.setFill(Color.web("#FFFFFF", 0.88));
            gc.fillOval(cx - 24, 52, 48, 48);
            gc.setStroke(Color.web("#E3E6E2"));
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - 24, 52, 48, 48);

            // Minimal compass diamond inside circle
            gc.setFill(Color.web("#E06338"));
            gc.fillPolygon(
                    new double[]{cx, cx + 6, cx, cx - 6},
                    new double[]{64, 76, 88, 76},
                    4
            );
            gc.setFill(Color.web("#525A62"));
            gc.setFont(TiketinTheme.fontRegular(11.5));
            gc.fillText("Acara Pilihan", cx - 34, 120);
        }
        gc.restore();
    }

    private static void drawSpeaker(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.web("#B94E3A", 0.85));
        gc.fillRoundRect(x, y, 26, 54, 6, 6);
        gc.setFill(Color.web("#8A4130"));
        gc.fillOval(x + 6.5, y + 9, 13, 13);
        gc.fillOval(x + 4.5, y + 29, 17, 17);
    }

    private static void drawCloud(GraphicsContext gc, double x, double y, double s, double alpha) {
        gc.setFill(Color.web("#FFFFFF", alpha));
        gc.fillOval(x - 16 * s, y - 10 * s, 22 * s, 22 * s);
        gc.fillOval(x - 4 * s, y - 15 * s, 28 * s, 28 * s);
        gc.fillOval(x + 12 * s, y - 9 * s, 20 * s, 20 * s);
    }

    private static void drawPine(GraphicsContext gc, double cx, double baseY, double halfW, double height, String hex) {
        gc.setFill(Color.web(hex));
        gc.fillPolygon(
                new double[]{cx, cx - halfW, cx + halfW},
                new double[]{baseY - height, baseY, baseY},
                3
        );
    }

    private Label createMetaLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(TiketinTheme.fontRegular(12.5));
        lbl.setStyle("-fx-text-fill: #525A62;");
        lbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private static void updateTopRoundedClipLocal(Path path, double w, double h, double r) {
        path.getElements().clear();
        double cr = Math.min(r, Math.min(w / 2.0, h / 2.0));
        path.getElements().addAll(
                new MoveTo(0, h),
                new LineTo(0, cr),
                new ArcTo(cr, cr, 0, cr, 0, false, true),
                new LineTo(w - cr, 0),
                new ArcTo(cr, cr, 0, w, cr, false, true),
                new LineTo(w, h),
                new ClosePath()
        );
    }
}
