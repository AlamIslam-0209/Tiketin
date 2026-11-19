package com.tiketin.ui.components;

import com.tiketin.util.ProofStorageUtil;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure-JavaFX helpers for 4:3 non-cropped ("fit" / preserveRatio) event gallery rendering,
 * smooth clipping, and bounded LRU image caching.
 */
public final class EventImageSupport {

    public static final String MATTE_BG_HEX = "#EFECE6";
    public static final double FOUR_THREE_RATIO = 0.75; // height / width = 3 / 4

    private static final int MAX_CACHE_SIZE = 72;
    private static final Map<String, Image> IMAGE_CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            }
    );

    private EventImageSupport() {}

    public static Image loadStoredEventImage(String storedFilename, double maxW, double maxH) {
        if (storedFilename == null || storedFilename.isBlank()) {
            return null;
        }
        File file = ProofStorageUtil.findEventImageFile(storedFilename);
        return loadLocalImageFile(file, maxW, maxH);
    }

    public static Image loadLocalImageFile(File file, double maxW, double maxH) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        String cacheKey = file.getAbsolutePath() + ":" + file.lastModified() + ":" + (int) maxW + "x" + (int) maxH;
        Image cached = IMAGE_CACHE.get(cacheKey);
        if (cached != null && !cached.isError() && cached.getWidth() > 0) {
            return cached;
        }
        try {
            Image img = new Image(file.toURI().toString(), maxW, maxH, true, true, false);
            if (!img.isError() && img.getWidth() > 0 && img.getHeight() > 0) {
                IMAGE_CACHE.put(cacheKey, img);
                return img;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Builds an ImageView that ALWAYS fits completely inside the frame (preserveRatio = true, zero crop).
     */
    public static ImageView createFittedImageView(StackPane container, double pad) {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.fitWidthProperty().bind(Bindings.max(1.0, container.widthProperty().subtract(pad)));
        view.fitHeightProperty().bind(Bindings.max(1.0, container.heightProperty().subtract(pad)));
        return view;
    }

    /**
     * Dynamically maintains an exact 4:3 (height = width * 0.75) box as container width changes,
     * updating only when delta >= 2px to avoid layout loops.
     */
    public static void bindFourThreeHeight(Region container, double initialHeight, double minH, double maxH) {
        container.setMinHeight(initialHeight);
        container.setPrefHeight(initialHeight);
        container.setMaxHeight(initialHeight);
        container.widthProperty().addListener((obs, oldW, newW) -> {
            double w = newW != null ? newW.doubleValue() : 0;
            if (w <= 20) return;
            double target = Math.round(Math.max(minH, Math.min(maxH, w * FOUR_THREE_RATIO)));
            if (Math.abs(container.getPrefHeight() - target) >= 2.0) {
                Platform.runLater(() -> {
                    container.setMinHeight(target);
                    container.setPrefHeight(target);
                    container.setMaxHeight(target);
                });
            }
        });
    }

    public static void applyRoundedClip(Region region, double radius) {
        Rectangle clip = new Rectangle();
        clip.setArcWidth(radius * 2.0);
        clip.setArcHeight(radius * 2.0);
        clip.widthProperty().bind(region.widthProperty());
        clip.heightProperty().bind(region.heightProperty());
        region.setClip(clip);
    }

    public static void updateTopRoundedClip(Path path, double w, double h, double r) {
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
