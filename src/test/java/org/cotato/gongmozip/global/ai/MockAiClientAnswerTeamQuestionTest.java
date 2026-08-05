package org.cotato.gongmozip.global.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Gemini 연동/폴백 분기 검증 (docs/decisions/08-ai.md). 규칙기반 메서드 검증은 {@link MockAiClientTest} 참고. */
@ExtendWith(MockitoExtension.class)
class MockAiClientAnswerTeamQuestionTest {

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private MockAiClient aiClient;

    @DisplayName("Gemini가 활성화돼 있으면 Gemini 응답을 그대로 반환한다.")
    @Test
    void Gemini가_활성화돼_있으면_Gemini_응답을_그대로_반환한다() {
        // given
        given(geminiClient.isEnabled()).willReturn(true);
        given(geminiClient.generateContent(org.mockito.ArgumentMatchers.anyString()))
                .willReturn("Gemini의 답변이에요.");

        // when
        String answer = aiClient.answerTeamQuestion("우리 팀 잘 되고 있는 걸까?");

        // then
        assertThat(answer).isEqualTo("Gemini의 답변이에요.");
    }

    @DisplayName("Gemini 호출이 실패하면 키워드 기반 응답으로 대체한다.")
    @Test
    void Gemini_호출이_실패하면_키워드_기반_응답으로_대체한다() {
        // given
        given(geminiClient.isEnabled()).willReturn(true);
        willThrow(new RuntimeException("timeout"))
                .given(geminiClient)
                .generateContent(org.mockito.ArgumentMatchers.anyString());

        // when
        String answer = aiClient.answerTeamQuestion("우리 역할 분담 추천해줘");

        // then
        assertThat(answer).contains("역할 분담 추천이에요");
    }

    @DisplayName("Gemini가 비활성화돼 있으면(키 없음) 키워드 기반 응답으로 대체한다.")
    @Test
    void Gemini가_비활성화돼_있으면_키워드_기반_응답으로_대체한다() {
        // given
        given(geminiClient.isEnabled()).willReturn(false);

        // when
        String answer = aiClient.answerTeamQuestion("우리 타임라인 추천해줘");

        // then
        assertThat(answer).contains("타임라인 추천이에요");
    }

    @DisplayName("질문이 비어있으면 Gemini를 호출하지 않고 안내 문구를 반환한다.")
    @Test
    void 질문이_비어있으면_Gemini를_호출하지_않고_안내_문구를_반환한다() {
        // when
        String answer = aiClient.answerTeamQuestion("  ");

        // then
        assertThat(answer).contains("궁금한 점을 말씀해주시면");
        verifyNoInteractions(geminiClient);
    }
}
