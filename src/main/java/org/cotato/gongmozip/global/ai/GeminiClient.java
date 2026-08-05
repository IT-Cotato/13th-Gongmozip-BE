package org.cotato.gongmozip.global.ai;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Google Gemini API(https://ai.google.dev) 호출 전담 클라이언트. 무료 티어(API 키 발급만 하면
 * 결제수단 등록 없이 사용 가능)를 사용하므로, {@code GEMINI_API_KEY}가 비어있으면 비활성
 * 상태로 취급한다 (docs/decisions/08-ai.md 참고). 호출부({@link MockAiClient})가 비활성/실패 시
 * 키워드 기반 응답으로 대체한다.
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";
    private static final int TIMEOUT_MILLIS = 5000;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:gemini-2.5-flash-lite}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 프롬프트를 그대로 Gemini에 보내고 첫 번째 candidate의 텍스트를 반환한다. 실패 시 예외를 던진다. */
    public String generateContent(String prompt) {
        GeminiRequest request =
                new GeminiRequest(List.of(new GeminiRequest.Content(List.of(new GeminiRequest.Part(prompt)))));
        GeminiResponse response = restClient
                .post()
                .uri("/models/{model}:generateContent?key={apiKey}", model, apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);
        return extractText(response);
    }

    private String extractText(GeminiResponse response) {
        if (response == null
                || response.candidates() == null
                || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini 응답에 candidates가 없습니다.");
        }
        GeminiResponse.Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new IllegalStateException("Gemini 응답에 텍스트 파트가 없습니다.");
        }
        return content.parts().get(0).text();
    }

    record GeminiRequest(List<Content> contents) {
        record Content(List<Part> parts) {}

        record Part(String text) {}
    }

    record GeminiResponse(List<Candidate> candidates) {
        record Candidate(Content content) {}

        record Content(List<Part> parts) {}

        record Part(String text) {}
    }
}
