package org.cotato.gongmozip.domains.matching.exception.codes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MatchingErrorCode implements BaseErrorCode {
    MATCHING_GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_1", "매칭 결과를 찾을 수 없습니다."),
    MATCHING_GROUP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MATCHING_403_1", "해당 매칭 결과에 접근할 권한이 없습니다."),
    MATCHING_REASON_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_2", "AI 추천 사유를 찾을 수 없습니다."),
    MATCHING_REASON_IN_PROGRESS(HttpStatus.CONFLICT, "MATCHING_409_1", "해당 매칭 결과의 추천 사유가 이미 생성 중입니다."),
    EXPLANATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_3", "AI 분석 매칭 설명을 찾을 수 없습니다."),
    LEADER_RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_4", "팀 또는 팀장 추천 결과를 찾을 수 없습니다."),
    LEADER_RECOMMENDATION_IN_PROGRESS(HttpStatus.CONFLICT, "MATCHING_409_2", "해당 팀의 팀장 추천이 이미 진행 중입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
