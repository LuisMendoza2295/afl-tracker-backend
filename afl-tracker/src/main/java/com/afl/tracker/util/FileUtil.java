package com.afl.tracker.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileUtil {

    public Path extractResourceToTemp(String resourceName) throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) throw new IOException("Resource not found: " + resourceName);
            Path tempFile = Files.createTempFile("logo_", ".png");
            Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempFile;
        }
    }
}
