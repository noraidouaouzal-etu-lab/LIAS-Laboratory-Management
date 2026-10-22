package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(length = 100)
    private String category;

    @Column(name = "arrival_date")
    private java.time.LocalDate arrivalDate;

    @Column(length = 50)
    private String status; // AVAILABLE, ASSIGNED, MAINTENANCE

    @Column(length = 150)
    private String location; // ex: Salle de calcul, Bureau 10
}
