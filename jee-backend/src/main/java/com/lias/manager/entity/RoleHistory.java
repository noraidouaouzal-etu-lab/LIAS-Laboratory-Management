package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "role_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "old_role", nullable = false)
    private String oldRole;

    @Column(name = "new_role", nullable = false)
    private String newRole;

    @Column(name = "change_date", nullable = false)
    @Builder.Default
    private LocalDateTime changedAt = LocalDateTime.now();
}
