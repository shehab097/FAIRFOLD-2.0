package com.fairfold.company;

import com.fairfold.common.errors.ApiException;
import com.fairfold.company.dto.CompanyProfileRequest;
import com.fairfold.company.dto.CompanyProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class CompanyService {

    private static final Set<String> SIZES = Set.of("1-10", "11-50", "51-200", "201-1000", "1000+");

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
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
        return toResponse(company);
    }

    public boolean isComplete(Company company) {
        return notBlank(company.getName()) && notBlank(company.getIndustry()) && notBlank(company.getSize());
    }

    private Company load(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> ApiException.notFound("COMPANY_NOT_FOUND", "Company not found."));
    }

    private CompanyProfileResponse toResponse(Company company) {
        return new CompanyProfileResponse(company.getName(), company.getIndustry(), company.getSize(), company.isShowNamePublicly());
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
