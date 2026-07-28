package org.cotato.gongmozip.domains.character.exception;

import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.cotato.gongmozip.global.exception.CustomException;

@Getter
public class CharacterException extends CustomException {

    public CharacterException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
