package org.cotato.gongmozip.domains.inquiry.repository;

import java.util.List;
import org.cotato.gongmozip.domains.inquiry.entity.Inquiry;
import org.cotato.gongmozip.domains.inquiry.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 비밀번호 대조가 애플리케이션에서 이뤄지므로 조회 행 수를 제한해 BCrypt 연산 폭증(DoS)을 방지한다
    List<Inquiry> findTop100ByEmailOrderByCreatedAtDesc(String email);

    Page<Inquiry> findAllByStatus(InquiryStatus status, Pageable pageable);
}
