package org.cotato.gongmozip.global.security.oauth2.dto;

import java.util.Map;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.auth.exception.codes.AuthErrorCode;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class GoogleOAuth2Response implements OAuth2Response {

    private final Map<String, Object> attributes;

    public GoogleOAuth2Response(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public String getProviderMemberId() {
        return attributes.get("sub").toString();
    }

    @Override
    public String getEmail() {
        Object email = attributes.get("email");
        if (email == null) {
            throw new OAuth2AuthenticationException(
                new OAuth2Error(AuthErrorCode.GOOGLE_EMAIL_NOT_PROVIDED.getCode(),
                    AuthErrorCode.GOOGLE_EMAIL_NOT_PROVIDED.getMessage(), null));
        }
        return email.toString();
    }
}
