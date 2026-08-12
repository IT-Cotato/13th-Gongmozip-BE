package org.cotato.gongmozip.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    // security에서 로그인 처리 시 사용하는 메서드
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository
                .findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        // 탈퇴한 회원은 미만료 access 토큰이 남아 있어도 인증을 차단한다 (블랙리스트와 이중 방어)
        if (member.isWithdrawn()) {
            throw new MemberException(MemberErrorCode.WITHDRAWN_MEMBER);
        }

        return new CustomUserDetails(member);
    }
}
