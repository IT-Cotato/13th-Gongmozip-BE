package org.cotato.gongmozip.domains.example.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.global.response.BaseSuccessCode;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExampleSuccessCode implements BaseSuccessCode {
    EXAMPLE_FETCH_SUCCESS(HttpStatus.OK, "EXAMPLE_200_1", "목록 조회에 성공하였습니다."),
    EXAMPLE_DETAIL_SUCCESS(HttpStatus.OK, "EXAMPLE_200_2", "단건 조회에 성공하였습니다."),
    EXAMPLE_CREATE_SUCCESS(HttpStatus.CREATED, "EXAMPLE_201_1", "생성에 성공하였습니다."),
    EXAMPLE_UPDATE_SUCCESS(HttpStatus.OK, "EXAMPLE_200_3", "수정에 성공하였습니다."),
    EXAMPLE_DELETE_SUCCESS(HttpStatus.OK, "EXAMPLE_200_4", "삭제에 성공하였습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
