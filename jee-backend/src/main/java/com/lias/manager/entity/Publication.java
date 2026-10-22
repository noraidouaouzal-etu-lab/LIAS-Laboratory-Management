package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "publications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(nullable = false, name = "publication_year")
    private Integer year;

    @Column(length = 150)
    private String team;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PrePersist
    public void prePersist() {
        if (this.createdBy == null || this.createdBy.trim().isEmpty()) {
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                this.createdBy = auth.getName();
            } else {
                this.createdBy = "system";
            }
        }
    }
}
