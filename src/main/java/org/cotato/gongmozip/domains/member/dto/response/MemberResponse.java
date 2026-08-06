package org.cotato.gongmozip.domains.member.dto.response;

import java.time.LocalDate;
import org.cotato.gongmozip.domains.member.enums.Gender;

public final class MemberResponse {

    private MemberResponse() {}

    public record MemberMeResponse(
            Long memberId,
            String email,
            String name,
            Gender gender,
            LocalDate birthDate,
            String snsType,
            Boolean snsLinked,
            Boolean marketingConsentEmail,
            Boolean marketingConsentSms,
            String profileImageUrl) {}
}
