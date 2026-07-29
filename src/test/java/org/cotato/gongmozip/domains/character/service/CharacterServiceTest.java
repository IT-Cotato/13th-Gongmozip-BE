package org.cotato.gongmozip.domains.character.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.CurrentCharacterResponse;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.PaletteListResponse;
import org.cotato.gongmozip.domains.character.dto.response.CharacterResponse.PaletteResponse;
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
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.domains.survey.enums.SubmissionStatus;
import org.cotato.gongmozip.domains.survey.repository.SurveySubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock
    private MemberCharacterRepository memberCharacterRepository;

    @Mock
    private CharacterDefinitionRepository characterDefinitionRepository;

    @Mock
    private CharacterDefinitionTagRepository characterDefinitionTagRepository;

    @Mock
    private CharacterDefinitionFeatureRepository characterDefinitionFeatureRepository;

    @Mock
    private SurveySubmissionRepository surveySubmissionRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CharacterService characterService;

    private Member member;
    private SurveySubmission submission;
    private MemberCharacter memberCharacter;
    private CharacterDefinition definition;

    @BeforeEach
    void setUp() {
        member = Member.builder().memberId(1L).email("character@gongmozip.com").build();
        submission = SurveySubmission.builder()
                .surveySubmissionId(10L)
                .member(member)
                .status(SubmissionStatus.SUBMITTED)
                .submittedAt(LocalDateTime.of(2026, 5, 19, 15, 0))
                .characterType(CharacterType.TRACK_RUNNER)
                .build();
        memberCharacter = MemberCharacter.builder()
                .memberCharacterId(20L)
                .member(member)
                .palette(CharacterPalette.DEFAULT)
                .build();
        definition = CharacterDefinition.builder()
                .characterDefinitionId(30L)
                .characterType(CharacterType.TRACK_RUNNER)
                .displayName("트랙러너")
                .catchphrase("조용히 달려도 결국 완주하는 건 나야!")
                .build();
    }

    @Test
    @DisplayName("최초 설문 제출 회원에게 기본 팔레트 캐릭터를 생성한다")
    void initializeCreatesDefaultCharacter() {
        given(memberCharacterRepository.findByMember(member)).willReturn(Optional.empty());

        characterService.initialize(member);

        ArgumentCaptor<MemberCharacter> captor = ArgumentCaptor.forClass(MemberCharacter.class);
        then(memberCharacterRepository).should().save(captor.capture());
        assertThat(captor.getValue().getMember()).isEqualTo(member);
        assertThat(captor.getValue().getPalette()).isEqualTo(CharacterPalette.DEFAULT);
    }

    @Test
    @DisplayName("DB 설명과 현재 팔레트를 조합해 캐릭터를 조회한다")
    void getCurrentCharacterReturnsCatalogAndCustomization() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        stubCurrentCharacter();
        given(characterDefinitionTagRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition))
                .willReturn(List.of(CharacterDefinitionTag.builder()
                        .definition(definition)
                        .tag("꼼꼼함")
                        .displayOrder(1)
                        .build()));
        given(characterDefinitionFeatureRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition))
                .willReturn(List.of(CharacterDefinitionFeature.builder()
                        .definition(definition)
                        .content("말보다 결과물로 보여주는 꾸준한 러너")
                        .displayOrder(1)
                        .build()));
        CurrentCharacterResponse response = characterService.getCurrentCharacter(1L);

        assertThat(response.characterType()).isEqualTo(CharacterType.TRACK_RUNNER);
        assertThat(response.displayName()).isEqualTo("트랙러너");
        assertThat(response.paletteCode()).isEqualTo(CharacterPalette.DEFAULT);
        assertThat(response.hashtags()).containsExactly("꼼꼼함");
        assertThat(response.features()).containsExactly("말보다 결과물로 보여주는 꾸준한 러너");
    }

    @Test
    @DisplayName("팔레트 변경 시 캐릭터 타입은 유지하고 선택 팔레트만 변경한다")
    void updatePaletteChangesOnlyPalette() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        stubCurrentCharacter();
        given(characterDefinitionTagRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition))
                .willReturn(List.of());
        given(characterDefinitionFeatureRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition))
                .willReturn(List.of());
        CurrentCharacterResponse response = characterService.updatePalette(1L, CharacterPalette.GRADIENT_OCEAN);

        assertThat(memberCharacter.getPalette()).isEqualTo(CharacterPalette.GRADIENT_OCEAN);
        assertThat(response.characterType()).isEqualTo(CharacterType.TRACK_RUNNER);
        assertThat(response.paletteCode()).isEqualTo(CharacterPalette.GRADIENT_OCEAN);
    }

    @Test
    @DisplayName("팔레트 목록을 표시 순서대로 반환하고 현재 팔레트를 선택 상태로 표시한다")
    void getPalettesReturnsOrderedPalettesAndCurrentSelection() {
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        stubCurrentCharacter();
        given(characterDefinitionTagRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition))
                .willReturn(List.of());
        given(characterDefinitionFeatureRepository.findAllByDefinitionOrderByDisplayOrderAsc(definition))
                .willReturn(List.of());

        PaletteListResponse response = characterService.getPalettes(1L);

        assertThat(response.defaultPaletteCode()).isEqualTo(CharacterPalette.DEFAULT);
        assertThat(response.palettes())
                .extracting(PaletteResponse::paletteCode)
                .containsExactly(CharacterPalette.values());
        assertThat(response.palettes())
                .filteredOn(PaletteResponse::selected)
                .singleElement()
                .extracting(PaletteResponse::paletteCode)
                .isEqualTo(CharacterPalette.DEFAULT);
    }

    @Test
    @DisplayName("설문 결과가 없으면 캐릭터 조회를 거부한다")
    void getCurrentCharacterWithoutSurveyThrows() {
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.getCurrentCharacter(member))
                .isInstanceOf(CharacterException.class)
                .extracting(error -> ((CharacterException) error).getErrorCode())
                .isEqualTo(CharacterErrorCode.CHARACTER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 회원의 캐릭터 조회를 거부한다")
    void getCurrentCharacterWithUnknownMemberThrows() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> characterService.getCurrentCharacter(1L))
                .isInstanceOf(MemberException.class)
                .extracting(error -> ((MemberException) error).getErrorCode())
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    private void stubCurrentCharacter() {
        given(surveySubmissionRepository.findByMember(member)).willReturn(Optional.of(submission));
        given(memberCharacterRepository.findByMember(member)).willReturn(Optional.of(memberCharacter));
        given(characterDefinitionRepository.findByCharacterType(CharacterType.TRACK_RUNNER))
                .willReturn(Optional.of(definition));
    }
}
