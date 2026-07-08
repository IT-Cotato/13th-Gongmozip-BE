package org.cotato.gongmozip.domains.member.converter;

import java.time.LocalDateTime;
import org.cotato.gongmozip.domains.member.dto.request.MemberAuthRequest.SignUpRequest;
import org.cotato.gongmozip.domains.member.dto.response.MemberAuthResponse.SignUpResponse;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.entity.MemberProfile;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;

public class MemberConverter {

    // dto -> entity
    public static Member toMember(SignUpRequest request, String encodedPassword) {
        return Member.builder()
                .email(request.email())
                .password(encodedPassword)
                .status(MemberStatus.ACTIVE)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
    }

    public static MemberProfile toMemberProfile(SignUpRequest request, Member member) {
        return MemberProfile.builder()
                .member(member)
                .gender(request.gender())
                .birthDate(request.birthDate())
                .build();
    }

    // entity -> dto
    public static SignUpResponse toSignUpResponse(Member member) {
        return new SignUpResponse(member.getMemberId(), member.getEmail());
    }
}
