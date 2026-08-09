package org.cotato.gongmozip.domains.upload.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
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

@lombok.extern.slf4j.Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final Map<String, List<String>> ALLOWED_EXTENSIONS_TO_MIMES = Map.ofEntries(
            Map.entry("jpg", List.of("image/jpeg", "image/jpg", "image/pjpeg")),
            Map.entry("jpeg", List.of("image/jpeg", "image/jpg", "image/pjpeg")),
            Map.entry("png", List.of("image/png", "image/x-png")),
            Map.entry("gif", List.of("image/gif")),
            Map.entry("webp", List.of("image/webp")),
            Map.entry("heic", List.of("image/heic", "image/heic-sequence")),
            Map.entry("heif", List.of("image/heif", "image/heif-sequence")),
            Map.entry("bmp", List.of("image/bmp", "image/x-windows-bmp")),
            Map.entry("tiff", List.of("image/tiff")),
            Map.entry("tif", List.of("image/tiff")),
            Map.entry("ico", List.of("image/x-icon", "image/vnd.microsoft.icon")));

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
        String extension = getFileExtension(fileName).toLowerCase(Locale.ROOT);

        List<String> allowedMimes = ALLOWED_EXTENSIONS_TO_MIMES.get(extension);
        if (allowedMimes == null) {
            throw new UploadException(UploadErrorCode.INVALID_FILE_TYPE);
        }

        if (contentType == null) {
            throw new UploadException(UploadErrorCode.INVALID_FILE_TYPE);
        }

        String trimmedContentType = contentType.trim();
        boolean match = allowedMimes.stream().anyMatch(mime -> mime.equalsIgnoreCase(trimmedContentType));
        if (!match) {
            throw new UploadException(UploadErrorCode.INVALID_FILE_TYPE);
        }

        return trimmedContentType;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    private String generateUniqueFileName(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase(Locale.ROOT);
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
                log.warn("Failed to delete S3 file. imageUrl: {}, error: {}", imageUrl, e.getMessage(), e);
            }
        }
    }
}
