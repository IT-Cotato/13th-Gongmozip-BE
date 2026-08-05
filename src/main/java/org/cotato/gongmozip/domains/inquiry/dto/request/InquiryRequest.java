package org.cotato.gongmozip.domains.inquiry.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class InquiryRequest {

    // 문의 등록 요청
    public record CreateInquiryRequest(
            @Email @NotBlank(message = "이메일은 필수 입력 항목입니다.") String email,
            @NotBlank(message = "문의 비밀번호는 필수 입력 항목입니다.")
                    @Pattern(regexp = "^\\d{4}$", message = "문의 비밀번호는 숫자 4자리여야 합니다.")
                    String password,
            @NotBlank(message = "문의 제목은 필수 입력 항목입니다.") @Size(max = 20, message = "문의 제목은 최대 20자까지 입력 가능합니다.")
                    String title,
            @NotBlank(message = "문의 내용은 필수 입력 항목입니다.") @Size(max = 1000, message = "문의 내용은 최대 1000자까지 입력 가능합니다.")
                    String content) {}

    // 문의 내역/상세 조회 요청 (작성 시 입력한 이메일 + 문의 비밀번호)
    public record InquiryAuthRequest(
            @Email @NotBlank(message = "이메일은 필수 입력 항목입니다.") String email,
            @NotBlank(message = "문의 비밀번호는 필수 입력 항목입니다.")
                    @Pattern(regexp = "^\\d{4}$", message = "문의 비밀번호는 숫자 4자리여야 합니다.")
                    String password) {}
}
