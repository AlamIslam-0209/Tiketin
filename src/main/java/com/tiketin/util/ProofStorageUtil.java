package com.tiketin.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Shared helper for storing and locating uploaded payment proof images.
 */
public final class ProofStorageUtil {

    private ProofStorageUtil() {}

    public static String saveProof(File source) throws IOException {
        return saveImage(source, "proof", "proofs");
    }

    public static File findProofFile(String filename) {
        return findImageFile(filename, "proofs");
    }

    public static String saveAvatar(File source) throws IOException {
        return saveImage(source, "avatar", "avatars");
    }

    public static File findAvatarFile(String filename) {
        return findImageFile(filename, "avatars");
    }

    public static String saveEventImage(File source) throws IOException {
        return saveImage(source, "event", "events");
    }

    public static File findEventImageFile(String filename) {
        return findImageFile(filename, "events");
    }

    private static String saveImage(File source, String prefix, String subFolder) throws IOException {
        String imgErr = ValidationUtil.validateImageFile(source);
        if (imgErr != null) {
            throw new IOException(imgErr);
        }
        String ext = ValidationUtil.extractExtension(source.getName());
        if (ext.isEmpty()) {
            ext = "jpg";
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String rand = UUID.randomUUID().toString().substring(0, 6);
        String savedFilename = prefix + "_" + timestamp + "_" + rand + "." + ext;

        File parentDir = new File("../uploads/" + subFolder);
        if (parentDir.exists() || parentDir.mkdirs()) {
            Files.copy(source.toPath(), new File(parentDir, savedFilename).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        File localDir = new File("uploads/" + subFolder);
        if (localDir.exists() || localDir.mkdirs()) {
            Files.copy(source.toPath(), new File(localDir, savedFilename).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return savedFilename;
    }

    private static File findImageFile(String filename, String subFolder) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String clean = filename.trim();
        File f0 = new File(clean);
        if (f0.isAbsolute() && f0.exists()) {
            return f0;
        }
        File f1 = new File("../uploads/" + subFolder, clean);
        if (f1.exists()) {
            return f1;
        }
        File f2 = new File("uploads/" + subFolder, clean);
        if (f2.exists()) {
            return f2;
        }
        return null;
    }
}
