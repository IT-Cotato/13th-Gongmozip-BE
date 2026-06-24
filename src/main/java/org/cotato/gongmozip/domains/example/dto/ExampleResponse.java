package org.cotato.gongmozip.domains.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "예시 응답")
@Getter
@Builder
public class ExampleResponse {

    @Schema(description = "예시 ID", example = "1")
    private Long id;

    @Schema(description = "제목", example = "첫 번째 예시")
    private String title;

    @Schema(description = "내용", example = "내용 1")
    private String content;
}
