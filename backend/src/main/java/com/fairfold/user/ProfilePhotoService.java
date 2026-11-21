package com.fairfold.user;

import com.fairfold.common.errors.ApiException;
import com.fairfold.common.storage.ImageTypeDetector;
import com.fairfold.common.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/** Uploads and serves the optional profile photo stored on {@link User#getAvatarUrl()}. */
@Service
public class ProfilePhotoService {

    private static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final String PHOTO_PATH = "/api/users/photo/";

    private final UserRepository userRepository;
    private final StorageService storageService;

    public ProfilePhotoService(UserRepository userRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @Transactional
    public User upload(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("INVALID_FILE", "Please choose an image file to upload.");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Profile photo must be at most 2 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_FILE", "Could not read the uploaded image.");
        }
        if (ImageTypeDetector.detect(bytes) == null) {
            throw ApiException.badRequest("INVALID_IMAGE", "Profile photo must be a valid PNG, JPEG, GIF, WebP, or SVG image.");
        }

        User user = load(userId);
        cleanupPreviousPhoto(user);

        String storageKey = storageService.save(bytes, file.getOriginalFilename());
        user.setAvatarUrl(PHOTO_PATH + storageKey);
        return userRepository.save(user);
    }

    @Transactional
    public User delete(UUID userId) {
        User user = load(userId);
        cleanupPreviousPhoto(user);
        user.setAvatarUrl(null);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public byte[] loadPhoto(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ApiException.notFound("PHOTO_NOT_FOUND", "Profile photo not found.");
        }
        try {
            return storageService.load(storageKey);
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND) {
                throw ApiException.notFound("PHOTO_NOT_FOUND", "Profile photo not found.");
            }
            throw ex;
        }
    }

    private void cleanupPreviousPhoto(User user) {
        String current = user.getAvatarUrl();
        if (current != null && current.startsWith(PHOTO_PATH)) {
            storageService.delete(current.substring(PHOTO_PATH.length()));
        }
    }

    private User load(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHORIZED", "Authentication required."));
    }
}
