package org.cotato.gongmozip.global.security.oauth2.dto;

import java.util.Map;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthErrorCode;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

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
        if (kakaoAccount == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    AuthErrorCode.KAKAO_ACCOUNT_NOT_FOUND.getCode(),
                    AuthErrorCode.KAKAO_ACCOUNT_NOT_FOUND.getMessage(),
                    null));
        }
        Object email = kakaoAccount.get("email");
        if (email == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    AuthErrorCode.KAKAO_EMAIL_NOT_PROVIDED.getCode(),
                    AuthErrorCode.KAKAO_EMAIL_NOT_PROVIDED.getMessage(),
                    null));
        }
        return email.toString();
    }

    // 카카오는 사용자가 profile_nickname 동의를 거부할 수 있어 null 반환을 허용한다
    @Override
    @SuppressWarnings("unchecked")
    public String getName() {
        if (kakaoAccount == null) {
            return null;
        }
        Object profile = kakaoAccount.get("profile");
        if (!(profile instanceof Map)) {
            return null;
        }
        Object nickname = ((Map<String, Object>) profile).get("nickname");
        return nickname == null ? null : nickname.toString();
    }
}
