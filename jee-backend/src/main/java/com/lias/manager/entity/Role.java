package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Rôle applicatif associé à Spring Security pour l'autorisation inter-modules.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // Ex. ROLE_VISITOR, ROLE_DIRECTOR, ROLE_TEAM_LEADER, ROLE_PHD_STUDENT
}
