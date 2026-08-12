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

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT cs FROM ContestScrap cs JOIN FETCH cs.contest WHERE cs.member = :member",
            countQuery = "SELECT COUNT(cs) FROM ContestScrap cs WHERE cs.member = :member")
    Page<ContestScrap> findAllByMemberWithContest(
            @org.springframework.data.repository.query.Param("member") Member member, Pageable pageable);

    int countByMember(Member member);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ContestScrap cs WHERE cs.contest = :contest")
    void deleteAllByContest(@org.springframework.data.repository.query.Param("contest") Contest contest);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ContestScrap cs WHERE cs.member = :member")
    void deleteAllByMember(@org.springframework.data.repository.query.Param("member") Member member);
}
