package org.cotato.gongmozip.domains.mypage.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MyPageSuccessCode implements BaseSuccessCode {

    // 200
    MYPAGE_MAIN_RETRIEVED(HttpStatus.OK, "MYPAGE_200_1", "마이페이지 메인 조회에 성공하였습니다."),
    ONGOING_PROJECTS_RETRIEVED(HttpStatus.OK, "MYPAGE_200_2", "진행 중 프로젝트 조회에 성공하였습니다."),
    COMPLETED_PROJECTS_RETRIEVED(HttpStatus.OK, "MYPAGE_200_3", "완료 프로젝트 조회에 성공하였습니다."),
    REVIEWS_RETRIEVED(HttpStatus.OK, "MYPAGE_200_4", "받은 팀원 후기 조회에 성공하였습니다."),
    SCRAPPED_CONTESTS_RETRIEVED(HttpStatus.OK, "MYPAGE_200_5", "내 스크랩 공모전 조회에 성공하였습니다."),
    COMPLETED_PROJECT_DELETED(HttpStatus.OK, "MYPAGE_200_6", "완료 프로젝트 기록 삭제에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
