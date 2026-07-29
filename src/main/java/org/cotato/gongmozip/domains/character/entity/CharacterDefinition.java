package org.cotato.gongmozip.domains.character.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.survey.enums.CharacterType;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "character_definitions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CharacterDefinition extends BaseEntity {

    // 캐릭터 정의 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_definition_id", nullable = false, updatable = false)
    private Long characterDefinitionId;

    // 협업 유형 검사로 결정되는 캐릭터 타입 — 타입별 정의는 하나만 존재
    // ex. TRACK_RUNNER
    @Enumerated(EnumType.STRING)
    @Column(name = "character_type", nullable = false, unique = true, length = 50)
    private CharacterType characterType;

    // 결과 화면과 캐릭터 관리 화면에 표시할 캐릭터 한글 이름
    // ex. 트랙러너
    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    // 캐릭터 성향을 요약해 보여주는 한 줄 소개 문구
    // ex. 조용히 달려도 결국 완주하는 건 나야!
    @Column(name = "catchphrase", nullable = false, length = 255)
    private String catchphrase;
}
