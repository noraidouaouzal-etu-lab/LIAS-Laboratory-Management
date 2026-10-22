package com.lias.manager.repository;

import com.lias.manager.entity.MemberAffiliationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemberAffiliationHistoryRepository extends JpaRepository<MemberAffiliationHistory, Long> {
    List<MemberAffiliationHistory> findByMemberIdOrderByStartDateDesc(Long memberId);
}
