package org.cotato.gongmozip.domains.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.cotato.gongmozip.domains.example.dto.ExampleResponse;
import org.cotato.gongmozip.domains.example.exception.ExampleErrorCode;
import org.cotato.gongmozip.domains.example.exception.ExampleSuccessCode;
import org.cotato.gongmozip.global.exception.CustomException;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Example", description = "응답 구조 테스트용 예시 API")
@RestController
@RequestMapping("/api/examples")
public class ExampleController {

    @Operation(summary = "목록 조회 예시")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ExampleErrorCode.class)
    @GetMapping
    public ResponseEntity<BaseResponse<List<ExampleResponse>>> getExamples() {
        List<ExampleResponse> data = List.of(
                ExampleResponse.builder()
                        .id(1L)
                        .title("첫 번째 예시")
                        .content("내용 1")
                        .build(),
                ExampleResponse.builder()
                        .id(2L)
                        .title("두 번째 예시")
                        .content("내용 2")
                        .build());
        return BaseResponseFormatter.success(ExampleSuccessCode.EXAMPLE_FETCH_SUCCESS, data);
    }

    @Operation(summary = "단건 조회 예시")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = ExampleErrorCode.class)
    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<ExampleResponse>> getExample(@PathVariable Long id) {
        // id > 100이면 에러 응답 테스트용 더미
        if (id > 100) {
            throw new CustomException(ExampleErrorCode.EXAMPLE_NOT_FOUND);
        }
        ExampleResponse data = ExampleResponse.builder()
                .id(id)
                .title("예시 데이터 " + id)
                .content(id + "번 예시의 내용입니다.")
                .build();
        return BaseResponseFormatter.success(ExampleSuccessCode.EXAMPLE_DETAIL_SUCCESS, data);
    }
}
