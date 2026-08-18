package org.cotato.gongmozip.domains.team.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamSuccessCode implements BaseSuccessCode {

    // 200
    CHAT_ROOM_LIST_RETRIEVED(HttpStatus.OK, "TEAM_200_1", "채팅방 목록 조회에 성공하였습니다."),
    TEAM_MEMBERS_RETRIEVED(HttpStatus.OK, "TEAM_200_2", "대화상대 조회에 성공하였습니다."),
    CHATBOT_TOGGLED(HttpStatus.OK, "TEAM_200_3", "챗봇 설정 변경에 성공하였습니다."),
    LEADER_CANDIDACY_SUBMITTED(HttpStatus.OK, "TEAM_200_4", "팀장 여부 투표에 성공하였습니다."),
    LEADER_VOTE_SUBMITTED(HttpStatus.OK, "TEAM_200_5", "팀장 투표에 성공하였습니다."),
    SUBMISSION_RECORDED(HttpStatus.OK, "TEAM_200_7", "제출 여부 응답에 성공하였습니다."),
    AI_RECOMMENDATION_ACCEPTED(HttpStatus.OK, "TEAM_200_8", "AI 추천을 수락하여 팀장이 확정되었습니다."),
    LEADER_REVOTE_REQUESTED(HttpStatus.OK, "TEAM_200_9", "재투표 안내 메시지를 발행했습니다."),
    LEADER_CANDIDACY_STATUS_RETRIEVED(HttpStatus.OK, "TEAM_200_10", "팀장 여부 투표 진행 상황 조회에 성공하였습니다."),
    LEADER_VOTE_STATUS_RETRIEVED(HttpStatus.OK, "TEAM_200_11", "팀장 투표 진행 상황 조회에 성공하였습니다."),

    // 204
    LEFT_CHAT_ROOM(HttpStatus.NO_CONTENT, "TEAM_204_1", "채팅방 나가기에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
