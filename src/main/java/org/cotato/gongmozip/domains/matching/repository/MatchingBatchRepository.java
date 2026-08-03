package org.cotato.gongmozip.domains.matching.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.cotato.gongmozip.domains.matching.entity.MatchingBatch;
import org.cotato.gongmozip.domains.matching.enums.MatchingBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 배치 선점과 일일 처리 대상 조회에 필요한 잠금 쿼리를 한곳에 두기 위해 만든 저장소다. */
public interface MatchingBatchRepository extends JpaRepository<MatchingBatch, Long> {

    // 선점 또는 결과 저장 중 같은 배치의 상태가 동시에 바뀌지 않도록 배치 행을 쓰기 잠금으로 조회한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            SELECT mb
            FROM MatchingBatch mb
            WHERE mb.matchingBatchId = :batchId
            """)
    Optional<MatchingBatch> findByIdWithLock(@Param("batchId") Long batchId);

    // 성공하지 않은 대상 중 오케스트레이터가 이번 실행에서 선점할 배치 ID만 안정된 순서로 조회한다.
    @Query(
            """
            SELECT mb.matchingBatchId
            FROM MatchingBatch mb
            WHERE mb.applicationDate = :applicationDate
              AND mb.status IN :statuses
            ORDER BY mb.category, mb.poolOrdinal
            """)
    List<Long> findProcessableIds(
            @Param("applicationDate") LocalDate applicationDate,
            @Param("statuses") Collection<MatchingBatchStatus> statuses);
}
