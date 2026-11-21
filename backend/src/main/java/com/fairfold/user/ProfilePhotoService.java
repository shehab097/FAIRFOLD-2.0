package com.fairfold.user;

import com.fairfold.common.errors.ApiException;
import com.fairfold.common.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/** Uploads and serves the optional profile photo stored on {@link User#getAvatarUrl()}. */
@Service
public class ProfilePhotoService {

    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int OUTPUT_SIZE = 480;
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
            throw ApiException.badRequest("FILE_TOO_LARGE", "Profile photo must be at most 5 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_FILE", "Could not read the uploaded image.");
        }

        byte[] webp = toSquareWebp(bytes);
        User user = load(userId);
        cleanupPreviousPhoto(user);

        String storageKey = storageService.save(webp, "profile-photo.webp");
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

    /** Center-crops to a square and scales to 480×480, encoded as WebP. */
    private byte[] toSquareWebp(byte[] bytes) {
        BufferedImage source;
        try {
            source = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException | RuntimeException e) {
            throw ApiException.badRequest("INVALID_IMAGE", "Profile photo must be a valid PNG, JPEG, GIF, or WebP image.");
        }
        if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
            throw ApiException.badRequest("INVALID_IMAGE", "Profile photo must be a valid PNG, JPEG, GIF, or WebP image.");
        }

        int side = Math.min(source.getWidth(), source.getHeight());
        int sx = (source.getWidth() - side) / 2;
        int sy = (source.getHeight() - side) / 2;

        BufferedImage square = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = square.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, sx, sy, sx + side, sy + side, null);
        } finally {
            graphics.dispose();
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!ImageIO.write(square, "webp", out)) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_CONVERSION_FAILED",
                        "Could not convert the profile photo to WebP.");
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_CONVERSION_FAILED",
                    "Could not convert the profile photo to WebP.");
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
