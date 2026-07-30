package org.cotato.gongmozip.domains.chat.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatSuccessCode implements BaseSuccessCode {

    // 200
    MESSAGE_LIST_RETRIEVED(HttpStatus.OK, "CHAT_200_1", "메시지 목록 조회에 성공하였습니다."),

    // 204
    MESSAGES_MARKED_READ(HttpStatus.NO_CONTENT, "CHAT_204_1", "읽음 처리에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
