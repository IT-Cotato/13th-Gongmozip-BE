package org.cotato.gongmozip.domains.profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CertificationCategory {
    NONE("자격증 없음", 1),
    LANGUAGE("어학 자격증", 2),
    COMPUTER_IT("컴퓨터/IT 자격증", 3),
    DATA_AI("데이터분석/AI 자격증", 4),
    DESIGN("디자인 자격증", 5),
    MANAGEMENT_OFFICE("경영/사무 자격증", 6),
    OTHER("기타", 7);

    private final String categoryName;
    private final int displayOrder;
}
