package org.cotato.gongmozip.domains.inquiry.repository;

import java.util.List;
import org.cotato.gongmozip.domains.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByEmailOrderByCreatedAtDesc(String email);
}
