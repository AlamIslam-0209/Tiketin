package com.tiketin.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Shared helper for storing and locating uploaded payment proof images inside the project uploads directory.
 */
public final class ProofStorageUtil {

    private ProofStorageUtil() {}

    private static File resolveProofDirectory() {
        if (new File("pom.xml").exists() || new File("uploads").exists()) {
            return new File("uploads/proofs");
        }
        if (new File("../pom.xml").exists() || new File("../uploads").exists()) {
            return new File("../uploads/proofs");
        }
        return new File("uploads/proofs");
    }

    public static String saveProof(File source) throws IOException {
        String originalName = source.getName();
        String ext = "jpg";
        int dot = originalName.lastIndexOf('.');
        if (dot > 0 && dot < originalName.length() - 1) {
            ext = originalName.substring(dot + 1).toLowerCase();
        }
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String rand = UUID.randomUUID().toString().substring(0, 6);
        String savedFilename = "proof_" + timestamp + "_" + rand + "." + ext;

        File proofDir = resolveProofDirectory();
        if (proofDir.exists() || proofDir.mkdirs()) {
            Files.copy(source.toPath(), new File(proofDir, savedFilename).toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        return savedFilename;
    }

    public static File findProofFile(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String clean = filename.trim();
        File primary = new File(resolveProofDirectory(), clean);
        if (primary.exists()) {
            return primary;
        }
        File fallback = new File("uploads/proofs", clean);
        if (fallback.exists()) {
            return fallback;
        }
        return null;
    }
}
