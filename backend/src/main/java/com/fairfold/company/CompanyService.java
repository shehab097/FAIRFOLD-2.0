package com.fairfold.company;

import com.fairfold.common.errors.ApiException;
import com.fairfold.common.storage.ImageTypeDetector;
import com.fairfold.common.storage.StorageService;
import com.fairfold.company.dto.CompanyProfileRequest;
import com.fairfold.company.dto.CompanyProfileResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
public class CompanyService {

    private static final Set<String> SIZES = Set.of("1-10", "11-50", "51-200", "201-1000", "1000+");
    private static final long MAX_LOGO_BYTES = 2L * 1024 * 1024; // 2 MB

    private final CompanyRepository companyRepository;
    private final StorageService storageService;

    public CompanyService(CompanyRepository companyRepository, StorageService storageService) {
        this.companyRepository = companyRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse get(UUID companyId) {
        return toResponse(load(companyId));
    }

    @Transactional
    public CompanyProfileResponse update(UUID companyId, CompanyProfileRequest request) {
        if (!SIZES.contains(request.size())) {
            throw ApiException.badRequest("INVALID_SIZE", "Company size must be one of 1-10, 11-50, 51-200, 201-1000, 1000+.");
        }
        Company company = load(companyId);
        company.setName(request.name().trim());
        company.setIndustry(request.industry().trim());
        company.setSize(request.size());
        company.setShowNamePublicly(request.showNamePublicly());
        company.setTagline(clean(request.tagline()));
        company.setAbout(clean(request.about()));
        company.setWebsite(clean(request.website()));
        company.setLogoUrl(clean(request.logoUrl()));
        company.setLinkedinUrl(clean(request.linkedinUrl()));
        company.setFoundedYear(request.foundedYear());
        company.setHeadquarters(clean(request.headquarters()));
        company.setContactEmail(clean(request.contactEmail()));
        company.setPhone(clean(request.phone()));
        company.setBenefits(clean(request.benefits()));
        return toResponse(company);
    }

    public boolean isComplete(Company company) {
        return notBlank(company.getName()) && notBlank(company.getIndustry()) && notBlank(company.getSize());
    }

    @Transactional
    public CompanyProfileResponse uploadLogo(UUID companyId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("INVALID_FILE", "Please choose an image file to upload.");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw ApiException.badRequest("FILE_TOO_LARGE", "Logo image must be at most 2 MB.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_FILE", "Could not read the uploaded image.");
        }
        MediaType mediaType = detectImageMediaType(bytes);
        if (mediaType == null) {
            throw ApiException.badRequest("INVALID_IMAGE", "Logo must be a valid PNG, JPEG, GIF, WebP, or SVG image.");
        }

        Company company = load(companyId);
        cleanupPreviousUploadedLogo(company);

        String storageKey = storageService.save(bytes, file.getOriginalFilename());
        company.setLogoUrl("/api/companies/logo/" + storageKey);
        companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional
    public CompanyProfileResponse deleteLogo(UUID companyId) {
        Company company = load(companyId);
        cleanupPreviousUploadedLogo(company);
        company.setLogoUrl(null);
        companyRepository.save(company);
        return toResponse(company);
    }

    @Transactional(readOnly = true)
    public byte[] loadLogo(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw ApiException.notFound("LOGO_NOT_FOUND", "Company logo not found.");
        }
        return storageService.load(storageKey);
    }

    private void cleanupPreviousUploadedLogo(Company company) {
        String current = company.getLogoUrl();
        if (current != null && current.startsWith("/api/companies/logo/")) {
            String oldKey = current.substring("/api/companies/logo/".length());
            storageService.delete(oldKey);
        }
    }

    public MediaType detectImageMediaType(byte[] bytes) {
        return ImageTypeDetector.detect(bytes);
    }

    private Company load(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> ApiException.notFound("COMPANY_NOT_FOUND", "Company not found."));
    }

    private CompanyProfileResponse toResponse(Company company) {
        return new CompanyProfileResponse(company.getName(), company.getIndustry(), company.getSize(), company.isShowNamePublicly(),
                company.getTagline(), company.getAbout(), company.getWebsite(), company.getLogoUrl(), company.getLinkedinUrl(),
                company.getFoundedYear(), company.getHeadquarters(), company.getContactEmail(), company.getPhone(), company.getBenefits());
    }

    private String clean(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
