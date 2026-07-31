package org.cotato.gongmozip.domains.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ChatRequest {

    private ChatRequest() {}

    public record SendMessageRequest(
            @NotBlank(message = "메시지 내용은 필수 입력 항목입니다.") @Size(max = 2000, message = "메시지는 최대 2000자까지 입력 가능합니다.")
                    String content) {}
}
