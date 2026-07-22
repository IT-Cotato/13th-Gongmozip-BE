package org.cotato.gongmozip.domains.upload.dto.request;

import jakarta.validation.constraints.NotBlank;

public final class UploadRequest {

    private UploadRequest() {}

    public record GetPresignedUrlRequest(
            @NotBlank(message = "파일 이름은 필수 입력 항목입니다.") String fileName,
            @NotBlank(message = "Content-Type은 필수 입력 항목입니다.") String contentType) {}
}
