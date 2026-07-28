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
@Table(name = "character_definition_tags")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CharacterDefinitionTag extends BaseEntity {

    // 캐릭터 태그 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_definition_tag_id", nullable = false, updatable = false)
    private Long characterDefinitionTagId;

    // 태그가 속한 캐릭터 정의 — 하나의 캐릭터 정의가 여러 태그를 가진다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_definition_id", nullable = false)
    private CharacterDefinition definition;

    // 결과 화면에 해시태그 형태로 표시할 성향 키워드
    // ex. 트랙러너: 꼼꼼함, 책임감, 집중력, 안정성
    @Column(name = "tag", nullable = false, length = 50)
    private String tag;

    // 같은 캐릭터에 속한 태그들의 화면 표시 순서
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
}
