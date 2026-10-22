package com.lias.manager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 50)
    private String type; // CONFERENCE, SEMINAR, WORKSHOP

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, name = "event_date")
    private LocalDate date;

    @Column(nullable = false, length = 200)
    private String location;

    @Column(length = 100)
    private String edition;

    @Column(length = 50)
    private String time;

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "event_organizers",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "member_id")
    )
    @Builder.Default
    private java.util.Set<Member> organizers = new java.util.HashSet<>();

    @Transient
    public String getOrganizersDisplayString() {
        if (organizers == null || organizers.isEmpty()) {
            return "";
        }
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Member m : organizers) {
            if (m != null) {
                String ln = m.getLastName() != null ? m.getLastName() : "";
                String fn = m.getFirstName() != null ? m.getFirstName() : "";
                names.add((ln + " " + fn).trim());
            }
        }
        return String.join(", ", names);
    }

    @Transient
    public String getTeamsDisplayString() {
        if (organizers == null || organizers.isEmpty()) {
            return "";
        }
        java.util.Set<String> teamNames = new java.util.LinkedHashSet<>();
        for (Member m : organizers) {
            if (m != null && m.getTeam() != null && m.getTeam().getName() != null) {
                teamNames.add(m.getTeam().getName());
            }
        }
        if (teamNames.isEmpty()) {
            return "";
        }
        return String.join(", ", teamNames);
    }
}
