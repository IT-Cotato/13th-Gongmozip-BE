package org.cotato.gongmozip.domains.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import org.cotato.gongmozip.domains.upload.dto.response.GetPresignedUrlResponse;
import org.cotato.gongmozip.domains.upload.exception.UploadException;
import org.cotato.gongmozip.domains.upload.exception.codes.UploadErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private software.amazon.awssdk.services.s3.S3Client s3Client;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        // Inject @Value fields manually
        ReflectionTestUtils.setField(s3Service, "bucketName", "gongmozip-contest-images-test");
        ReflectionTestUtils.setField(s3Service, "region", "ap-northeast-2");
        ReflectionTestUtils.setField(s3Service, "cloudFrontDomain", "https://test.cloudfront.net");
    }

    @DisplayName("올바른 파일명과 Content-Type으로 요청하면 Presigned URL 및 CloudFront 이미지 URL이 생성된다.")
    @Test
    void Presigned_URL_발급_성공() throws Exception {
        // given
        String fileName = "poster.png";
        String contentType = "image/png";

        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        given(mockPresignedRequest.url()).willReturn(new URI("https://s3-upload-url.com").toURL());
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(mockPresignedRequest);

        // when
        GetPresignedUrlResponse response = s3Service.getPresignedUrlForUpload(fileName, contentType);

        // then
        assertThat(response.uploadUrl()).isEqualTo("https://s3-upload-url.com");
        assertThat(response.imageUrl())
                .startsWith("https://test.cloudfront.net/contests/posters/")
                .endsWith(".png");
        assertThat(response.contentType()).isEqualTo("image/png");
    }

    @DisplayName("JPEG 파일에 대해 image/jpg Content-Type으로 발급 요청하면, MIME 형식이 변환되지 않고 image/jpg 그대로 발급된다.")
    @Test
    void Presigned_URL_발급_JPG_타입_보존() throws Exception {
        // given
        String fileName = "profile.jpg";
        String contentType = "image/jpg";

        PresignedPutObjectRequest mockPresignedRequest = mock(PresignedPutObjectRequest.class);
        given(mockPresignedRequest.url()).willReturn(new URI("https://s3-upload-url.com").toURL());

        org.mockito.ArgumentCaptor<PutObjectPresignRequest> captor =
                org.mockito.ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        given(s3Presigner.presignPutObject(captor.capture())).willReturn(mockPresignedRequest);

        // when
        GetPresignedUrlResponse response = s3Service.getProfileImagePresignedUrl(fileName, contentType);

        // then
        assertThat(response.uploadUrl()).isEqualTo("https://s3-upload-url.com");
        assertThat(response.contentType()).isEqualTo("image/jpg");
        assertThat(response.imageUrl())
                .startsWith("https://test.cloudfront.net/members/profiles/")
                .endsWith(".jpg");
        assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo("image/jpg");
    }

    @DisplayName("허용되지 않은 확장자(예: txt)로 요청 시 예외가 발생한다.")
    @Test
    void 허용되지_않은_확장자_예외_발생() {
        // given
        String fileName = "attack.txt";
        String contentType = "text/plain";

        // when & then
        assertThatThrownBy(() -> s3Service.getPresignedUrlForUpload(fileName, contentType))
                .isInstanceOf(UploadException.class)
                .hasFieldOrPropertyWithValue("errorCode", UploadErrorCode.INVALID_FILE_TYPE);
    }

    @DisplayName("확장자와 Content-Type이 일치하지 않을 시 예외가 발생한다.")
    @Test
    void 확장자_MIME타입_불일치_예외_발생() {
        // given
        String fileName = "poster.png";
        String contentType = "image/jpeg"; // .png is mapped to image/png, not image/jpeg

        // when & then
        assertThatThrownBy(() -> s3Service.getPresignedUrlForUpload(fileName, contentType))
                .isInstanceOf(UploadException.class)
                .hasFieldOrPropertyWithValue("errorCode", UploadErrorCode.INVALID_FILE_TYPE);
    }

    @DisplayName("올바른 CloudFront URL을 삭제 요청하면 S3Client의 deleteObject가 정상 실행된다.")
    @Test
    void S3_파일_삭제_성공() {
        // given
        String imageUrl = "https://test.cloudfront.net/members/profiles/photo.png";

        // when
        s3Service.deleteFile(imageUrl);

        // then
        org.mockito.ArgumentCaptor<software.amazon.awssdk.services.s3.model.DeleteObjectRequest> captor =
                org.mockito.ArgumentCaptor.forClass(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class);
        org.mockito.Mockito.verify(s3Client, org.mockito.Mockito.times(1)).deleteObject(captor.capture());

        assertThat(captor.getValue().bucket()).isEqualTo("gongmozip-contest-images-test");
        assertThat(captor.getValue().key()).isEqualTo("members/profiles/photo.png");
    }
}
