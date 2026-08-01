package org.cotato.gongmozip.domains.matching.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MatchingErrorCode implements BaseErrorCode {
    APPLICATION_DEADLINE_PASSED(HttpStatus.BAD_REQUEST, "MATCHING_400_1", "오늘의 매칭 신청이 마감되었습니다."),
    PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "MATCHING_400_2", "매칭에 사용할 프로필 작성이 필요합니다."),
    SURVEY_REQUIRED(HttpStatus.BAD_REQUEST, "MATCHING_400_3", "협업 유형 검사를 완료해야 합니다."),
    WITHDRAWAL_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "MATCHING_400_4", "현재는 매칭 신청을 철회할 수 없습니다."),

    PROFILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MATCHING_403_1", "본인의 프로필만 매칭에 사용할 수 있습니다."),
    MATCHING_RESTRICTED(HttpStatus.FORBIDDEN, "MATCHING_403_2", "협업거리 감소로 인해 현재 매칭 참여가 제한되어 있습니다."),

    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCHING_404_1", "매칭 신청을 찾을 수 없습니다."),

    ALREADY_APPLIED_TODAY(HttpStatus.CONFLICT, "MATCHING_409_1", "매칭풀에는 하루에 한 번만 입장할 수 있습니다."),
    INVALID_APPLICATION_STATUS(HttpStatus.CONFLICT, "MATCHING_409_2", "현재 신청 상태에서는 해당 요청을 처리할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
