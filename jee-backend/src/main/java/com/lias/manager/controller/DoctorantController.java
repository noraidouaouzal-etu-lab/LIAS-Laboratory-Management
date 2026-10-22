package com.lias.manager.controller;

import com.lias.manager.entity.Member;
import com.lias.manager.entity.Publication;
import com.lias.manager.entity.Event;
import com.lias.manager.entity.Mandate;
import com.lias.manager.service.MemberService;
import com.lias.manager.service.PublicationService;
import com.lias.manager.repository.TeamRepository;
import com.lias.manager.service.EventService;
import com.lias.manager.repository.NotificationRepository;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.MemberProfileHistoryRepository;
import com.lias.manager.repository.MandateRepository;
import com.lias.manager.service.FileStorageService;
import com.lias.manager.repository.DocumentRepository;
import com.lias.manager.repository.PVMeetingRepository;
import com.lias.manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/doctorant")
public class DoctorantController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DoctorantController.class);

    private final MemberService memberService;
    private final PublicationService publicationService;
    private final TeamRepository teamRepository;
    private final EventService eventService;
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final MemberProfileHistoryRepository memberProfileHistoryRepository;
    private final UserRepository userRepository;
    private final MandateRepository mandateRepository;
    private final FileStorageService fileStorageService;
    private final DocumentRepository documentRepository;
    private final PVMeetingRepository pvMeetingRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String email = resolveCurrentUserEmail();
        log.info("Accessing doctorant dashboard. Current user: {}", email);
        Member doctorant = memberRepository.findByEmail(email).orElse(null);
        model.addAttribute("member", doctorant);
        model.addAttribute("currentUserMember", doctorant);

        if (doctorant != null) {
            log.info("Doctorant found: ID={}, Name={} {}", doctorant.getId(), doctorant.getFirstName(), doctorant.getLastName());
            String supervisor = "Non renseigné";
            if (doctorant.getTeam() != null) {
                // Find active team leader mandate for the same team
                Mandate leaderMandate = mandateRepository.findAll().stream()
                    .filter(Mandate::isActive)
                    .filter(m -> m.getRoleTitle() != null && ("TEAM_LEADER".equalsIgnoreCase(m.getRoleTitle()) || m.getRoleTitle().toLowerCase().contains("chef d")))
                    .filter(m -> m.getMember() != null && m.getMember().getTeam() != null && m.getMember().getTeam().getId().equals(doctorant.getTeam().getId()))
                    .findFirst()
                    .orElse(null);

                if (leaderMandate != null && leaderMandate.getMember() != null) {
                    supervisor = "Pr. " + leaderMandate.getMember().getFirstName() + " " + leaderMandate.getMember().getLastName();
                } else {
                    // Fallback to finding a PERMANENT member in the team if no active mandate matches
                    Member leader = memberRepository.findAll().stream()
                        .filter(m -> "PERMANENT".equalsIgnoreCase(m.getStatus()))
                        .filter(m -> m.getTeam() != null && m.getTeam().getId().equals(doctorant.getTeam().getId()))
                        .findFirst()
                        .orElse(null);
                    if (leader != null) {
                        supervisor = "Pr. " + leader.getFirstName() + " " + leader.getLastName();
                    }
                }
            }
            model.addAttribute("supervisor", supervisor);

            // Compute dynamic metrics for the dashboard
            String ln = doctorant.getLastName() != null ? doctorant.getLastName().toLowerCase() : "";
            String fn = doctorant.getFirstName() != null ? doctorant.getFirstName().toLowerCase() : "";
            long myPubsCount = publicationService.findAll().stream()
                .filter(p -> p != null && p.getAuthor() != null && (p.getAuthor().toLowerCase().contains(ln) || p.getAuthor().toLowerCase().contains(fn)))
                .count();
            model.addAttribute("myPublicationsCount", myPubsCount);
        } else {
            log.warn("Doctorant profile not found for user: {}", email);
            model.addAttribute("supervisor", "Non renseigné");
            model.addAttribute("myPublicationsCount", 0L);
        }

        // Upcoming events count
        long upcomingEventsCount = eventService.findAll().stream()
            .filter(e -> e != null && !e.isArchived())
            .count();
        model.addAttribute("upcomingEventsCount", upcomingEventsCount);

        // Documents count
        long documentsCount = documentRepository.findAll().stream()
            .filter(d -> d != null && !d.isArchived())
            .count();
        model.addAttribute("documentsCount", documentsCount);

        // PV Meetings count
        long pvMeetingsCount = pvMeetingRepository.findAll().stream()
            .filter(m -> m != null && !"ARCHIVED".equalsIgnoreCase(m.getStatus()))
            .count();
        model.addAttribute("pvMeetingsCount", pvMeetingsCount);

        return "dashboard-doctorant";
    }

    private String resolveCurrentUserEmail() {
        String loginName = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        var userOpt = userRepository.findByEmail(loginName);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(loginName);
        }
        if (userOpt.isPresent()) {
            return userOpt.get().getEmail();
        }
        return loginName;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        String email = resolveCurrentUserEmail();
        Member doctorant = memberRepository.findByEmail(email)
                .orElseGet(() -> {
                    String first = "";
                    String last = "";
                    if (email != null && email.contains("@")) {
                        String prefix = email.split("@")[0];
                        if (prefix.contains(".")) {
                            String[] parts = prefix.split("\\.");
                            first = parts[0].substring(0, 1).toUpperCase() + (parts[0].length() > 1 ? parts[0].substring(1) : "");
                            last = parts[1].substring(0, 1).toUpperCase() + (parts[1].length() > 1 ? parts[1].substring(1) : "");
                        } else {
                            first = prefix.substring(0, 1).toUpperCase() + (prefix.length() > 1 ? prefix.substring(1) : "");
                            last = "Chercheur";
                        }
                    } else {
                        first = "Membre";
                        last = "LIAS";
                    }
                    Member fallback = Member.builder()
                            .firstName(first)
                            .lastName(last)
                            .email(email)
                            .status("DOCTORANT")
                            .institution(null)
                            .originLab(null)
                            .biography(null)
                            .interests(null)
                            .build();
                    return memberRepository.saveAndFlush(fallback);
                });
        model.addAttribute("member", doctorant);
        if (doctorant != null && doctorant.getId() != null) {
            model.addAttribute("history", memberProfileHistoryRepository.findByMemberIdOrderByChangeDateDesc(doctorant.getId()));
            
            // Retrieve member's publications
            String ln = doctorant.getLastName() != null ? doctorant.getLastName().toLowerCase() : "";
            String fn = doctorant.getFirstName() != null ? doctorant.getFirstName().toLowerCase() : "";
            java.util.List<Publication> myPubs = publicationService.findAll().stream()
                .filter(p -> p != null && p.getAuthor() != null && 
                    (p.getAuthor().toLowerCase().contains(ln) || p.getAuthor().toLowerCase().contains(fn)))
                .toList();
            model.addAttribute("myPublications", myPubs);
        } else {
            model.addAttribute("myPublications", java.util.List.of());
        }
        model.addAttribute("teams", teamRepository.findAll());
        return "profile";
    }

    @PostMapping("/profile")
    @org.springframework.transaction.annotation.Transactional
    public String updateProfile(
            @ModelAttribute Member edited,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        String email = resolveCurrentUserEmail();
        Member existing = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Membre introuvable."));

        if (photoFile != null && !photoFile.isEmpty()) {
            String savedPath = fileStorageService.storeFile(photoFile);
            edited.setPhoto(savedPath);
        } else {
            edited.setPhoto(existing.getPhoto());
        }

        // Strictly enforce security: Doctorant can only edit photo, biography, and interests
        edited.setFirstName(existing.getFirstName());
        edited.setLastName(existing.getLastName());
        edited.setInstitution(existing.getInstitution());
        edited.setOriginLab(existing.getOriginLab());
        edited.setStatus(existing.getStatus());
        edited.setTeam(existing.getTeam());
        edited.setAffiliationDate(existing.getAffiliationDate());
        edited.setBirthDate(existing.getBirthDate());
        edited.setHireDate(existing.getHireDate());

        memberService.updateMemberProfile(email, edited);
        return "redirect:/doctorant/profile?success";
    }

    @GetMapping("/publications")
    public String publications(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String team,
            Model model) {
        model.addAttribute("publications", publicationService.searchAndFilter(author, year, team));
        model.addAttribute("newPublication", new Publication());
        model.addAttribute("author", author);
        model.addAttribute("year", year);
        model.addAttribute("team", team);
        return "publications";
    }

    @PostMapping("/publications")
    @org.springframework.transaction.annotation.Transactional
    public String savePublication(@ModelAttribute Publication publication) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/doctorant/publications?error=unauthorized";
        }
        String username = auth.getName();
        if (publication.getId() != null) {
            Publication existing = publicationService.findById(publication.getId()).orElse(null);
            if (existing != null) {
                if (!username.equalsIgnoreCase(existing.getCreatedBy())) {
                    return "redirect:/doctorant/publications?error=unauthorized";
                }
                publication.setCreatedBy(existing.getCreatedBy());
            } else {
                publication.setCreatedBy(username);
            }
        } else {
            publication.setCreatedBy(username);
        }

        publicationService.save(publication);

        try {
            com.lias.manager.entity.Notification notification = com.lias.manager.entity.Notification.builder()
                .message(publication.getAuthor() + " a publié un nouvel article: " + publication.getTitle())
                .read(false)
                .type("NEW_PUBLICATION")
                .date(LocalDateTime.now())
                .user("all")
                .build();
            notificationRepository.saveAndFlush(notification);
        } catch (Exception e) {
            System.err.println("Failed to write publication notification: " + e.getMessage());
        }

        return "redirect:/doctorant/publications?success";
    }

    @GetMapping("/events")
    public String events(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filterType,
            Model model) {
        java.util.List<Event> allEvents = eventService.findAll();
        
        java.util.stream.Stream<Event> stream = allEvents.stream();
        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.trim().toLowerCase();
            stream = stream.filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(lowerSearch));
        }
        if (filterType != null && !filterType.trim().isEmpty() && !"ALL".equalsIgnoreCase(filterType)) {
            stream = stream.filter(e -> filterType.equalsIgnoreCase(e.getType()));
        }
        
        java.util.List<Event> filteredList = stream.collect(Collectors.toList());
        
        java.util.List<Event> upcomingEvents = filteredList.stream()
                .filter(e -> e != null && !e.isArchived())
                .collect(Collectors.toList());
                
        java.util.List<Event> archivedEvents = filteredList.stream()
                .filter(e -> e != null && e.isArchived())
                .collect(Collectors.toList());
                
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("archivedEvents", archivedEvents);
        model.addAttribute("events", filteredList);
        
        model.addAttribute("search", search);
        model.addAttribute("filterType", filterType);
        
        model.addAttribute("newEvent", new Event());
        model.addAttribute("allMembers", memberRepository.findAll());
        return "events";
    }
}
