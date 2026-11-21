package com.fairfold.common.storage;

import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

/** Detects image media types from magic bytes (plus a small SVG sniff). */
public final class ImageTypeDetector {

    private ImageTypeDetector() {
    }

    public static MediaType detect(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return null;
        }
        // PNG: 89 50 4E 47
        if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        // JPEG: FF D8 FF
        if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG;
        }
        // GIF: GIF8
        if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == '8') {
            return MediaType.IMAGE_GIF;
        }
        // WEBP: RIFF....WEBP
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return MediaType.parseMediaType("image/webp");
        }
        // SVG: XML or <svg
        String snippet = new String(bytes, 0, Math.min(bytes.length, 512), StandardCharsets.UTF_8).toLowerCase();
        if (snippet.contains("<svg")) {
            return MediaType.parseMediaType("image/svg+xml");
        }
        return null;
    }
}
