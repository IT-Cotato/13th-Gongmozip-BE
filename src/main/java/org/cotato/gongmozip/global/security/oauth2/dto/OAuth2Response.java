package org.cotato.gongmozip.global.security.oauth2.dto;

import org.cotato.gongmozip.domains.auth.enums.AuthProvider;

public interface OAuth2Response {

    AuthProvider getProvider();

    String getProviderMemberId();

    String getEmail();

    String getName();
}
