package org.cotato.gongmozip.domains.profile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.dto.request.ProfileRequest.*;
import org.cotato.gongmozip.domains.profile.dto.response.ProfileResponse.*;
import org.cotato.gongmozip.domains.profile.enums.CertificationCategory;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileSuccessCode;
import org.cotato.gongmozip.domains.profile.service.ProfileService;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.security.jwt.CustomUserDetails;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Profile", description = "프로필 및 연관 경험 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final MemberRepository memberRepository;

    private Member getAuthenticatedMember(CustomUserDetails userDetails) {
        return memberRepository
                .findById(userDetails.getMemberId())
                .orElseThrow(() -> new org.cotato.gongmozip.domains.member.exception.MemberException(
                        MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // 프로필 API

    @Operation(summary = "프로필 생성")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PostMapping("/profiles")
    public ResponseEntity<BaseResponse<CreateProfileResponse>> createProfile(
            @RequestBody @Valid CreateProfileRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        CreateProfileResponse response = profileService.createProfile(request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROFILE_CREATED, response);
    }

    @Operation(summary = "내 프로필 목록 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles")
    public ResponseEntity<BaseResponse<ProfileListResponse>> getMyProfiles(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProfileListResponse response = profileService.getMyProfiles(member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROFILE_LIST_RETRIEVED, response);
    }

    @Operation(summary = "프로필 상세 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<BaseResponse<ProfileDetailResponse>> getProfileDetail(
            @PathVariable("profileId") Long profileId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProfileDetailResponse response = profileService.getProfileDetail(profileId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROFILE_RETRIEVED, response);
    }

    @Operation(summary = "프로필 수정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PatchMapping("/profiles/{profileId}")
    public ResponseEntity<BaseResponse<UpdateProfileResponse>> updateProfile(
            @PathVariable("profileId") Long profileId,
            @RequestBody @Valid UpdateProfileRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        UpdateProfileResponse response = profileService.updateProfile(profileId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROFILE_UPDATED, response);
    }

    @Operation(summary = "프로필 삭제")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @DeleteMapping("/profiles/{profileId}")
    public ResponseEntity<BaseResponse<Void>> deleteProfile(
            @PathVariable("profileId") Long profileId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        profileService.deleteProfile(profileId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROFILE_DELETED);
    }

    @Operation(summary = "대표 프로필 설정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PatchMapping("/profiles/{profileId}/main")
    public ResponseEntity<BaseResponse<UpdateMainProfileResponse>> setMainProfile(
            @PathVariable("profileId") Long profileId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        UpdateMainProfileResponse response = profileService.setMainProfile(profileId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.MAIN_PROFILE_SET, response);
    }

    @Operation(summary = "프로필 공개 여부 변경")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PatchMapping("/profiles/{profileId}/visibility")
    public ResponseEntity<BaseResponse<UpdateVisibilityResponse>> updateVisibility(
            @PathVariable("profileId") Long profileId,
            @RequestBody @Valid UpdateVisibilityRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        UpdateVisibilityResponse response = profileService.updateVisibility(profileId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROFILE_VISIBILITY_CHANGED, response);
    }

    @Operation(summary = "프로필 미리보기 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}/preview")
    public ResponseEntity<BaseResponse<ProfilePreviewResponse>> getProfilePreview(
            @PathVariable("profileId") Long profileId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProfilePreviewResponse response = profileService.getProfilePreview(profileId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROFILE_PREVIEW_RETRIEVED, response);
    }

    @Operation(summary = "공개 프로필 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/public/profiles/{profileId}")
    public ResponseEntity<BaseResponse<PublicProfileResponse>> getPublicProfile(
            @PathVariable("profileId") Long profileId) {
        PublicProfileResponse response = profileService.getPublicProfile(profileId);
        return BaseResponseFormatter.success(ProfileSuccessCode.PUBLIC_PROFILE_RETRIEVED, response);
    }

    // 프로젝트 경험 API

    @Operation(summary = "프로젝트 경험 등록")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PostMapping("/profiles/{profileId}/projects")
    public ResponseEntity<BaseResponse<ProjectResponse>> createProject(
            @PathVariable("profileId") Long profileId,
            @RequestBody @Valid CreateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProjectResponse response = profileService.createProject(profileId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROJECT_CREATED, response);
    }

    @Operation(summary = "프로젝트 경험 목록 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}/projects")
    public ResponseEntity<BaseResponse<ProjectListResponse>> getProjects(
            @PathVariable("profileId") Long profileId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProjectListResponse response = profileService.getProjects(profileId, page, size, sort, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROJECT_LIST_RETRIEVED, response);
    }

    @Operation(summary = "프로젝트 경험 상세 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}/projects/{projectId}")
    public ResponseEntity<BaseResponse<ProjectDetailResponse>> getProjectDetail(
            @PathVariable("profileId") Long profileId,
            @PathVariable("projectId") Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProjectDetailResponse response = profileService.getProjectDetail(profileId, projectId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROJECT_RETRIEVED, response);
    }

    @Operation(summary = "프로젝트 경험 수정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PatchMapping("/profiles/{profileId}/projects/{projectId}")
    public ResponseEntity<BaseResponse<ProjectResponse>> updateProject(
            @PathVariable("profileId") Long profileId,
            @PathVariable("projectId") Long projectId,
            @RequestBody @Valid UpdateProjectRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProjectResponse response = profileService.updateProject(profileId, projectId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROJECT_UPDATED, response);
    }

    @Operation(summary = "프로젝트 경험 삭제")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @DeleteMapping("/profiles/{profileId}/projects/{projectId}")
    public ResponseEntity<BaseResponse<Void>> deleteProject(
            @PathVariable("profileId") Long profileId,
            @PathVariable("projectId") Long projectId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        profileService.deleteProject(profileId, projectId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.PROJECT_DELETED);
    }

    // 수상 경험 API

    @Operation(summary = "수상 경험 등록")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PostMapping("/profiles/{profileId}/awards")
    public ResponseEntity<BaseResponse<AwardResponse>> createAward(
            @PathVariable("profileId") Long profileId,
            @RequestBody @Valid CreateAwardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        AwardResponse response = profileService.createAward(profileId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.AWARD_CREATED, response);
    }

    @Operation(summary = "수상 경험 목록 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}/awards")
    public ResponseEntity<BaseResponse<AwardListResponse>> getAwards(
            @PathVariable("profileId") Long profileId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        AwardListResponse response = profileService.getAwards(profileId, page, size, sort, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.AWARD_LIST_RETRIEVED, response);
    }

    @Operation(summary = "수상 경험 상세 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}/awards/{awardId}")
    public ResponseEntity<BaseResponse<AwardResponse>> getAwardDetail(
            @PathVariable("profileId") Long profileId,
            @PathVariable("awardId") Long awardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        AwardResponse response = profileService.getAwardDetail(profileId, awardId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.AWARD_RETRIEVED, response);
    }

    @Operation(summary = "수상 경험 수정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PatchMapping("/profiles/{profileId}/awards/{awardId}")
    public ResponseEntity<BaseResponse<AwardResponse>> updateAward(
            @PathVariable("profileId") Long profileId,
            @PathVariable("awardId") Long awardId,
            @RequestBody @Valid UpdateAwardRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        AwardResponse response = profileService.updateAward(profileId, awardId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.AWARD_UPDATED, response);
    }

    @Operation(summary = "수상 경험 삭제")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @DeleteMapping("/profiles/{profileId}/awards/{awardId}")
    public ResponseEntity<BaseResponse<Void>> deleteAward(
            @PathVariable("profileId") Long profileId,
            @PathVariable("awardId") Long awardId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        profileService.deleteAward(profileId, awardId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.AWARD_DELETED);
    }

    // 자격증 API

    @Operation(summary = "자격증 등록")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PostMapping("/profiles/{profileId}/certifications")
    public ResponseEntity<BaseResponse<ProfileCertificationResponse>> createProfileCertification(
            @PathVariable("profileId") Long profileId,
            @RequestBody @Valid CreateCertificationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProfileCertificationResponse response = profileService.createProfileCertification(profileId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.CERTIFICATION_CREATED, response);
    }

    @Operation(summary = "자격증 목록 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}/certifications")
    public ResponseEntity<BaseResponse<ProfileCertificationListResponse>> getProfileCertifications(
            @PathVariable("profileId") Long profileId,
            @RequestParam(name = "category", required = false) CertificationCategory category,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "newest") String sort,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProfileCertificationListResponse response =
                profileService.getProfileCertifications(profileId, category, page, size, sort, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.CERTIFICATION_LIST_RETRIEVED, response);
    }

    @Operation(summary = "자격증 상세 조회")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @GetMapping("/profiles/{profileId}/certifications/{certificationId}")
    public ResponseEntity<BaseResponse<ProfileCertificationResponse>> getProfileCertificationDetail(
            @PathVariable("profileId") Long profileId,
            @PathVariable("certificationId") Long certificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProfileCertificationResponse response =
                profileService.getProfileCertificationDetail(profileId, certificationId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.CERTIFICATION_RETRIEVED, response);
    }

    @Operation(summary = "자격증 수정")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @PatchMapping("/profiles/{profileId}/certifications/{certificationId}")
    public ResponseEntity<BaseResponse<ProfileCertificationResponse>> updateProfileCertification(
            @PathVariable("profileId") Long profileId,
            @PathVariable("certificationId") Long certificationId,
            @RequestBody @Valid UpdateCertificationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        ProfileCertificationResponse response =
                profileService.updateProfileCertification(profileId, certificationId, request, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.CERTIFICATION_UPDATED, response);
    }

    @Operation(summary = "자격증 삭제")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ProfileErrorCode.class)
    @DeleteMapping("/profiles/{profileId}/certifications/{certificationId}")
    public ResponseEntity<BaseResponse<Void>> deleteProfileCertification(
            @PathVariable("profileId") Long profileId,
            @PathVariable("certificationId") Long certificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Member member = getAuthenticatedMember(userDetails);
        profileService.deleteProfileCertification(profileId, certificationId, member);
        return BaseResponseFormatter.success(ProfileSuccessCode.CERTIFICATION_DELETED);
    }
}
