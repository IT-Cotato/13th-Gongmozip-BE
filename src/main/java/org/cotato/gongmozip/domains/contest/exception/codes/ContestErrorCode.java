package org.cotato.gongmozip.domains.contest.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContestErrorCode implements BaseErrorCode {

    // 400
    INVALID_CONTEST_INPUT(HttpStatus.BAD_REQUEST, "CONTEST_400_1", "공모전 입력값이 올바르지 않습니다."),
    INVALID_CONTEST_VOTE_SELECTION(HttpStatus.BAD_REQUEST, "CONTEST_400_2", "공모전은 1~2개만 선택할 수 있습니다."),
    NOT_RECOMMENDED_CONTEST(HttpStatus.BAD_REQUEST, "CONTEST_400_3", "추천 대상이 아닌 공모전입니다."),

    // 404
    CONTEST_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTEST_404_1", "존재하지 않는 공모전입니다."),
    SCRAP_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTEST_404_2", "공모전 또는 스크랩 정보를 찾을 수 없습니다."),
    CONTEST_CANDIDATE_NOT_FOUND(HttpStatus.NOT_FOUND, "CONTEST_404_3", "존재하지 않는 후보 공모전입니다."),

    // 409
    ALREADY_SCRAPPED(HttpStatus.CONFLICT, "CONTEST_409_1", "이미 스크랩한 공모전입니다."),
    DUPLICATE_CONTEST(HttpStatus.CONFLICT, "CONTEST_409_2", "동일한 공모전이 이미 등록되어 있습니다."),
    DUPLICATE_CONTEST_CANDIDATE(HttpStatus.CONFLICT, "CONTEST_409_3", "이미 후보로 추가된 공모전입니다."),
    ALREADY_VOTED_CONTEST(HttpStatus.CONFLICT, "CONTEST_409_4", "이미 이번 라운드에 투표했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
