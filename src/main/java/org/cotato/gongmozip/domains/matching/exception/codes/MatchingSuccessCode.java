package org.cotato.gongmozip.domains.matching.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingSuccessCode implements BaseSuccessCode {
    ELIGIBILITY_RETRIEVED(HttpStatus.OK, "MATCHING_200_1", "매칭 신청 자격을 조회했습니다."),
    TODAY_APPLICATION_RETRIEVED(HttpStatus.OK, "MATCHING_200_2", "오늘의 매칭 신청 상태를 조회했습니다."),
    APPLICATION_WITHDRAWN(HttpStatus.OK, "MATCHING_200_3", "매칭 신청 철회가 완료되었습니다."),
    APPLICATION_CREATED(HttpStatus.CREATED, "MATCHING_201_1", "매칭풀 입장이 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
