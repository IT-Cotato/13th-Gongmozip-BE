package org.cotato.gongmozip.domains.character.service;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.cotato.gongmozip.domains.character.converter.CharacterConverter;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.PaletteListResponse;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinition;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinitionFeature;
import org.cotato.gongmozip.domains.character.entity.CharacterDefinitionTag;
import org.cotato.gongmozip.domains.character.entity.MemberCharacter;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.character.exception.CharacterException;
import org.cotato.gongmozip.domains.character.exception.codes.CharacterErrorCode;
import org.cotato.gongmozip.domains.character.repository.CharacterDefinitionFeatureRepository;
import org.cotato.gongmozip.domains.character.repository.CharacterDefinitionRepository;
import org.cotato.gongmozip.domains.character.repository.CharacterDefinitionTagRepository;
import org.cotato.gongmozip.domains.character.repository.MemberCharacterRepository;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.member.exception.MemberException;
import org.cotato.gongmozip.domains.member.exception.codes.MemberErrorCode;
import org.cotato.gongmozip.domains.member.repository.MemberRepository;
import org.cotato.gongmozip.domains.survey.entity.SurveySubmission;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CharacterService {

    private final MemberCharacterRepository memberCharacterRepository;
    private final CharacterDefinitionRepository characterDefinitionRepository;
    private final CharacterDefinitionTagRepository characterDefinitionTagRepository;
    private final CharacterDefinitionFeatureRepository characterDefinitionFeatureRepository;
    private final SurveySubmissionRepository surveySubmissionRepository;
    private final MemberRepository memberRepository;

    // 최초 설문 제출 회원의 캐릭터 설정을 기본 팔레트로 초기화(Default)
    @Transactional
    public void initialize(Member member) {
        if (memberCharacterRepository.findByMember(member).isPresent()) {
            return;
        }
        memberCharacterRepository.save(CharacterConverter.toDefaultMemberCharacter(member));
    }

    // 내 캐릭터 조회 — 협업 유형 검사 전이면 CHARACTER_NOT_FOUND
    public CurrentCharacterResponse getCurrentCharacter(Long memberId) {
        return getCurrentCharacter(getMember(memberId));
    }

    // 다른 도메인에서 이미 조회한 회원의 캐릭터를 조회한다
    public CurrentCharacterResponse getCurrentCharacter(Member member) {
        return findCurrentCharacter(member)
                .orElseThrow(() -> new CharacterException(CharacterErrorCode.CHARACTER_NOT_FOUND));
    }

    // 다른 도메인 연동용 캐릭터 조회 — 검사 전이면 Optional.empty() 반환
    // 마이페이지 등 조회 시 설문을 안한 경우 예외처리를 방지하기 위함.
    public Optional<CurrentCharacterResponse> findCurrentCharacter(Member member) {
        Optional<SurveySubmission> submission = surveySubmissionRepository
                .findByMember(member)
                .filter(value -> value.getStatus() == SubmissionStatus.SUBMITTED);
        if (submission.isEmpty()) {
            return Optional.empty();
        }

        MemberCharacter memberCharacter = memberCharacterRepository
                .findByMember(member)
                .orElseThrow(() -> new CharacterException(CharacterErrorCode.CHARACTER_NOT_FOUND));
        return Optional.of(createCurrentCharacterResponse(submission.get(), memberCharacter));
    }

    // enum 팔레트 목록을 표시 순서대로 조회하고 현재 선택 여부를 함께 반환한다
    public PaletteListResponse getPalettes(Long memberId) {
        CurrentCharacterResponse current = getCurrentCharacter(memberId);
        return CharacterConverter.toPaletteListResponse(current.paletteCode());
    }

    // 회원 캐릭터의 팔레트만 변경(캐릭터 유형은 기존 설문 결과를 유지)
    @Transactional
    public CurrentCharacterResponse updatePalette(Long memberId, CharacterPalette palette) {
        Member member = getMember(memberId);
        SurveySubmission submission = getSubmittedSurvey(member);
        MemberCharacter memberCharacter = memberCharacterRepository
                .findByMember(member)
                .orElseThrow(() -> new CharacterException(CharacterErrorCode.CHARACTER_NOT_FOUND));
        memberCharacter.changePalette(palette);
        return createCurrentCharacterResponse(submission, memberCharacter);
    }

    // ------ 내부 메서드 -------

    // 인증 회원 조회
    private Member getMember(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    // 설문 제출 조회
    private SurveySubmission getSubmittedSurvey(Member member) {
        return surveySubmissionRepository
                .findByMember(member)
                .filter(value -> value.getStatus() == SubmissionStatus.SUBMITTED)
                .orElseThrow(() -> new CharacterException(CharacterErrorCode.CHARACTER_NOT_FOUND));
    }

    // 캐릭터 응답 생성에 필요한 데이터를 조회하고 DTO 변환 Converter로 위임
    private CurrentCharacterResponse createCurrentCharacterResponse(
            SurveySubmission submission, MemberCharacter memberCharacter) {
        // 캐릭터 정의 조회
        CharacterDefinition definition = characterDefinitionRepository
                .findByCharacterType(submission.getCharacterType())
                .orElseThrow(() -> new CharacterException(CharacterErrorCode.CHARACTER_DEFINITION_NOT_FOUND));

        // 태그 조회
        List<CharacterDefinitionTag> tags =
                characterDefinitionTagRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition);
        // 설명 조회
        List<CharacterDefinitionFeature> features =
                characterDefinitionFeatureRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition);

        // 조합 후 반환
        return CharacterConverter.toCurrentCharacterResponse(submission, memberCharacter, definition, tags, features);
    }
}
