package org.cotato.gongmozip.domains.contest.repository;

import java.util.Optional;
import org.cotato.gongmozip.domains.contest.entity.Contest;
import org.cotato.gongmozip.domains.contest.entity.ContestScrap;
import org.cotato.gongmozip.domains.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContestScrapRepository extends JpaRepository<ContestScrap, Long> {
    boolean existsByMemberAndContest(Member member, Contest contest);

    Optional<ContestScrap> findByMemberAndContest(Member member, Contest contest);

    Optional<ContestScrap> findByMember_MemberIdAndContest_ContestId(Long memberId, Long contestId);

    boolean existsByMember_MemberIdAndContest_ContestId(Long memberId, Long contestId);

    Page<ContestScrap> findAllByMember(Member member, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ContestScrap cs WHERE cs.contest = :contest")
    void deleteAllByContest(@org.springframework.data.repository.query.Param("contest") Contest contest);
}
