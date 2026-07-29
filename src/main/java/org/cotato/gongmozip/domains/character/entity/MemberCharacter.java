package org.cotato.gongmozip.domains.character.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.character.enums.CharacterPalette;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "member_characters")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberCharacter extends BaseEntity {

    // 회원 캐릭터 설정 식별자
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_character_id", nullable = false, updatable = false)
    private Long memberCharacterId;

    // 캐릭터 설정의 소유 회원 — 회원당 하나의 캐릭터 설정만 존재(-> 설문 제출의 캐릭터에서 확인)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    // 사용자가 선택한 캐릭터 색상 팔레트 — enum 이름을 문자열로 저장
    @Enumerated(EnumType.STRING)
    @Column(name = "palette_code", nullable = false, length = 40)
    private CharacterPalette palette;

    // 사용자가 선택한 팔레트로 변경
    public void changePalette(CharacterPalette palette) {
        this.palette = palette;
    }

    // 캐릭터 원본 색상으로 초기화
    public void resetPalette() {
        this.palette = CharacterPalette.DEFAULT;
    }
}
