package org.cotato.gongmozip.global.security.oauth2.dto;

import java.util.Map;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;

public class KakaoOAuth2Response implements OAuth2Response {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;

    @SuppressWarnings("unchecked")
    public KakaoOAuth2Response(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }

    @Override
    public String getProviderMemberId() {
        return attributes.get("id").toString();
    }

    @Override
    public String getEmail() {
        return kakaoAccount.get("email").toString();
    }
}
