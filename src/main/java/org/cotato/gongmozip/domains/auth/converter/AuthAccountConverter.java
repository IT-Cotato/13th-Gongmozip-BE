package org.cotato.gongmozip.domains.auth.converter;

import org.cotato.gongmozip.domains.auth.entity.AuthAccount;
import org.cotato.gongmozip.domains.auth.enums.AuthProvider;
import org.cotato.gongmozip.domains.member.entity.Member;

public class AuthAccountConverter {

    public static AuthAccount toEmailAuthAccount(Member member) {
        return AuthAccount.builder()
                .member(member)
                .provider(AuthProvider.EMAIL)
                .build();
    }
}
