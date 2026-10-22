package com.lias.manager.controller;

import com.lias.manager.entity.Member;
import com.lias.manager.entity.Event;
import com.lias.manager.entity.Publication;
import com.lias.manager.entity.MemberProfileHistory;
import com.lias.manager.service.MemberService;
import com.lias.manager.service.PublicationService;
import com.lias.manager.repository.TeamRepository;
import com.lias.manager.repository.MemberRepository;
import com.lias.manager.repository.MemberProfileHistoryRepository;
import com.lias.manager.service.EventService;
import com.lias.manager.repository.DocumentRepository;
import com.lias.manager.entity.Document;
import com.lias.manager.entity.Notification;
import com.lias.manager.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequiredArgsConstructor
@RequestMapping("/associate")
public class AssociateController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssociateController.class);

    private final MemberService memberService;
    private final PublicationService publicationService;
    private final TeamRepository teamRepository;
    private final EventService eventService;
    private final MemberRepository memberRepository;
    private final MemberProfileHistoryRepository memberProfileHistoryRepository;
    private final com.lias.manager.repository.UserRepository userRepository;
    private final com.lias.manager.repository.NotificationRepository notificationRepository;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        String email = resolveCurrentUserEmail();
        log.info("Accessing associate dashboard. Current user: {}", email);
        Member associate = memberRepository.findByEmail(email).orElse(null);
        model.addAttribute("currentUserMember", associate);

        long myPubsCount = 0;
        if (associate != null) {
            log.info("Associate found: ID={}, Name={} {}", associate.getId(), associate.getFirstName(), associate.getLastName());
            String ln = associate.getLastName() != null ? associate.getLastName().toLowerCase() : "";
            String fn = associate.getFirstName() != null ? associate.getFirstName().toLowerCase() : "";
            myPubsCount = publicationService.findAll().stream()
                .filter(p -> p != null && p.getAuthor() != null && 
                    (p.getAuthor().toLowerCase().contains(ln) || p.getAuthor().toLowerCase().contains(fn)))
                .count();
        } else {
            log.warn("Associate profile not found for user: {}", email);
        }
        model.addAttribute("myPublicationsCount", myPubsCount);
        model.addAttribute("totalPublicationsCount", (long) publicationService.findAll().size());

        List<Event> upcomingEvents = eventService.findAll().stream()
            .filter(e -> e != null && !e.isArchived())
            .toList();
        model.addAttribute("upcomingEvents", upcomingEvents);

        List<Document> sharedDocuments = documentRepository.findAll().stream()
            .filter(d -> d != null && !d.isArchived())
            .toList();
        model.addAttribute("sharedDocuments", sharedDocuments);

        List<Notification> notifications = notificationRepository.findAll().stream()
            .filter(n -> n != null)
            .sorted((n1, n2) -> {
                if (n1.getDate() == null && n2.getDate() == null) return 0;
                if (n1.getDate() == null) return 1;
                if (n2.getDate() == null) return -1;
                return n2.getDate().compareTo(n1.getDate());
            })
            .limit(5)
            .toList();
        model.addAttribute("latestNotifications", notifications);

        return "dashboard-associate";
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
        Member associate = memberRepository.findByEmail(email)
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
                            .status("ASSOCIATE")
                            .institution(null)
                            .originLab(null)
                            .biography(null)
                            .interests(null)
                            .build();
                    return memberRepository.saveAndFlush(fallback);
                });
         model.addAttribute("member", associate);
        if (associate != null && associate.getId() != null) {
            model.addAttribute("history", memberProfileHistoryRepository.findByMemberIdOrderByChangeDateDesc(associate.getId()));
            
            // Retrieve member's publications
            String ln = associate.getLastName() != null ? associate.getLastName().toLowerCase() : "";
            String fn = associate.getFirstName() != null ? associate.getFirstName().toLowerCase() : "";
            List<Publication> myPubs = publicationService.findAll().stream()
                .filter(p -> p != null && p.getAuthor() != null && 
                    (p.getAuthor().toLowerCase().contains(ln) || p.getAuthor().toLowerCase().contains(fn)))
                .toList();
            model.addAttribute("myPublications", myPubs);
        } else {
            model.addAttribute("myPublications", List.of());
        }
        model.addAttribute("teams", teamRepository.findAll());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            @ModelAttribute Member edited,
            @RequestParam(value = "photoFile", required = false) MultipartFile photoFile) {
        String email = resolveCurrentUserEmail();
        Member existing = memberRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            if (photoFile != null && !photoFile.isEmpty()) {
                String savedPath = fileStorageService.storeFile(photoFile);
                existing.setPhoto(savedPath);
            }
            existing.setBiography(edited.getBiography());
            existing.setInterests(edited.getInterests());
            memberRepository.saveAndFlush(existing);
        }
        return "redirect:/associate/profile?success";
    }

    @GetMapping("/publications")
    public String publications(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir,
            Model model) {
        
        List<Publication> list = publicationService.findAll();
        
        java.util.stream.Stream<Publication> stream = list.stream().filter(p -> p != null);
        
        if (title != null && !title.trim().isEmpty()) {
            String t = title.trim().toLowerCase();
            stream = stream.filter(p -> p.getTitle() != null && p.getTitle().toLowerCase().contains(t));
        }
        if (author != null && !author.trim().isEmpty()) {
            String a = author.trim().toLowerCase();
            stream = stream.filter(p -> p.getAuthor() != null && p.getAuthor().toLowerCase().contains(a));
        }
        if (year != null) {
            stream = stream.filter(p -> p.getYear() != null && p.getYear().equals(year));
        }
        if (team != null && !team.trim().isEmpty()) {
            String tm = team.trim().toLowerCase();
            stream = stream.filter(p -> p.getTeam() != null && p.getTeam().toLowerCase().contains(tm));
        }
        
        // Sorting
        java.util.Comparator<Publication> comparator = null;
        if ("title".equalsIgnoreCase(sortBy)) {
            comparator = java.util.Comparator.comparing(p -> p.getTitle() != null ? p.getTitle() : "", String.CASE_INSENSITIVE_ORDER);
        } else if ("author".equalsIgnoreCase(sortBy)) {
            comparator = java.util.Comparator.comparing(p -> p.getAuthor() != null ? p.getAuthor() : "", String.CASE_INSENSITIVE_ORDER);
        } else {
            // Default or year
            comparator = java.util.Comparator.comparing(p -> p.getYear() != null ? p.getYear() : 0);
        }
        
        if ("desc".equalsIgnoreCase(sortDir) || (sortBy == null && sortDir == null)) {
            if (comparator != null) {
                comparator = comparator.reversed();
            }
        }
        
        if (comparator != null) {
            stream = stream.sorted(comparator);
        }
        
        List<Publication> filtered = stream.toList();
        
        model.addAttribute("publications", filtered);
        model.addAttribute("newPublication", new Publication());
        model.addAttribute("title", title);
        model.addAttribute("author", author);
        model.addAttribute("year", year);
        model.addAttribute("team", team);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        
        // Populate stats
        model.addAttribute("totalPublications", (long) list.size());
        model.addAttribute("publicationsThisYear", list.stream().filter(p -> p.getYear() != null && p.getYear().equals(java.time.LocalDate.now().getYear())).count());
        model.addAttribute("uniqueTeamsCount", list.stream().filter(p -> p.getTeam() != null && !p.getTeam().isEmpty()).map(p -> p.getTeam().trim().toLowerCase()).distinct().count());
        
        return "publications";
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
        
        java.util.List<Event> filteredList = stream.collect(java.util.stream.Collectors.toList());
        
        java.util.List<Event> upcomingEvents = filteredList.stream()
                .filter(e -> e != null && !e.isArchived())
                .collect(java.util.stream.Collectors.toList());
                
        java.util.List<Event> archivedEvents = filteredList.stream()
                .filter(e -> e != null && e.isArchived())
                .collect(java.util.stream.Collectors.toList());
                
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
