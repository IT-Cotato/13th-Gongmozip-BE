package org.cotato.gongmozip.domains.survey.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.survey.converter.SurveyConverter;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.AnswerRequest;
import org.cotato.gongmozip.domains.survey.dto.request.SurveyRequest.SubmitSurveyRequest;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.QuestionListResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.QuestionResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyResultResponse;
import org.cotato.gongmozip.domains.survey.dto.response.SurveyResponse.SurveyStatusResponse;
import org.cotato.gongmozip.domains.survey.entity.SurveyAnswer;
import org.cotato.gongmozip.domains.survey.entity.SurveyOption;
import org.cotato.gongmozip.domains.survey.entity.SurveyQuestion;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.ExtroversionType;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.exception.SurveyException;
import org.cotato.gongmozip.domains.survey.exception.codes.SurveyErrorCode;
import org.cotato.gongmozip.domains.survey.repository.SurveyAnswerRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyOptionRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveyQuestionRepository;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.cotato.gongmozip.domains.survey.vo.SurveyScoreSnapshot;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SurveyService {

    // 캐릭터 유형 결정 임계값: 만점(15점)의 절반 초과 기준
    private static final BigDecimal CHARACTER_THRESHOLD = new BigDecimal("8");
    private static final int RETAKE_INTERVAL_MONTHS = 3;

    private final SurveyQuestionRepository surveyQuestionRepository;
    private final SurveyOptionRepository surveyOptionRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final SurveyAnswerRepository surveyAnswerRepository;

    // 질문 목록 조회 — 선택지와 함께 반환하며 매 요청마다 순서를 셔플한다
    public QuestionListResponse getQuestions() {
        List<SurveyQuestion> questions = surveyQuestionRepository.findAllByOrderByDisplayOrderAsc();
        Map<Long, List<SurveyOption>> optionsByQuestionId = loadOptionsByQuestionId(questions);
        List<QuestionResponse> responses = new ArrayList<>(questions.stream()
                .map(q -> SurveyConverter.toQuestionResponse(
                        q, optionsByQuestionId.getOrDefault(q.getQuestionId(), List.of())))
                .toList());
        Collections.shuffle(responses);
        return new QuestionListResponse(responses);
    }

    // 설문 제출 상태 조회 — 제출 이력이 없으면 NONE 반환
    public SurveyStatusResponse getSurveyStatus(Member member) {
        return surveySubmissionRepository
                .findByMember(member)
                .map(submission ->
                        new SurveyStatusResponse(submission.getStatus().name()))
                .orElseGet(() -> new SurveyStatusResponse("NONE"));
    }

    // 설문 제출 — 답변 검증 → 저장 → 성향 점수 계산 → 캐릭터 유형 판정 순으로 처리
    @Transactional
    public SurveyResultResponse submitSurvey(Member member, SubmitSurveyRequest request) {
        List<SurveyQuestion> questions = surveyQuestionRepository.findAllByOrderByDisplayOrderAsc();
        Map<Long, List<SurveyOption>> optionsByQuestionId = loadOptionsByQuestionId(questions);

        // 선택지 전체 로드: 검증 시 N+1을 피하기 위해 먼저 맵으로 구성한다
        Map<Long, SurveyOption> optionById = new HashMap<>();
        Map<Long, Long> optionToQuestionId = new HashMap<>();
        for (SurveyQuestion q : questions) {
            for (SurveyOption opt : optionsByQuestionId.getOrDefault(q.getQuestionId(), List.of())) {
                optionById.put(opt.getOptionId(), opt);
                optionToQuestionId.put(opt.getOptionId(), q.getQuestionId());
            }
        }

        // 필수 질문 전부 답변했는지 검증
        Map<Long, SurveyQuestion> questionById =
                questions.stream().collect(Collectors.toMap(SurveyQuestion::getQuestionId, q -> q));
        Set<Long> requiredIds = questions.stream()
                .filter(SurveyQuestion::isRequired)
                .map(SurveyQuestion::getQuestionId)
                .collect(Collectors.toSet());
        Set<Long> answeredIds =
                request.answers().stream().map(AnswerRequest::questionId).collect(Collectors.toSet());
        if (!answeredIds.containsAll(requiredIds)) {
            throw new SurveyException(SurveyErrorCode.MISSING_REQUIRED_ANSWER);
        }

        // 각 답변의 질문·선택지 유효성 검증 후 answerMap 구성: questionId → SurveyOption
        Map<Long, SurveyOption> answerMap = new HashMap<>();
        for (AnswerRequest a : request.answers()) {
            if (!questionById.containsKey(a.questionId())) {
                throw new SurveyException(SurveyErrorCode.QUESTION_NOT_FOUND);
            }
            if (!optionById.containsKey(a.selectedOptionId())) {
                throw new SurveyException(SurveyErrorCode.OPTION_NOT_FOUND);
            }
            if (!optionToQuestionId.get(a.selectedOptionId()).equals(a.questionId())) {
                throw new SurveyException(SurveyErrorCode.OPTION_NOT_BELONG_TO_QUESTION);
            }
            answerMap.put(a.questionId(), optionById.get(a.selectedOptionId()));
        }

        // 회원별 submission 하나를 재사용하고, 재검사 시 기존 답변을 교체한다
        Optional<SurveySubmission> existingSubmission = surveySubmissionRepository.findByMember(member);
        existingSubmission.ifPresent(this::validateRetakeInterval);

        SurveySubmission submission = existingSubmission.orElseGet(() -> {
            SurveySubmission newSubmission = SurveySubmission.builder()
                    .member(member)
                    .status(SubmissionStatus.IN_PROGRESS)
                    .build();
            return surveySubmissionRepository.save(newSubmission);
        });
        surveyAnswerRepository.deleteAllBySubmission(submission);
        surveyAnswerRepository.flush(); // unique 제약 위반 방지를 위해 삭제를 먼저 DB에 반영

        // 새 답변 저장 후 제출 상태로 전환
        List<SurveyAnswer> answers = questions.stream()
                .filter(q -> answerMap.containsKey(q.getQuestionId()))
                .map(q -> SurveyConverter.toSurveyAnswer(submission, q, answerMap.get(q.getQuestionId())))
                .toList();
        surveyAnswerRepository.saveAll(answers);
        submission.submit();

        // 점수 계산 후 submission에 기록
        Map<String, Long> keyToId = questions.stream()
                .collect(Collectors.toMap(SurveyQuestion::getQuestionKey, SurveyQuestion::getQuestionId));
        calculateAndRecordScores(submission, answerMap, keyToId);

        return SurveyConverter.toResultResponse(submission);
    }

    private Map<Long, List<SurveyOption>> loadOptionsByQuestionId(List<SurveyQuestion> questions) {
        if (questions.isEmpty()) {
            return Map.of();
        }

        return surveyOptionRepository.findAllByQuestions(questions).stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getQuestionId()));
    }

    private void validateRetakeInterval(SurveySubmission submission) {
        if (submission.getStatus() != SubmissionStatus.SUBMITTED || submission.getSubmittedAt() == null) {
            return;
        }

        LocalDateTime nextRetakeAt = submission.getSubmittedAt().plusMonths(RETAKE_INTERVAL_MONTHS);
        if (nextRetakeAt.isAfter(LocalDateTime.now())) {
            throw new SurveyException(SurveyErrorCode.RETAKE_NOT_ALLOWED);
        }
    }

    // 가장 최근 제출한 설문 결과 조회
    public SurveyResultResponse getResult(Member member) {
        SurveySubmission submission = surveySubmissionRepository
                .findByMember(member)
                .filter(s -> s.getStatus() == SubmissionStatus.SUBMITTED)
                .orElseThrow(() -> new SurveyException(SurveyErrorCode.SURVEY_NOT_SUBMITTED));
        return SurveyConverter.toResultResponse(submission);
    }

    // 답변 기반으로 HEXACO 점수와 팀 성향 점수를 계산해 submission에 기록한다
    private void calculateAndRecordScores(
            SurveySubmission submission, Map<Long, SurveyOption> answerMap, Map<String, Long> keyToId) {

        // HEXACO 4개 요인: 각 3문항 평균
        BigDecimal agreeableness = avg(
                scoreOf("AGREEABLENESS_1", keyToId, answerMap),
                scoreOf("AGREEABLENESS_2", keyToId, answerMap),
                scoreOf("AGREEABLENESS_3", keyToId, answerMap));
        BigDecimal conscientiousness = avg(
                scoreOf("CONSCIENTIOUSNESS_1", keyToId, answerMap),
                scoreOf("CONSCIENTIOUSNESS_2", keyToId, answerMap),
                scoreOf("CONSCIENTIOUSNESS_3", keyToId, answerMap));
        BigDecimal honestyHumility = avg(
                scoreOf("HONESTY_HUMILITY_1", keyToId, answerMap),
                scoreOf("HONESTY_HUMILITY_2", keyToId, answerMap),
                scoreOf("HONESTY_HUMILITY_3", keyToId, answerMap));
        BigDecimal extroversion = avg(
                scoreOf("EXTROVERSION_1", keyToId, answerMap),
                scoreOf("EXTROVERSION_2", keyToId, answerMap),
                scoreOf("EXTROVERSION_3", keyToId, answerMap));

        // 팀 성향 3문항: 단일 선택 점수 (1·3·5점)
        BigDecimal goalPreference = scoreOf("GOAL_PREFERENCE", keyToId, answerMap);
        BigDecimal workStyle = scoreOf("WORK_STYLE", keyToId, answerMap);
        BigDecimal communicationStyle = scoreOf("COMMUNICATION_STYLE", keyToId, answerMap);

        // 캐릭터 축 계산에 사용하는 외향성 개별 문항 점수
        BigDecimal extroversion2 = scoreOf("EXTROVERSION_2", keyToId, answerMap);
        BigDecimal extroversion3 = scoreOf("EXTROVERSION_3", keyToId, answerMap);

        // X축: 목표선호 + 업무방식 + 성실성1 (만점 15)
        BigDecimal xScore = goalPreference.add(workStyle).add(scoreOf("CONSCIENTIOUSNESS_1", keyToId, answerMap));
        // Y축: 소통방식 + 외향성2 + 외향성3 (만점 15)
        BigDecimal yScore = communicationStyle.add(extroversion2).add(extroversion3);

        SurveyScoreSnapshot snapshot = SurveyScoreSnapshot.builder()
                .agreeablenessScore(agreeableness)
                .conscientiousnessScore(conscientiousness)
                .honestyHumilityScore(honestyHumility)
                .extroversionScore(extroversion)
                .goalPreferenceScore(goalPreference)
                .workStyleScore(workStyle)
                .communicationStyleScore(communicationStyle)
                .extroversion2Score(extroversion2)
                .extroversion3Score(extroversion3)
                .extroversionType(resolveExtroversionType(extroversion))
                .characterType(resolveCharacterType(xScore, yScore))
                .characterXScore(xScore)
                .characterYScore(yScore)
                .build();
        submission.recordScores(snapshot);
    }

    // 질문 키로 해당 답변의 점수를 반환한다
    private BigDecimal scoreOf(String key, Map<String, Long> keyToId, Map<Long, SurveyOption> answerMap) {
        return answerMap.get(keyToId.get(key)).getScoreWeight();
    }

    // 3문항 평균 (소수점 둘째 자리 반올림)
    private BigDecimal avg(BigDecimal a, BigDecimal b, BigDecimal c) {
        return a.add(b).add(c).divide(BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
    }

    // 외향성 유형 결정: I(<2.6) / A(2.6~3.4) / E(>3.4)
    private ExtroversionType resolveExtroversionType(BigDecimal score) {
        if (score.compareTo(new BigDecimal("2.6")) < 0) return ExtroversionType.I;
        if (score.compareTo(new BigDecimal("3.4")) <= 0) return ExtroversionType.A;
        return ExtroversionType.E;
    }

    // 캐릭터 유형 결정: X·Y 축 점수가 임계값(8점) 이상이면 high로 판정
    private CharacterType resolveCharacterType(BigDecimal xScore, BigDecimal yScore) {
        boolean highX = xScore.compareTo(CHARACTER_THRESHOLD) >= 0;
        boolean highY = yScore.compareTo(CHARACTER_THRESHOLD) >= 0;
        if (highX && highY) return CharacterType.LEAD_RUNNER;
        if (highX) return CharacterType.TRACK_RUNNER;
        if (highY) return CharacterType.BOOST_RUNNER;
        return CharacterType.FREE_RUNNER;
    }
}
