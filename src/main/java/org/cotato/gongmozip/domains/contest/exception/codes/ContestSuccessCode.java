package org.cotato.gongmozip.domains.contest.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ContestSuccessCode implements BaseSuccessCode {

    // 200
    CONTEST_UPDATED(HttpStatus.OK, "CONTEST_200_1", "공모전 수정에 성공하였습니다."),
    CONTEST_LIST_RETRIEVED(HttpStatus.OK, "CONTEST_200_2", "공모전 목록 조회에 성공하였습니다."),
    CONTEST_RETRIEVED(HttpStatus.OK, "CONTEST_200_3", "공모전 상세 조회에 성공하였습니다."),
    SCRAP_STATUS_RETRIEVED(HttpStatus.OK, "CONTEST_200_4", "공모전 스크랩 여부 조회에 성공하였습니다."),
    SHARE_PREVIEW_RETRIEVED(HttpStatus.OK, "CONTEST_200_5", "공유용 공모전 정보 조회에 성공하였습니다."),
    CONTEST_CANDIDATE_LIST_RETRIEVED(HttpStatus.OK, "CONTEST_200_6", "후보 공모전 리스트 조회에 성공하였습니다."),
    CONTEST_VOTE_SUBMITTED(HttpStatus.OK, "CONTEST_200_7", "공모전 투표에 성공하였습니다."),
    RECOMMENDED_CONTESTS_RETRIEVED(HttpStatus.OK, "CONTEST_200_8", "추천 공모전 목록 조회에 성공하였습니다."),
    RECOMMENDATION_REASON_RETRIEVED(HttpStatus.OK, "CONTEST_200_9", "추천 사유 조회에 성공하였습니다."),
    CONTEST_VOTE_STATUS_RETRIEVED(HttpStatus.OK, "CONTEST_200_10", "공모전 투표 진행 상황 조회에 성공하였습니다."),

    // 201
    CONTEST_CREATED(HttpStatus.CREATED, "CONTEST_201_1", "공모전 등록에 성공하였습니다."),
    CONTEST_SCRAPPED(HttpStatus.CREATED, "CONTEST_201_2", "공모전 스크랩에 성공하였습니다."),
    CONTEST_CANDIDATE_ADDED(HttpStatus.CREATED, "CONTEST_201_3", "후보 공모전 추가에 성공하였습니다."),
    CONTEST_SHARED(HttpStatus.CREATED, "CONTEST_201_4", "공모전 공유에 성공하였습니다."),

    // 204
    CONTEST_DELETED(HttpStatus.NO_CONTENT, "CONTEST_204_1", "공모전 삭제에 성공하였습니다."),
    CONTEST_UNSCRAPPED(HttpStatus.NO_CONTENT, "CONTEST_204_2", "공모전 스크랩 취소에 성공하였습니다."),
    CONTEST_CANDIDATE_REMOVED(HttpStatus.NO_CONTENT, "CONTEST_204_3", "후보 공모전 삭제에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
