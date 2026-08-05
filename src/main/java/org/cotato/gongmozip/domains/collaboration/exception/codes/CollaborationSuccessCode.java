package org.cotato.gongmozip.domains.collaboration.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CollaborationSuccessCode implements BaseSuccessCode {
    COLLABORATION_DISTANCE_RETRIEVED(HttpStatus.OK, "COLLABORATION_200_1", "협업거리 게이지 조회 성공"),
    COLLABORATION_HISTORY_RETRIEVED(HttpStatus.OK, "COLLABORATION_200_2", "협업거리 변경 내역 조회 성공");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
