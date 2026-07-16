package org.cotato.gongmozip.domains.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.CertificationCategoriesResponse;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.CertificationSearchResponse;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileSuccessCode;
import org.cotato.gongmozip.domains.profile.service.ProfileService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Certification", description = "자격증 마스터 정보 조회 API")
@RestController
@RequestMapping("/api/certifications")
@RequiredArgsConstructor
public class CertificationController {

    private final ProfileService profileService;

    @Operation(summary = "자격증 카테고리 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/categories")
    public ResponseEntity<BaseResponse<CertificationCategoriesResponse>> getCertificationCategories() {
        CertificationCategoriesResponse response = profileService.getCertificationCategories();
        return BaseResponseFormatter.success(ProfileSuccessCode.CERTIFICATION_CATEGORIES_RETRIEVED, response);
    }

    @Operation(summary = "자격증 검색")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping
    public ResponseEntity<BaseResponse<CertificationSearchResponse>> searchCertifications(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CertificationCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        CertificationSearchResponse response = profileService.searchCertifications(keyword, category, page, size);
        return BaseResponseFormatter.success(ProfileSuccessCode.CERTIFICATIONS_SEARCHED, response);
    }
}
