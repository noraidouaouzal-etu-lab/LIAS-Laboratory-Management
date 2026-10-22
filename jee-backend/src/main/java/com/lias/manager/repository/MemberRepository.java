package com.lias.manager.repository;

import com.lias.manager.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByStatus(String status);
    java.util.Optional<Member> findByEmail(String email);
    long countByStatusIn(java.util.List<String> statuses);
}
