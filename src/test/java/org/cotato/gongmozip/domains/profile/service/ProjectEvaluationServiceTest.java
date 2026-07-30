package org.cotato.gongmozip.domains.profile.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.entity.Profile;
import org.cotato.gongmozip.domains.profile.entity.ProjectEvaluation;
import org.cotato.gongmozip.domains.profile.entity.ProjectExperience;
import org.cotato.gongmozip.domains.profile.enums.AiSummaryStatus;
import org.cotato.gongmozip.domains.profile.exception.ProfileException;
import org.cotato.gongmozip.domains.profile.exception.codes.ProfileErrorCode;
import org.cotato.gongmozip.domains.profile.repository.ProjectEvaluationRepository;
import org.cotato.gongmozip.domains.profile.repository.ProjectExperienceRepository;
import org.cotato.gongmozip.global.ai.AiClient;
import org.cotato.gongmozip.global.ai.dto.ProjectEvaluationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class ProjectEvaluationServiceTest {

    @InjectMocks
    private ProjectEvaluationService projectEvaluationService;

    @Mock
    private ProjectEvaluationRepository projectEvaluationRepository;

    @Mock
    private ProjectExperienceRepository projectExperienceRepository;

    @Mock
    private ProjectEvaluationTxService projectEvaluationTxService;

    @Mock
    private AiClient aiClient;

    private Member member;
    private Profile profile;
    private ProjectExperience project;

    @BeforeEach
    void setUp() {
        projectEvaluationService.setSelf(projectEvaluationService);
        TransactionSynchronizationManager.initSynchronization();

        member = Member.builder().memberId(1L).email("user@gongmozip.com").build();
        profile =
                Profile.builder().profileId(100L).member(member).nickname("러너").build();
        project = ProjectExperience.builder()
                .projectId(10L)
                .profile(profile)
                .projectName("프로젝트")
                .role("개발자")
                .description("설명")
                .build();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @DisplayName("프로젝트 AI 평가 요청 시 정상 접수된다.")
    @Test
    void 프로젝트_AI_평가_요청_시_정상_접수된다() {
        // given
        given(projectExperienceRepository.findById(10L)).willReturn(Optional.of(project));
        given(projectEvaluationRepository.findByProjectExperience(project)).willReturn(Optional.empty());

        // when
        projectEvaluationService.evaluateProject(10L, member);

        // then
        then(projectEvaluationTxService).should().pending(10L);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        then(projectEvaluationTxService).should().startProcessing(10L);
    }

    @DisplayName("이미 진행 중인 평가가 있을 경우 예외가 발생한다.")
    @Test
    void 이미_진행_중인_평가가_있을_경우_예외가_발생한다() {
        // given
        ProjectEvaluation evaluation = ProjectEvaluation.builder()
                .projectExperience(project)
                .status(AiSummaryStatus.PROCESSING)
                .build();
        given(projectExperienceRepository.findById(10L)).willReturn(Optional.of(project));
        given(projectEvaluationRepository.findByProjectExperience(project)).willReturn(Optional.of(evaluation));

        // when & then
        assertThatThrownBy(() -> projectEvaluationService.evaluateProject(10L, member))
                .isInstanceOf(ProfileException.class)
                .hasMessage(ProfileErrorCode.PROJECT_EVALUATION_GENERATION_IN_PROGRESS.getMessage());
    }

    @DisplayName("비동기 분석 스케줄링 실패(TaskRejectedException) 시 에러 로그와 함께 실패 상태로 저장된다.")
    @Test
    void 비동기_분석_스케줄링_실패_시_실패_상태로_저장된다() {
        // given
        given(projectExperienceRepository.findById(10L)).willReturn(Optional.of(project));
        given(projectEvaluationRepository.findByProjectExperience(project)).willReturn(Optional.empty());

        // setSelf에 스레드 거부 동작 Mocking
        ProjectEvaluationService mockSelf = org.mockito.Mockito.mock(ProjectEvaluationService.class);
        projectEvaluationService.setSelf(mockSelf);
        doThrow(new TaskRejectedException("Thread pool saturated"))
                .when(mockSelf)
                .evaluateProjectAsync(any(), any(), any(), any());

        // when
        projectEvaluationService.evaluateProject(10L, member);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        // then
        then(projectEvaluationTxService).should().fail(10L, "Thread pool saturation: Thread pool saturated");
    }

    @DisplayName("비동기 AI 평가 수행 시 AI 분석 응답 결과를 성공적으로 반영한다.")
    @Test
    void 비동기_AI_평가_수행_시_성공적으로_결과가_반영된다() {
        // given
        ProjectEvaluationResult mockResult = new ProjectEvaluationResult(95, "피드백");
        given(aiClient.evaluateProject("프로젝트", "개발자", "설명")).willReturn(mockResult);

        // when
        projectEvaluationService.evaluateProjectAsync(10L, "프로젝트", "개발자", "설명");

        // then
        then(projectEvaluationTxService).should().startProcessing(10L);
        then(projectEvaluationTxService).should().complete(10L, 95, "피드백");
    }
}
