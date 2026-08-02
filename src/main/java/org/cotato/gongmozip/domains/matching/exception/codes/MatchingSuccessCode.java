package org.cotato.gongmozip.domains.matching.exception.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MatchingSuccessCode implements BaseSuccessCode {
    MATCHING_EXPLANATION_RETRIEVED(HttpStatus.OK, "MATCHING_200_1", "AI 분석 매칭 설명 조회 성공"),
    MATCHING_REASON_REQUESTED(HttpStatus.ACCEPTED, "MATCHING_202_1", "AI 매칭 추천 사유 생성 요청이 접수되었습니다."),
    MATCHING_REASON_RETRIEVED(HttpStatus.OK, "MATCHING_200_2", "AI 매칭 추천 사유 조회 성공"),
    LEADER_RECOMMENDATION_REQUESTED(HttpStatus.ACCEPTED, "MATCHING_202_2", "AI 팀장 추천 생성 요청이 접수되었습니다."),
    LEADER_RECOMMENDATION_RETRIEVED(HttpStatus.OK, "MATCHING_200_3", "AI 팀장 추천 조회 성공");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
