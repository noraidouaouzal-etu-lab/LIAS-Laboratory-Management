package com.lias.manager.repository;

import com.lias.manager.entity.RoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoleHistoryRepository extends JpaRepository<RoleHistory, Long> {
    List<RoleHistory> findByMemberId(Long memberId);
}
