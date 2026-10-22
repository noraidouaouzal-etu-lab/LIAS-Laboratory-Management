package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "mandates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mandate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "role_title", nullable = false)
    private String roleTitle; // Ex. DIRECTOR, VICE_DIRECTOR, TEAM_LEADER

    @Column(name = "team_assigned")
    private String teamAssigned;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
