package com.tiketin.ui.theme;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

public class TiketinIcons {

    public static Node getIcon(String name, double size, Color color) {
        SVGPath svg = new SVGPath();
        svg.setContent(getSvgContent(name));
        svg.setFill(color != null ? color : Color.web("#18181B"));
        svg.setScaleX(size / 24.0);
        svg.setScaleY(size / 24.0);

        StackPane pane = new StackPane(svg);
        pane.setMinSize(size, size);
        pane.setPrefSize(size, size);
        pane.setMaxSize(size, size);
        pane.setAlignment(Pos.CENTER);
        pane.setPickOnBounds(false);
        pane.setMouseTransparent(true);
        return pane;
    }

    public static Node getIcon(String name, double size, String hexColor) {
        return getIcon(name, size, hexColor != null ? Color.web(hexColor) : Color.web("#18181B"));
    }

    private static String getSvgContent(String name) {
        if (name == null) return "M0 0h24v24H0z";
        switch (name.toLowerCase()) {
            case "home":
                return "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z";
            case "compass":
                return "M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8zm-1-13h2v6h-6v-2h4z";
            case "ticket":
                return "M20 6h-2.18a3 3 0 1 0-5.64 0H5a2 2 0 0 0-2 2v2a2 2 0 0 1 2 2 2 2 0 0 1-2 2v2a2 2 0 0 0 2 2h7.18a3 3 0 1 0 5.64 0H20a2 2 0 0 0 2-2v-2a2 2 0 0 1-2-2 2 2 0 0 1 2-2V8a2 2 0 0 0-2-2zm-3 9a1 1 0 1 1-1-1 1 1 0 0 1 1 1zm0-6a1 1 0 1 1-1-1 1 1 0 0 1 1 1z";
            case "apply":
            case "sparkles":
                return "M12 2l2.4 4.8L19.2 9l-4.8 2.4L12 16.2 9.6 11.4 4.8 9l4.8-2.2L12 2zm7 11l1.2 2.4L22.6 17l-2.4 1.2L19 20.6l-1.2-2.4L15.4 17l2.4-1.2L19 13zM6 15l.9 1.8L8.7 17.5l-1.8.9L6 20.2l-.9-1.8L3.3 17.5l1.8-.7L6 15z";
            case "organizer":
                return "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z";
            case "sliders":
                return "M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z";
            case "admin":
            case "shield":
                return "M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z";
            case "search":
                return "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
            case "plus":
                return "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
            case "arrow-right":
                return "M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z";
            case "arrow-left":
                return "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z";
            case "arrow-up-right":
                return "M14 3v2h3.59l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z";
            case "check":
                return "M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z";
            case "close":
                return "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";
            case "copy":
                return "M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z";
            case "refresh":
                return "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
            case "trash":
                return "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
            case "edit":
                return "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
            case "clock":
                return "M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z";
            case "eye":
                return "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z";
            case "location":
            case "map":
                return "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 0 1 0-5 2.5 2.5 0 0 1 0 5z";
            case "calendar":
                return "M19 3h-1V1h-2v2H8V1H6v2H5c-1.11 0-1.99.9-1.99 2L3 19a2 2 0 0 0 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V8h14v11zM7 10h5v5H7z";
            case "upload":
                return "M19 15v4H5v-4H3v4c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2v-4h-2zM13 5.83l2.88 2.88L17.3 7.3 12 2 6.7 7.3l1.41 1.41L11 5.83V16h2V5.83z";
            case "user":
                return "M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z";
            case "settings":
                return "M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36.25c-.59.24-1.13.56-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z";
            case "logout":
                return "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z";
            case "alert":
                return "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
            case "music":
                return "M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z";
            case "mic":
                return "M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm-1-9c0-.55.45-1 1-1s1 .45 1 1v6c0 .55-.45 1-1 1s-1-.45-1-1V5zm6 6c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z";
            case "grid":
                return "M3 3h8v8H3V3zm10 0h8v5h-8V3zm0 8h8v10h-8V11zM3 14h8v7H3v-7z";
            case "filter":
                return "M3 4h18l-7 8v6l-4 2v-8L3 4z";
            case "chevron-down":
                return "M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z";
            case "chevron-up":
                return "M7.41 15.41L12 10.83l4.59 4.58L18 14l-6-6-6 6z";
            case "chevron-left":
                return "M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z";
            case "chevron-right":
                return "M8.59 16.59L10 18l6-6-6-6-1.41 1.41L13.17 12z";
            case "image":
                return "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z";
            case "star":
                return "M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z";
            case "qrcode":
                return "M3 3h8v8H3V3zm2 2v4h4V5H5zm8-2h8v8h-8V3zm2 2v4h4V5h-4zM3 13h8v8H3v-8zm2 2v4h4v-4H5zm10-2h2v2h-2v-2zm4 0h2v2h-2v-2zm-4 4h2v2h-2v-2zm2-2h2v2h-2v-2zm2 2h2v4h-4v-2h2v-2zm-6 2h2v2h-2v-2z";
            case "users":
                return "M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z";
            default:
                return "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
        }
    }
}
