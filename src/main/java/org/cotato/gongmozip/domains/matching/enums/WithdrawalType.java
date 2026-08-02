package org.cotato.gongmozip.domains.matching.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "철회 처리 방식: FREE_CANCEL=14시 전 무료 취소, PENALIZED_PASS=14시 이후 협업거리 감점 패스")
public enum WithdrawalType {
    FREE_CANCEL,
    PENALIZED_PASS
}
