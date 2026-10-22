package com.lias.manager.repository;

import com.lias.manager.entity.Convention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConventionRepository extends JpaRepository<Convention, Long> {
}
