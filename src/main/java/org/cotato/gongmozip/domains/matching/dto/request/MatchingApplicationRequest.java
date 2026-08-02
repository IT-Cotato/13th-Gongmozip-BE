package org.cotato.gongmozip.domains.matching.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.cotato.gongmozip.domains.matching.enums.LeaderPreference;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;

public class MatchingApplicationRequest {

    private MatchingApplicationRequest() {}

    // 매칭풀 입장 시 사용자가 화면에서 선택한 값을 전달한다
    // 프로필과 설문 결과의 실제 점수는 서버에서 조회하므로 요청에 포함하지 않는다
    @Schema(name = "MatchingApplyRequest", description = "팀원 매칭풀 입장 요청. 모든 필드는 필수이며 noticeConfirmed는 반드시 true여야 합니다.")
    public record ApplyRequest(
            // 여러 프로필 중 이번 매칭에서 다른 팀원에게 보여줄 본인 프로필
            @NotNull(message = "프로필을 선택해야 합니다.") Long profileId,
            // 사용자가 매칭받고 싶은 공모전 관심 카테고리
            @NotNull(message = "공모전 카테고리를 선택해야 합니다.") InterestCategory contestCategory,
            // 팀장 희망, 필요하면 가능, 원하지 않음 중 선택한 값
            @NotNull(message = "팀장 희망 여부를 선택해야 합니다.") LeaderPreference leaderPreference,
            // 마지막 주의사항 화면에서 확인 버튼을 눌렀는지 검증하는 값
            @NotNull(message = "매칭 주의사항 확인이 필요합니다.") @AssertTrue(message = "매칭 주의사항을 확인해야 합니다.")
                    Boolean noticeConfirmed) {}
}
