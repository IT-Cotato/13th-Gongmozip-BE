package org.cotato.gongmozip.domains.chatbot.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.ai.AiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ChatbotContestRecommendationAsyncServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private ChatbotContestRecommendationTxService txService;

    @InjectMocks
    private ChatbotContestRecommendationAsyncService service;

    @DisplayName("AI가 공모전을 추천하면 후보 등록/카드 발행을 트랜잭션 서비스에 위임한다.")
    @Test
    void AI가_공모전을_추천하면_후보_등록을_위임한다() {
        // given
        Contest contest = Contest.builder()
                .contestId(100L)
                .title("공모전")
                .category(InterestCategory.IT_AI_TECH)
                .build();
        given(contestRepository.findAllWithFilterAndDeadlineDesc(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(contest)));
        given(aiClient.recommendContests(InterestCategory.IT_AI_TECH, List.of(100L)))
                .willReturn(List.of(100L));

        // when
        service.recommendContestsAsync(1L, InterestCategory.IT_AI_TECH);

        // then
        verify(txService).registerCandidatesAndAnnounce(eq(1L), eq(List.of(contest)), eq(List.of(100L)));
        verify(txService, never()).announcePlainPrompt(any());
    }

    @DisplayName("추천할 공모전이 없으면 일반 안내만 위임한다.")
    @Test
    void 추천할_공모전이_없으면_일반_안내만_위임한다() {
        // given
        given(contestRepository.findAllWithFilterAndDeadlineDesc(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));
        given(aiClient.recommendContests(InterestCategory.IT_AI_TECH, List.of()))
                .willReturn(List.of());

        // when
        service.recommendContestsAsync(1L, InterestCategory.IT_AI_TECH);

        // then
        verify(txService).announcePlainPrompt(1L);
        verify(txService, never()).registerCandidatesAndAnnounce(any(), any(), any());
    }

    @DisplayName("AI 호출이 실패하면 예외를 삼키고 일반 안내로 폴백한다.")
    @Test
    void AI_호출이_실패하면_일반_안내로_폴백한다() {
        // given
        given(contestRepository.findAllWithFilterAndDeadlineDesc(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of()));
        given(aiClient.recommendContests(any(), any())).willThrow(new RuntimeException("AI Gateway timeout"));

        // when
        service.recommendContestsAsync(1L, InterestCategory.IT_AI_TECH);

        // then
        verify(txService).announcePlainPrompt(1L);
        verify(txService, never()).registerCandidatesAndAnnounce(any(), any(), any());
    }
}
