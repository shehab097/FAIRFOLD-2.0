package com.fairfold.common.storage;

import com.fairfold.common.errors.ApiException;
import com.fairfold.config.AppProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Stores resume files on local disk under {@code app.storage-dir}, named by a
 * random UUID (no user-controlled path or extension is kept).
 */
@Service
public class LocalDiskStorageService implements StorageService {

    private final Path root;

    public LocalDiskStorageService(AppProperties properties) {
        String dir = properties.storageDir();
        if (dir == null || dir.isBlank()) {
            dir = "data/resumes";
        }
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    @Override
    public String save(byte[] content, String originalFilename) {
        try {
            Files.createDirectories(root);
            String key = UUID.randomUUID().toString();
            Files.write(root.resolve(key), content);
            return key;
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "STORAGE_ERROR", "Could not store the file.");
        }
    }

    @Override
    public byte[] load(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ApiException.notFound("RESUME_NOT_FOUND", "Resume file not found.");
        }
        try {
            Path target = root.resolve(storageKey).normalize();
            if (!target.startsWith(root) || !Files.exists(target)) {
                throw ApiException.notFound("RESUME_NOT_FOUND", "Resume file not found.");
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "STORAGE_ERROR", "Could not read the stored file.");
        }
    }

    @Override
    public void delete(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(storageKey));
        } catch (IOException ignored) {
            // Best effort; a failed delete must not break the business flow.
        }
    }
}
