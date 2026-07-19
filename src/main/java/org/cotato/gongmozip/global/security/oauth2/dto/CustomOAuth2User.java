package org.cotato.gongmozip.global.security.oauth2.dto;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.enums.MemberRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class CustomOAuth2User implements OAuth2User {

    private final Long memberId;
    private final String email;
    private final MemberRole role;
    private final AuthProvider provider;

    // @Transactional 종료 후 getAuthorities() 호출 시 LAZY 프록시 초기화 실패를 막기 위해 생성자 안에서 값을 미리 추출
    public CustomOAuth2User(Member member, AuthProvider provider) {
        this.memberId = member.getMemberId();
        this.email = member.getEmail();
        this.role = member.getRole();
        this.provider = provider;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return email;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getEmail() {
        return email;
    }

    public AuthProvider getProvider() {
        return provider;
    }
}
