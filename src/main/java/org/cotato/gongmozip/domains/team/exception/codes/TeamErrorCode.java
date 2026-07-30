package org.cotato.gongmozip.domains.team.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.exception.BaseErrorCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements BaseErrorCode {

    // 400
    EMPTY_TEAM_MEMBER_LIST(HttpStatus.BAD_REQUEST, "TEAM_400_1", "팀에는 최소 1명 이상의 팀원이 필요합니다."),
    DUPLICATE_TEAM_MEMBER_INPUT(HttpStatus.BAD_REQUEST, "TEAM_400_2", "동일한 회원이 팀 생성 요청에 중복 포함되어 있습니다."),
    INVALID_TEAM_STATUS(HttpStatus.BAD_REQUEST, "TEAM_400_3", "지금 단계에서는 수행할 수 없는 요청입니다."),
    LEADER_CANDIDACY_PENDING(HttpStatus.BAD_REQUEST, "TEAM_400_4", "아직 팀장 여부 투표를 마치지 않은 팀원이 있습니다."),
    INVALID_LEADER_CANDIDATE(HttpStatus.BAD_REQUEST, "TEAM_400_5", "이번 라운드에서 투표할 수 없는 후보입니다."),
    NO_PENDING_AI_RECOMMENDATION(HttpStatus.BAD_REQUEST, "TEAM_400_6", "현재 수락할 수 있는 AI 추천이 없습니다."),

    // 403
    NOT_TEAM_MEMBER(HttpStatus.FORBIDDEN, "TEAM_403_1", "해당 채팅방(팀)의 참여자가 아닙니다."),
    NOT_TEAM_LEADER(HttpStatus.FORBIDDEN, "TEAM_403_2", "팀장만 수행할 수 있는 요청입니다."),

    // 404
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "TEAM_404_1", "존재하지 않는 팀입니다."),

    // 409
    ALREADY_VOTED_LEADER(HttpStatus.CONFLICT, "TEAM_409_1", "이미 이번 라운드에 투표했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
