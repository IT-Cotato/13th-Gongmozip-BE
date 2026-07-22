package org.cotato.gongmozip.domains.upload.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.upload.dto.request.UploadRequest.GetPresignedUrlRequest;
import org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse;
import org.cotato.gongmozip.domains.upload.exception.codes.UploadErrorCode;
import org.cotato.gongmozip.domains.upload.exception.codes.UploadSuccessCode;
import org.cotato.gongmozip.domains.upload.service.S3Service;
import org.cotato.gongmozip.global.exception.GlobalErrorCode;
import org.cotato.gongmozip.global.response.BaseResponse;
import org.cotato.gongmozip.global.response.BaseResponseFormatter;
import org.cotato.gongmozip.global.swagger.CustomErrorCodes;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Upload", description = "이미지 업로드 관련 API")
@RestController
@RequestMapping("/api/admin/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

    private final S3Service s3Service;

    @Operation(summary = "관리자용 S3 Presigned URL 발급")
    @CustomErrorCodes(commonErrorCodes = GlobalErrorCode.class, domainErrorCodes = UploadErrorCode.class)
    @PostMapping("/presigned-url")
    public ResponseEntity<BaseResponse<GetPresignedUrlResponse>> getPresignedUrl(
            @RequestBody @Validated GetPresignedUrlRequest request) {
        GetPresignedUrlResponse response =
                s3Service.getPresignedUrlForUpload(request.fileName(), request.contentType());
        return BaseResponseFormatter.success(UploadSuccessCode.PRESIGNED_URL_GENERATED, response);
    }
}
