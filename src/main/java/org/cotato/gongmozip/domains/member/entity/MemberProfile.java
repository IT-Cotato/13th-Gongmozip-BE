package org.cotato.gongmozip.domains.member.entity;

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
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.enums.Gender;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "member_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MemberProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_profile_id", nullable = false, updatable = false)
    private Long userProfileId;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "profile_image_url", columnDefinition = "TEXT")
    private String profileImageUrl;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "school_name", length = 150)
    private String schoolName;

    @Column(name = "major", length = 150)
    private String major;

    @Column(name = "grade", length = 30)
    private String grade;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "intro", columnDefinition = "TEXT")
    private String intro;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Column(name = "interests", columnDefinition = "TEXT")
    private String interests;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = true;

    // 연관 관계 (1:1)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
