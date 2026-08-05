package org.cotato.gongmozip.domains.collaboration.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public final class CollaborationResponse {

    private CollaborationResponse() {}

    public record CollaborationDistanceResponse(
            int collaborationPoint, int maxCollaborationPoint, double gaugePercent) {}

    public record CollaborationHistoryResponse(Long historyId, int delta, String reason, LocalDateTime createdAt) {}

    public record CollaborationHistoryListResponse(List<CollaborationHistoryResponse> histories) {}
}
