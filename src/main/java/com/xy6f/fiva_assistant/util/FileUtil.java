package com.xy6f.fiva_assistant.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

public class FileUtil {

    private static final String UPLOAD_DIR = "uploads";

    public static String saveFile(byte[] bytes, String originalFilename) throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String ext = getExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + ext;
        Path filePath = uploadPath.resolve(newFilename);
        Files.write(filePath, bytes);

        return filePath.toString();
    }

    public static String encodeToBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String getMimeType(String filename) {
        String ext = getExtension(filename).toLowerCase();
        return switch (ext) {
            case ".png" -> "image/png";
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            default -> "image/png";
        };
    }

    private static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : ".png";
    }
}
