package com.fairfold.user;

import com.fairfold.common.storage.ImageTypeDetector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Public user resources")
public class PublicUserController {

    private final ProfilePhotoService profilePhotoService;

    public PublicUserController(ProfilePhotoService profilePhotoService) {
        this.profilePhotoService = profilePhotoService;
    }

    @GetMapping("/photo/{storageKey}")
    @Operation(summary = "Get a profile photo image by storage key")
    public ResponseEntity<byte[]> getPhoto(@PathVariable("storageKey") String storageKey) {
        byte[] bytes = profilePhotoService.loadPhoto(storageKey);
        MediaType mediaType = ImageTypeDetector.detect(bytes);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
