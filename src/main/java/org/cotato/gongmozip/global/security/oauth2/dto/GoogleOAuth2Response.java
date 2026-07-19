package org.cotato.gongmozip.global.security.oauth2.dto;

import java.util.Map;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;

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
        return attributes.get("email").toString();
    }
}
