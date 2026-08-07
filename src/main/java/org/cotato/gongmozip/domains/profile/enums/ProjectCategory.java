package org.cotato.gongmozip.domains.profile.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProjectCategory {
    CONTEST("공모전 출품"),
    EXTERNAL_ACTIVITY("대외활동 프로젝트"),
    CAMPUS_PROJECT("교내 프로젝트");

    private final String description;
}
