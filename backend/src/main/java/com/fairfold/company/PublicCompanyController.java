package com.fairfold.company;

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
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Public company resources and logos")
public class PublicCompanyController {

    private final CompanyService companyService;

    public PublicCompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping("/logo/{storageKey}")
    @Operation(summary = "Get a company logo image by storage key")
    public ResponseEntity<byte[]> getLogo(@PathVariable("storageKey") String storageKey) {
        byte[] bytes = companyService.loadLogo(storageKey);
        MediaType mediaType = companyService.detectImageMediaType(bytes);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}

