package com.lias.manager.repository;

import com.lias.manager.entity.MemberProfileHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemberProfileHistoryRepository extends JpaRepository<MemberProfileHistory, Long> {
    List<MemberProfileHistory> findByMemberIdOrderByChangeDateDesc(Long memberId);
}
