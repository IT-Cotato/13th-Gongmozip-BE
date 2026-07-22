package org.cotato.gongmozip.domains.upload.service;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse;
import org.cotato.gongmozip.domains.upload.exception.UploadException;
import org.cotato.gongmozip.domains.upload.exception.codes.UploadErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region}")
    private String region;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;

    public GetPresignedUrlResponse getPresignedUrlForUpload(String fileName, String contentType) {
        validateImageFile(fileName, contentType);

        String uniqueFileName = generateUniqueFileName(fileName);
        String objectKey = "contests/posters/" + uniqueFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // URL valid for 10 minutes
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedPutObjectRequest.url().toString();

        // Calculate CloudFront URL for image retrieval
        String baseDomain = cloudFrontDomain.endsWith("/")
                ? cloudFrontDomain.substring(0, cloudFrontDomain.length() - 1)
                : cloudFrontDomain;
        String imageUrl = baseDomain + "/" + objectKey;

        return new GetPresignedUrlResponse(uploadUrl, imageUrl);
    }

    private void validateImageFile(String fileName, String contentType) {
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new UploadException(UploadErrorCode.INVALID_FILE_TYPE);
        }

        String extension = getFileExtension(fileName).toLowerCase();
        if (!extension.equals("jpg")
                && !extension.equals("jpeg")
                && !extension.equals("png")
                && !extension.equals("gif")
                && !extension.equals("webp")) {
            throw new UploadException(UploadErrorCode.INVALID_FILE_TYPE);
        }
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    private String generateUniqueFileName(String originalFileName) {
        String cleanName = originalFileName.replaceAll("\\s+", "_");
        return UUID.randomUUID() + "_" + cleanName;
    }
}
