package org.cotato.gongmozip.domains.contest.repository;

import java.time.LocalDateTime;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.profile.enums.InterestCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    boolean existsByTitleAndApplyEndAt(String title, LocalDateTime applyEndAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Contest c SET c.viewCount = c.viewCount + 1 WHERE c.contestId = :contestId")
    void incrementViewCount(@Param("contestId") Long contestId);

    // 1. deadlineAsc (마감일 임박순 정렬)
    @Query(
            "SELECT c FROM Contest c "
                    + "WHERE (:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) "
                    + "AND (:category IS NULL OR c.category = :category) "
                    + "AND (:status IS NULL OR "
                    + "     (:status = 'CLOSED' AND (c.status = 'CLOSED' OR c.applyEndAt < :now)) OR "
                    + "     (:status = 'OPEN' AND c.status = 'OPEN' AND c.applyEndAt >= :now) OR "
                    + "     (:status = 'UPCOMING' AND c.status = 'UPCOMING' AND c.applyEndAt >= :now)) "
                    + "ORDER BY CASE WHEN (c.status = 'CLOSED' OR c.applyEndAt < :now) THEN 1 ELSE 0 END ASC, c.applyEndAt ASC, c.createdAt DESC")
    Page<Contest> findAllWithFilterAndDeadlineAsc(
            @Param("keyword") String keyword,
            @Param("category") InterestCategory category,
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    // 2. deadlineDesc (마감일 먼 순 정렬)
    @Query(
            "SELECT c FROM Contest c "
                    + "WHERE (:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) "
                    + "AND (:category IS NULL OR c.category = :category) "
                    + "AND (:status IS NULL OR "
                    + "     (:status = 'CLOSED' AND (c.status = 'CLOSED' OR c.applyEndAt < :now)) OR "
                    + "     (:status = 'OPEN' AND c.status = 'OPEN' AND c.applyEndAt >= :now) OR "
                    + "     (:status = 'UPCOMING' AND c.status = 'UPCOMING' AND c.applyEndAt >= :now)) "
                    + "ORDER BY CASE WHEN (c.status = 'CLOSED' OR c.applyEndAt < :now) THEN 1 ELSE 0 END ASC, c.applyEndAt DESC, c.createdAt DESC")
    Page<Contest> findAllWithFilterAndDeadlineDesc(
            @Param("keyword") String keyword,
            @Param("category") InterestCategory category,
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    // 3. newest (최신 등록 순 정렬)
    @Query("SELECT c FROM Contest c "
            + "WHERE (:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) "
            + "AND (:category IS NULL OR c.category = :category) "
            + "AND (:status IS NULL OR "
            + "     (:status = 'CLOSED' AND (c.status = 'CLOSED' OR c.applyEndAt < :now)) OR "
            + "     (:status = 'OPEN' AND c.status = 'OPEN' AND c.applyEndAt >= :now) OR "
            + "     (:status = 'UPCOMING' AND c.status = 'UPCOMING' AND c.applyEndAt >= :now)) "
            + "ORDER BY CASE WHEN (c.status = 'CLOSED' OR c.applyEndAt < :now) THEN 1 ELSE 0 END ASC, c.createdAt DESC")
    Page<Contest> findAllWithFilterAndNewest(
            @Param("keyword") String keyword,
            @Param("category") InterestCategory category,
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    // 4. popular (스크랩 많은 순 정렬)
    @Query(
            value =
                    "SELECT c FROM Contest c " + "LEFT JOIN ContestScrap cs ON cs.contest = c "
                            + "WHERE (:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) "
                            + "AND (:category IS NULL OR c.category = :category) "
                            + "AND (:status IS NULL OR "
                            + "     (:status = 'CLOSED' AND (c.status = 'CLOSED' OR c.applyEndAt < :now)) OR "
                            + "     (:status = 'OPEN' AND c.status = 'OPEN' AND c.applyEndAt >= :now) OR "
                            + "     (:status = 'UPCOMING' AND c.status = 'UPCOMING' AND c.applyEndAt >= :now)) "
                            + "GROUP BY c.contestId, c.title, c.summary, c.description, c.category, c.status, c.hostName, c.applyStartAt, c.applyEndAt, c.announcementAt, c.eligibilityText, c.prizeText, c.locationText, c.thumbnailUrl, c.sourceUrl, c.isTeamParticipation, c.minTeamSize, c.maxTeamSize, c.viewCount, c.createdAt, c.updatedAt "
                            + "ORDER BY CASE WHEN (c.status = 'CLOSED' OR c.applyEndAt < :now) THEN 1 ELSE 0 END ASC, COUNT(cs) DESC, c.createdAt DESC",
            countQuery = "SELECT COUNT(c) FROM Contest c "
                    + "WHERE (:keyword IS NULL OR c.title LIKE %:keyword% OR c.description LIKE %:keyword%) "
                    + "AND (:category IS NULL OR c.category = :category) "
                    + "AND (:status IS NULL OR "
                    + "     (:status = 'CLOSED' AND (c.status = 'CLOSED' OR c.applyEndAt < :now)) OR "
                    + "     (:status = 'OPEN' AND c.status = 'OPEN' AND c.applyEndAt >= :now) OR "
                    + "     (:status = 'UPCOMING' AND c.status = 'UPCOMING' AND c.applyEndAt >= :now))")
    Page<Contest> findAllWithFilterAndPopular(
            @Param("keyword") String keyword,
            @Param("category") InterestCategory category,
            @Param("status") String status,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}
