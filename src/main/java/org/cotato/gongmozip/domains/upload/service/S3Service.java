package org.cotato.gongmozip.domains.upload.service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse;
import org.cotato.gongmozip.domains.upload.exception.UploadException;
import org.cotato.gongmozip.domains.upload.exception.codes.UploadErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "gif", "image/gif",
            "webp", "image/webp");

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region}")
    private String region;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;

    public GetPresignedUrlResponse getPresignedUrlForUpload(String fileName, String contentType) {
        String canonicalMimeType = validateAndGetCanonicalMimeType(fileName, contentType);

        String uniqueFileName = generateUniqueFileName(fileName);
        String objectKey = "contests/posters/" + uniqueFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(canonicalMimeType)
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

        return new GetPresignedUrlResponse(uploadUrl, imageUrl, canonicalMimeType);
    }

    public GetPresignedUrlResponse getProfileImagePresignedUrl(String fileName, String contentType) {
        String canonicalMimeType = validateAndGetCanonicalMimeType(fileName, contentType);

        String uniqueFileName = generateUniqueFileName(fileName);
        String objectKey = "members/profiles/" + uniqueFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(canonicalMimeType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // URL valid for 10 minutes
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedPutObjectRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedPutObjectRequest.url().toString();

        String baseDomain = cloudFrontDomain.endsWith("/")
                ? cloudFrontDomain.substring(0, cloudFrontDomain.length() - 1)
                : cloudFrontDomain;
        String imageUrl = baseDomain + "/" + objectKey;

        return new GetPresignedUrlResponse(uploadUrl, imageUrl, canonicalMimeType);
    }

    private String validateAndGetCanonicalMimeType(String fileName, String contentType) {
        String extension = getFileExtension(fileName).toLowerCase();

        String canonicalMimeType = ALLOWED_IMAGE_TYPES.get(extension);
        if (canonicalMimeType == null) {
            throw new UploadException(UploadErrorCode.INVALID_FILE_TYPE);
        }

        if (!canonicalMimeType.equalsIgnoreCase(contentType)) {
            throw new UploadException(UploadErrorCode.INVALID_FILE_TYPE);
        }

        return canonicalMimeType;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    private String generateUniqueFileName(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        return UUID.randomUUID() + "." + extension;
    }

    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        String domainPattern = cloudFrontDomain.endsWith("/") ? cloudFrontDomain : cloudFrontDomain + "/";

        if (imageUrl.startsWith(domainPattern)) {
            String objectKey = imageUrl.substring(domainPattern.length());
            try {
                DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .build();
                s3Client.deleteObject(deleteObjectRequest);
            } catch (Exception e) {
                // Log and ignore to prevent blocking user profile updates if S3 deletion fails
            }
        }
    }
}
