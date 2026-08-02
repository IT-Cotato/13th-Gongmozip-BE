package org.cotato.gongmozip.domains.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.cotato.gongmozip.domains.matching.converter.SectionListConverter;
import org.cotato.gongmozip.domains.matching.dto.MatchingResponse.SectionResponse;
import org.cotato.gongmozip.global.entity.BaseEntity;

@Getter
@Entity
@Table(name = "matching_explanations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class MatchingExplanation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matching_explanation_id", nullable = false, updatable = false)
    private Long matchingExplanationId;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Convert(converter = SectionListConverter.class)
    @Column(name = "sections", nullable = false, columnDefinition = "TEXT")
    private List<SectionResponse> sections;

    @Column(name = "disclaimer", nullable = false, columnDefinition = "TEXT")
    private String disclaimer;
}
