package org.cotato.gongmozip.global.security.oauth2.service;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.auth.entity.AuthAccount;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.auth.repository.AuthAccountRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.global.security.oauth2.dto.CustomOAuth2User;
import org.cotato.gongmozip.global.security.oauth2.dto.GoogleOAuth2Response;
import org.cotato.gongmozip.global.security.oauth2.dto.KakaoOAuth2Response;
import org.cotato.gongmozip.global.security.oauth2.dto.OAuth2Response;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final AuthAccountRepository authAccountRepository;

    // 소셜 로그인 사용자 조회 메서드 (Spring Security OAuth2 흐름에서 자동 호출)
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 어떤 소셜 제공자인지 확인 후 응답 파싱 (google / kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Response oAuth2Response = parseResponse(registrationId, oAuth2User.getAttributes());

        AuthProvider provider = oAuth2Response.getProvider();
        String providerMemberId = oAuth2Response.getProviderMemberId();
        String email = oAuth2Response.getEmail();
        String name = oAuth2Response.getName();

        // 기존 소셜 계정이 있으면 해당 멤버, 없으면 신규 가입 또는 기존 멤버에 소셜 계정 연동
        Member member = authAccountRepository
                .findByProviderAndProviderMemberId(provider, providerMemberId)
                .map(AuthAccount::getMember)
                .orElseGet(() -> registerOrLink(email, name, provider, providerMemberId));

        validateNotWithdrawn(member);

        // 재로그인/신규 가입 모두에서, name이 비어있으면 소셜 프로필로 백필
        member.backfillNameIfAbsent(name);

        return new CustomOAuth2User(member, provider, isRequiredInfoMissing(member));
    }

    // 신규 가입 또는 이메일이 같은 기존 멤버에 소셜 계정 연동
    private Member registerOrLink(String email, String name, AuthProvider provider, String providerMemberId) {
        // 이메일이 동일한 멤버가 있으면 재사용, 없으면 소셜에서 받은 이름으로 신규 생성
        Member member = memberRepository
                .findByEmail(email)
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .email(email)
                        .name(name)
                        .status(MemberStatus.ACTIVE)
                        .emailVerifiedAt(LocalDateTime.now())
                        .build()));

        // 소셜 인증 계정 저장
        authAccountRepository.save(AuthAccount.builder()
                .member(member)
                .provider(provider)
                .providerMemberId(providerMemberId)
                .build());

        return member;
    }

    private void validateNotWithdrawn(Member member) {
        if (member.isWithdrawn()) {
            throw new OAuth2AuthenticationException("탈퇴한 회원입니다. 탈퇴 후 14일간 재가입할 수 없습니다.");
        }
    }

    private boolean isRequiredInfoMissing(Member member) {
        return member.getGender() == null || member.getBirthDate() == null;
    }

    // 소셜 제공자 식별자(registrationId)에 따라 응답 파싱 구현체 선택
    private OAuth2Response parseResponse(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "google" -> new GoogleOAuth2Response(attributes);
            case "kakao" -> new KakaoOAuth2Response(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        };
    }
}
