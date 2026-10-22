package com.lias.manager.repository;

import com.lias.manager.entity.Mandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MandateRepository extends JpaRepository<Mandate, Long> {

    @Query("SELECT m FROM Mandate m JOIN FETCH m.member mem LEFT JOIN FETCH mem.team WHERE m.active = true")
    List<Mandate> findAllActiveWithMemberAndTeam();

    @Query("SELECT m FROM Mandate m JOIN FETCH m.member mem LEFT JOIN FETCH mem.team")
    List<Mandate> findAllWithMemberAndTeam();
}
