package org.cotato.gongmozip.domains.matching.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingSuccessCode implements BaseSuccessCode {
    // 200 OK
    ELIGIBILITY_RETRIEVED(HttpStatus.OK, "MATCHING_200_1", "매칭 신청 자격을 조회했습니다."),
    TODAY_APPLICATION_RETRIEVED(HttpStatus.OK, "MATCHING_200_2", "오늘의 매칭 신청 상태를 조회했습니다."),
    APPLICATION_WITHDRAWN(HttpStatus.OK, "MATCHING_200_3", "매칭 신청 철회가 완료되었습니다."),
    MATCHING_EXPLANATION_RETRIEVED(HttpStatus.OK, "MATCHING_200_4", "AI 분석 매칭 설명 조회 성공"),
    MATCHING_REASON_RETRIEVED(HttpStatus.OK, "MATCHING_200_5", "AI 매칭 추천 사유 조회 성공"),
    LEADER_RECOMMENDATION_RETRIEVED(HttpStatus.OK, "MATCHING_200_6", "AI 팀장 추천 조회 성공"),
    MATCHING_RESULT_RETRIEVED(HttpStatus.OK, "MATCHING_200_7", "오늘의 매칭 결과를 조회했습니다."),
    MATCHING_GROUP_RESPONSES_RETRIEVED(HttpStatus.OK, "MATCHING_200_8", "매칭 그룹 응답 현황을 조회했습니다."),
    MATCHING_RESPONSE_ACCEPTED(HttpStatus.OK, "MATCHING_200_9", "매칭 결과를 수락했습니다."),

    // 201 Created
    APPLICATION_CREATED(HttpStatus.CREATED, "MATCHING_201_1", "매칭풀 입장이 완료되었습니다."),

    // 202 Accepted
    MATCHING_REASON_REQUESTED(HttpStatus.ACCEPTED, "MATCHING_202_1", "AI 매칭 추천 사유 생성 요청이 접수되었습니다."),
    LEADER_RECOMMENDATION_REQUESTED(HttpStatus.ACCEPTED, "MATCHING_202_2", "AI 팀장 추천 생성 요청이 접수되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
