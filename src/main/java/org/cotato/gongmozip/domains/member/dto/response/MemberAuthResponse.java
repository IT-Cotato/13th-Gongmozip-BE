package org.cotato.gongmozip.domains.member.dto.response;

public class MemberAuthResponse {

    // 회원가입 응답
    public record SignUpResponse(Long memberId, String email) {}
}
