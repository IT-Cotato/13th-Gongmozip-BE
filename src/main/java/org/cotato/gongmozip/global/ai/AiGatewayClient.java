package org.cotato.gongmozip.global.ai;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 팩트챗 API Gateway(https://factchat-cloud.mindlogic.ai/v1/gateway) 호출 전담 클라이언트.
 * OpenAI Chat Completions API 호환 포맷을 이용해 다양한 LLM 모델(OpenAI, Gemini, Claude 등)을 호출할 수 있습니다.
 */
@Slf4j
@Component
public class AiGatewayClient {

    private static final int TIMEOUT_MILLIS = 20000;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public AiGatewayClient(
            @Value("${ai.gateway.api-key:}") String apiKey,
            @Value("${ai.gateway.base-url:https://factchat-cloud.mindlogic.ai/v1/gateway}") String baseUrl,
            @Value("${ai.gateway.model:gpt-4o}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 프롬프트를 OpenAI Chat Completions 규격으로 변환하여 API Gateway에 전달하고 답변을 반환합니다. */
    public String generateContent(String prompt) {
        ChatCompletionRequest request =
                new ChatCompletionRequest(model, List.of(new ChatCompletionRequest.Message("user", prompt)));

        ChatCompletionResponse response = restClient
                .post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("API Gateway로부터 유효한 응답을 받지 못했습니다.");
        }

        ChatCompletionResponse.Choice.Message message =
                response.choices().get(0).message();
        if (message == null || message.content() == null) {
            throw new IllegalStateException("API Gateway 응답에 메시지 본문이 없습니다.");
        }

        return message.content();
    }

    record ChatCompletionRequest(String model, List<Message> messages) {
        record Message(String role, String content) {}
    }

    record ChatCompletionResponse(List<Choice> choices) {
        record Choice(Message message) {
            record Message(String role, String content) {}
        }
    }
}
