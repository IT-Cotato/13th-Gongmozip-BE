package org.cotato.gongmozip.domains.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.auth.converter.AuthAccountConverter;
import org.cotato.gongmozip.domains.auth.repository.AuthAccountRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.cotato.gongmozip.domains.member.enums.MemberStatus;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.domains.profile.repository.ProfileRepository;
import org.cotato.gongmozip.global.security.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚠️ 임시 개발용 엔드포인트. 이메일 인증 없이 회원(+기본 프로필)을 즉시 생성하고 accessToken을
 * 발급한다. WebSocket 채팅 등을 수동으로 테스트할 때 로그인 절차를 건너뛰기 위한 용도.
 * {@code local} 프로필을 명시적으로 활성화했을 때만 켜진다(기본값은 비활성 — 서버 배포 설정에
 * 기대지 않고 항상 안전한 쪽으로 fail-safe). 실제 회원가입/로그인 플로우가 자리잡으면 삭제한다.
 */
@org.springframework.context.annotation.Profile("local")
@Tag(name = "AuthTest", description = "[개발용] 이메일 인증 없이 토큰 발급 - 매칭/프로필 연동 전까지만 사용")
@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
public class AuthTestController {

    private final MemberRepository memberRepository;
    private final AuthAccountRepository authAccountRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Operation(summary = "[개발용] 이메일 인증 없이 회원/프로필을 자동 생성하고 accessToken 발급")
    @PostMapping("/quick-login")
    @Transactional
    public QuickLoginResponse quickLogin(@RequestBody @Valid QuickLoginRequest request) {
        Member member = memberRepository.findByEmail(request.email()).orElseGet(() -> createMember(request.email()));
        Profile profile = profileRepository.findAllByMemberOrderByUpdatedAtDesc(member).stream()
                .findFirst()
                .orElseGet(() -> createProfile(member));

        String accessToken = jwtProvider.generateAccessToken(member.getMemberId(), member.getEmail());
        return new QuickLoginResponse(member.getMemberId(), profile.getProfileId(), accessToken);
    }

    private Member createMember(String email) {
        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.USER)
                .gender(Gender.MALE)
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();
        Member saved = memberRepository.save(member);
        authAccountRepository.save(AuthAccountConverter.toEmailAuthAccount(saved));
        return saved;
    }

    private Profile createProfile(Member member) {
        Profile profile = Profile.builder()
                .member(member)
                .nickname("테스트_" + member.getMemberId())
                .schoolName("테스트대학교")
                .grade(4)
                .major("테스트학과")
                .gpa(4.0)
                .gpaScale(4.5)
                .interestCategories(List.of(InterestCategory.IT_AI_TECH))
                .isPublic(true)
                .build();
        return profileRepository.save(profile);
    }

    public record QuickLoginRequest(@Email @NotBlank String email) {}

    public record QuickLoginResponse(Long memberId, Long profileId, String accessToken) {}
}
