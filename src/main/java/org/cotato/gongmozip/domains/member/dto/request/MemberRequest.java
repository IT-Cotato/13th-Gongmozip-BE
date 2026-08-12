package org.cotato.gongmozip.domains.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.cotato.gongmozip.domains.member.enums.WithdrawalReasonType;
import org.cotato.gongmozip.global.validation.annotation.ValidBirthDate;

public final class MemberRequest {

    private MemberRequest() {}

    public record UpdateMemberMeRequest(
            @NotBlank(message = "이름은 필수 입력 항목입니다.") @Size(max = 50, message = "이름은 최대 50자까지 입력 가능합니다.") String name,
            @NotNull(message = "성별은 필수 입력 항목입니다.") Gender gender,
            @NotNull(message = "생년월일은 필수 입력 항목입니다.") @ValidBirthDate(minimumAge = 14) LocalDate birthDate) {}

    public record UpdateMarketingConsentRequest(
            @NotNull(message = "이메일 마케팅 수신동의 여부는 필수 입력 항목입니다.") Boolean marketingConsentEmail,
            @NotNull(message = "SMS 마케팅 수신동의 여부는 필수 입력 항목입니다.") Boolean marketingConsentSms) {}

    public record GetProfileImagePresignedUrlRequest(
            @NotBlank(message = "파일명은 필수 입력 항목입니다.") String fileName,
            @NotBlank(message = "컨텐츠 타입은 필수 입력 항목입니다.") String contentType) {}

    public record UpdateProfileImageRequest(String profileImageUrl) {}

    public record WithdrawMemberRequest(
            String password,
            @NotNull(message = "탈퇴 사유는 필수 입력 항목입니다.") WithdrawalReasonType reason,
            @Size(max = 500, message = "탈퇴 사유 상세는 최대 500자까지 입력 가능합니다.") String reasonDetail) {}
}
