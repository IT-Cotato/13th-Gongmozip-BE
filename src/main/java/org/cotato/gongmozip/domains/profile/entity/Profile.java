package org.cotato.gongmozip.domains.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Profile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id", nullable = false, updatable = false)
    private Long profileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "school_name", nullable = false, length = 150)
    private String schoolName;

    @Column(name = "grade", nullable = false)
    private Integer grade;

    @Column(name = "major", nullable = false, length = 150)
    private String major;

    @Column(name = "secondary_major", length = 150)
    private String secondaryMajor;

    @Column(name = "gpa", nullable = false)
    private Double gpa;

    @Column(name = "gpa_scale", nullable = false)
    private Double gpaScale;

    @Convert(converter = InterestCategoryListConverter.class)
    @Column(name = "interest_categories", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private List<InterestCategory> interestCategories = new ArrayList<>();

    @Column(name = "is_main", nullable = false)
    private boolean isMain;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public void updateGrade(Integer grade) {
        this.grade = grade;
    }

    public void updateMajor(String major) {
        this.major = major;
    }

    public void updateSecondaryMajor(String secondaryMajor) {
        this.secondaryMajor = secondaryMajor;
    }

    public void updateGpa(Double gpa) {
        this.gpa = gpa;
    }

    public void updateGpaScale(Double gpaScale) {
        this.gpaScale = gpaScale;
    }

    public void updateInterestCategories(List<InterestCategory> interestCategories) {
        this.interestCategories = interestCategories;
    }

    public void setMain(boolean isMain) {
        this.isMain = isMain;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
