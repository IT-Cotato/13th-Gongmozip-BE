package org.cotato.gongmozip.domains.collaboration.converter;

import java.util.List;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationDistanceResponse;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationHistoryListResponse;
import org.cotato.gongmozip.domains.collaboration.dto.response.CollaborationResponse.CollaborationHistoryResponse;
import org.cotato.gongmozip.domains.collaboration.entity.CollaborationPointHistory;
import org.cotato.gongmozip.domains.collaboration.enums.CollaborationPointReason;
import org.cotato.gongmozip.domains.member.entity.Member;

public final class CollaborationConverter {

    private CollaborationConverter() {}

    public static CollaborationDistanceResponse toDistanceResponse(Member member) {
        int point = member.getCollaborationPoint();
        int max = Member.MAX_COLLABORATION_POINT;
        double percent = (point / (double) max) * 100.0;
        return new CollaborationDistanceResponse(point, max, percent);
    }

    public static CollaborationHistoryResponse toHistoryResponse(CollaborationPointHistory history) {
        return new CollaborationHistoryResponse(
                history.getCollaborationPointHistoryId(),
                history.getDelta(),
                toKoreanReason(history.getReasonCode()),
                history.getCreatedAt());
    }

    public static CollaborationHistoryListResponse toHistoryListResponse(List<CollaborationPointHistory> histories) {
        List<CollaborationHistoryResponse> list = histories.stream()
                .map(CollaborationConverter::toHistoryResponse)
                .toList();
        return new CollaborationHistoryListResponse(list);
    }

    private static String toKoreanReason(CollaborationPointReason reason) {
        if (reason == null) {
            return "기타";
        }
        return switch (reason) {
            case LEAVE_PENALTY -> "채팅방 중도 이탈";
            case PROGRESS_CHECK_RESPONSE -> "중간점검 응답 완료";
            case PROJECT_COMPLETE_MEMBER -> "프로젝트 완주";
            case PROJECT_COMPLETE_LEADER -> "프로젝트 완주 (팀장)";
            case REVIEW_WRITTEN -> "팀원 리뷰 작성 완료";
            case MATCHING_PASS_PENALTY -> "매칭 패스 페널티";
        };
    }
}
