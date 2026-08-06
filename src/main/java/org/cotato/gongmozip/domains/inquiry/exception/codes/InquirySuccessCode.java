package org.cotato.gongmozip.domains.inquiry.exception.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InquirySuccessCode implements BaseSuccessCode {

    // 200
    INQUIRY_LIST_RETRIEVED(HttpStatus.OK, "INQUIRY_200_1", "문의 내역 조회 성공"),
    INQUIRY_DETAIL_RETRIEVED(HttpStatus.OK, "INQUIRY_200_2", "문의 상세 조회 성공"),
    ADMIN_INQUIRY_LIST_RETRIEVED(HttpStatus.OK, "INQUIRY_200_3", "관리자 문의 목록 조회 성공"),
    ADMIN_INQUIRY_DETAIL_RETRIEVED(HttpStatus.OK, "INQUIRY_200_4", "관리자 문의 상세 조회 성공"),
    INQUIRY_ANSWERED(HttpStatus.OK, "INQUIRY_200_5", "문의 답변이 등록되었습니다."),

    // 201
    INQUIRY_CREATED(HttpStatus.CREATED, "INQUIRY_201_1", "문의가 접수되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
