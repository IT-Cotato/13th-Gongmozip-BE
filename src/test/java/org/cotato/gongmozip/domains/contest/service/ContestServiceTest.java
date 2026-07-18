package org.cotato.gongmozip.domains.contest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.contest.dto.request.ContestRequest.CreateContestRequest;
import org.cotato.gongmozip.domains.contest.dto.response.ContestResponse.*;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.contest.enums.ContestStatus;
import org.cotato.gongmozip.domains.contest.exception.ContestException;
import org.cotato.gongmozip.domains.contest.exception.codes.ContestErrorCode;
import org.cotato.gongmozip.domains.contest.repository.ContestRepository;
import org.cotato.gongmozip.domains.contest.repository.ContestScrapRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ContestServiceTest {

    @Mock
    private ContestRepository contestRepository;

    @Mock
    private ContestScrapRepository contestScrapRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ContestService contestService;

    @DisplayName("유효한 입력으로 공모전을 등록하면 성공한다.")
    @Test
    void 유효한_입력으로_공모전을_등록하면_성공한다() {
        // given
        CreateContestRequest request = new CreateContestRequest(
                "2026 미래도시 공모전",
                "요약",
                "상세 내용",
                "IT_AI_TECH",
                "OPEN",
                "진흥원",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                null,
                "대학생",
                "대상 500만원",
                "서울",
                "http://thumb.url",
                List.of("http://detail1.url"),
                "http://source.url",
                true,
                2,
                5);
        given(contestRepository.existsByTitleAndApplyEndAt(request.title(), request.applyEndAt()))
                .willReturn(false);
        given(contestRepository.save(any(Contest.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        ContestCreateResponse response = contestService.createContest(request);

        // then
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.category()).isEqualTo("IT_AI_TECH");
        assertThat(response.status()).isEqualTo("OPEN");
        then(contestRepository).should().save(any(Contest.class));
    }

    @DisplayName("동일한 제목과 마감기한을 가진 공모전이 이미 존재하면 예외가 발생한다.")
    @Test
    void 중복_공모전_등록시_예외가_발생한다() {
        // given
        CreateContestRequest request = new CreateContestRequest(
                "2026 미래도시 공모전",
                "요약",
                "상세 내용",
                "IT_AI_TECH",
                "OPEN",
                "진흥원",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                null,
                "대학생",
                "대상 500만원",
                "서울",
                "http://thumb.url",
                List.of("http://detail1.url"),
                "http://source.url",
                true,
                2,
                5);
        given(contestRepository.existsByTitleAndApplyEndAt(request.title(), request.applyEndAt()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> contestService.createContest(request))
                .isInstanceOf(ContestException.class)
                .hasMessage(ContestErrorCode.DUPLICATE_CONTEST.getMessage());
    }

    @DisplayName("팀 참여 인원 조건(최소 > 최대)이 올바르지 않으면 등록 시 예외가 발생한다.")
    @Test
    void 팀_참여_인원_조건_오류시_등록_예외가_발생한다() {
        // given
        CreateContestRequest request = new CreateContestRequest(
                "2026 미래도시 공모전",
                "요약",
                "상세 내용",
                "IT_AI_TECH",
                "OPEN",
                "진흥원",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                null,
                "대학생",
                "대상 500만원",
                "서울",
                "http://thumb.url",
                List.of("http://detail1.url"),
                "http://source.url",
                true,
                5, // 최소
                2 // 최대
                );

        // when & then
        assertThatThrownBy(() -> contestService.createContest(request))
                .isInstanceOf(ContestException.class)
                .hasMessage(ContestErrorCode.INVALID_CONTEST_INPUT.getMessage());
    }

    @DisplayName("존재하지 않는 공모전 상세 조회 시 예외가 발생한다.")
    @Test
    void 존재하지_않는_공모전_상세_조회시_예외가_발생한다() {
        // given
        given(contestRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> contestService.getContestDetail(1L))
                .isInstanceOf(ContestException.class)
                .hasMessage(ContestErrorCode.CONTEST_NOT_FOUND.getMessage());
    }

    @DisplayName("공모전 상세 조회 시 조회수가 1 증가한다.")
    @Test
    void 공모전_상세_조회시_조회수가_증가한다() {
        // given
        Contest contestBefore = Contest.builder()
                .contestId(1L)
                .title("제목")
                .description("내용")
                .category(InterestCategory.IT_AI_TECH)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(5))
                .viewCount(0)
                .build();
        Contest contestAfter = Contest.builder()
                .contestId(1L)
                .title("제목")
                .description("내용")
                .category(InterestCategory.IT_AI_TECH)
                .status(ContestStatus.OPEN)
                .applyEndAt(LocalDateTime.now().plusDays(5))
                .viewCount(1)
                .build();
        given(contestRepository.findById(1L))
                .willReturn(Optional.of(contestBefore))
                .willReturn(Optional.of(contestAfter));

        // when
        ContestDetailResponse response = contestService.getContestDetail(1L);

        // then
        assertThat(response.viewCount()).isEqualTo(1);
        then(contestRepository).should(times(1)).incrementViewCount(1L);
    }

    @DisplayName("공모전 목록 조회 시 페이징과 정렬 기준이 올바르게 전달된다.")
    @Test
    void 공모전_목록_조회시_정렬별로_메소드가_호출된다() {
        // given
        Page<Contest> page = new PageImpl<>(List.of());
        given(contestRepository.findAllWithFilterAndDeadlineAsc(any(), any(), any(), any(), any(PageRequest.class)))
                .willReturn(page);

        // when
        contestService.getContests("키워드", "IT_AI_TECH", "OPEN", "deadlineAsc", 0, 20);

        // then
        then(contestRepository)
                .should(times(1))
                .findAllWithFilterAndDeadlineAsc(any(), any(), any(), any(), any(Pageable.class));
    }

    @DisplayName("공모전을 스크랩하면 성공한다.")
    @Test
    void 공모전_스크랩시_성공한다() {
        // given
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        Contest contest = Contest.builder().contestId(1L).title("공모전").build();
        given(contestRepository.findById(1L)).willReturn(Optional.of(contest));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(contestScrapRepository.existsByMemberAndContest(member, contest)).willReturn(false);
        given(contestScrapRepository.save(any(ContestScrap.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        ScrapResponse response = contestService.scrapContest(1L, 1L);

        // then
        assertThat(response.contestId()).isEqualTo(1L);
        assertThat(response.isScrapped()).isTrue();
        then(contestScrapRepository).should().save(any(ContestScrap.class));
    }

    @DisplayName("이미 스크랩한 공모전을 중복 스크랩 시도하면 예외가 발생한다.")
    @Test
    void 중복_스크랩_시도시_예외가_발생한다() {
        // given
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        Contest contest = Contest.builder().contestId(1L).title("공모전").build();
        given(contestRepository.findById(1L)).willReturn(Optional.of(contest));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(contestScrapRepository.existsByMemberAndContest(member, contest)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> contestService.scrapContest(1L, 1L))
                .isInstanceOf(ContestException.class)
                .hasMessage(ContestErrorCode.ALREADY_SCRAPPED.getMessage());
    }

    @DisplayName("스크랩 취소 시 내역이 존재하면 성공적으로 삭제한다.")
    @Test
    void 스크랩_취소시_성공한다() {
        // given
        Member member =
                Member.builder().memberId(1L).email("user@gongmozip.com").build();
        Contest contest = Contest.builder().contestId(1L).title("공모전").build();
        ContestScrap scrap = ContestScrap.builder()
                .contestScrapId(1L)
                .member(member)
                .contest(contest)
                .build();
        given(contestRepository.findById(1L)).willReturn(Optional.of(contest));
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(contestScrapRepository.findByMemberAndContest(member, contest)).willReturn(Optional.of(scrap));

        // when
        contestService.unscrapContest(1L, 1L);

        // then
        then(contestScrapRepository).should().delete(scrap);
    }

    @DisplayName("페이지 크기가 100을 초과하면 예외가 발생한다.")
    @Test
    void 페이지_크기_초과시_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> contestService.getContests(null, null, null, "deadlineAsc", 0, 101))
                .isInstanceOf(ContestException.class)
                .hasMessage(ContestErrorCode.INVALID_CONTEST_INPUT.getMessage());
    }

    @DisplayName("최대 팀 원 수가 1 미만이면 등록 시 예외가 발생한다.")
    @Test
    void 최대_팀원_수_1_미만시_등록_예외가_발생한다() {
        // given
        CreateContestRequest request = new CreateContestRequest(
                "2026 미래도시 공모전",
                "요약",
                "상세 내용",
                "IT_AI_TECH",
                "OPEN",
                "진흥원",
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(30),
                null,
                "대학생",
                "대상 500만원",
                "서울",
                "http://thumb.url",
                List.of("http://detail1.url"),
                "http://source.url",
                true,
                1,
                0 // 최대 팀원 수 0
                );

        // when & then
        assertThatThrownBy(() -> contestService.createContest(request))
                .isInstanceOf(ContestException.class)
                .hasMessage(ContestErrorCode.INVALID_CONTEST_INPUT.getMessage());
    }

    @DisplayName("마감일이 지난 공모전의 남은 일수는 0으로 클램핑된다.")
    @Test
    void 마감일이_지난_공모전_남은_일수는_0으로_클램핑된다() {
        // given
        LocalDateTime pastApplyEndAt = LocalDateTime.now().minusDays(5);

        // when
        int daysRemaining = org.cotato.gongmozip.domains.contest.converter.ContestConverter.calculateDaysRemaining(
                pastApplyEndAt, LocalDateTime.now());

        // then
        assertThat(daysRemaining).isEqualTo(0);
    }
}
