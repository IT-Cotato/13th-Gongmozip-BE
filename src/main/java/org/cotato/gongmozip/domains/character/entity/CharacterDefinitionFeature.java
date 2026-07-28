package org.cotato.gongmozip.domains.character.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "character_definition_features")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CharacterDefinitionFeature extends BaseEntity {

    // 캐릭터 특징 설명 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_definition_feature_id", nullable = false, updatable = false)
    private Long characterDefinitionFeatureId;

    // 특징 설명이 속한 캐릭터 정의 — 하나의 캐릭터 정의가 여러 특징을 가진다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_definition_id", nullable = false)
    private CharacterDefinition definition;

    // 결과 화면에 목록으로 표시할 캐릭터의 구체적인 성향 설명
    // ex. 트랙러너: 말보다 결과물로 보여주는 꾸준한 러너, 눈에 띄진 않지만 팀의 완성도를 책임짐, 맡은 일은 끝까지 해내는 신뢰형 플레이어
    @Column(name = "content", nullable = false, length = 500)
    private String content;

    // 같은 캐릭터에 속한 특징 설명들의 화면 표시 순서
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
