package org.cotato.gongmozip.domains.contest.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import org.cotato.gongmozip.global.validation.ValidationGroup;

public final class ContestRequest {

    private ContestRequest() {}

    public record SaveContestRequest(
            @NotBlank(groups = ValidationGroup.OnCreate.class, message = "공모전 제목은 필수 입력 항목입니다.")
                    @Size(max = 255, message = "공모전 제목은 최대 255자까지 입력 가능합니다.")
                    String title,
            @Size(max = 500, message = "공모전 요약은 최대 500자까지 입력 가능합니다.") String summary,
            @NotBlank(groups = ValidationGroup.OnCreate.class, message = "공모전 상세 내용은 필수 입력 항목입니다.") String description,
            @NotBlank(groups = ValidationGroup.OnCreate.class, message = "공모전 카테고리는 필수 입력 항목입니다.") String category,
            @NotBlank(groups = ValidationGroup.OnCreate.class, message = "모집 상태는 필수 입력 항목입니다.") String status,
            @NotBlank(groups = ValidationGroup.OnCreate.class, message = "주최 기관명은 필수 입력 항목입니다.")
                    @Size(max = 150, message = "주최 기관명은 최대 150자까지 입력 가능합니다.")
                    String hostName,
            LocalDateTime applyStartAt,
            @NotNull(groups = ValidationGroup.OnCreate.class, message = "접수 마감 일시는 필수 입력 항목입니다.")
                    LocalDateTime applyEndAt,
            LocalDateTime announcementAt,
            String eligibilityText,
            String prizeText,
            @Size(max = 255, message = "진행 장소는 최대 255자까지 입력 가능합니다.") String locationText,
            String thumbnailUrl,
            List<String> detailImageUrls,
            String sourceUrl,
            @NotNull(groups = ValidationGroup.OnCreate.class, message = "팀 참가 가능 여부는 필수 입력 항목입니다.")
                    Boolean isTeamParticipation,
            @Min(value = 1, message = "최소 팀 인원은 1명 이상이어야 합니다.") Integer minTeamSize,
            Integer maxTeamSize) {}
}
